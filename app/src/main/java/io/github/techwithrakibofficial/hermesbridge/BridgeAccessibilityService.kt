package io.github.techwithrakibofficial.hermesbridge

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import android.app.KeyguardManager
import android.os.Build
import android.os.BatteryManager
import android.net.Uri
import android.content.IntentFilter
import android.content.pm.PackageManager
import org.json.JSONObject
import org.json.JSONArray
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException

data class LocalBridgeState(
    val running: Boolean = false,
    val token: String = "",
    val message: String = "Bridge stopped",
    val screenEnabled: Boolean = false,
    val lastAction: String = "No remote action requested"
)

class BridgeAccessibilityService : AccessibilityService() {
    @Volatile private var server: LocalBridgeServer? = null
    @Volatile private var appCatalog: AppCatalog? = null
    @Volatile private var lastDispatchError = "none"
    private val screen = ScreenController(this)
    private var remoteVoice: VoiceOutput? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connectedState.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType in listOf(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_WINDOWS_CHANGED)) screen.changed()
    }
    override fun onInterrupt() { stopSession() }

    private fun startSession() {
        if (server?.isRunning == true) return
        stopSession()
        try {
            lateinit var candidate: LocalBridgeServer
            fun dispatch(action: String, body: String): String {
                if (server !== candidate || !candidate.isRunning) return ScreenController.error("bridge_stopped").toString()
                // Package queries and label resources must not block UI, TTS init or screen actions.
                if (action == "apps") return readAppCatalog(candidate).toString()
                val task = FutureTask<String> {
                    if (server !== candidate || !candidate.isRunning) ScreenController.error("bridge_stopped").toString()
                    else executeCommand(action, body).toString()
                }
                mainHandler.post(task)
                return try { task.get(1200, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) {
                    task.cancel(false)
                    mainHandler.removeCallbacks(task)
                    lastDispatchError = if (action == "screen") "screen_observation_timed_out" else "android_dispatch_timed_out"
                    ScreenController.error(lastDispatchError).toString()
                } catch (_: ExecutionException) {
                    lastDispatchError = "android_handler_failed"
                    ScreenController.error(lastDispatchError).toString()
                } catch (_: InterruptedException) {
                    task.cancel(false)
                    mainHandler.removeCallbacks(task)
                    Thread.currentThread().interrupt()
                    ScreenController.error("android_dispatch_interrupted").toString()
                }
            }
            candidate = LocalBridgeServer(PORT,
                { action -> JSONObject(dispatch(action, "{}")).optBoolean("accepted", false) },
                { action, body -> dispatch(action, body) })
            server = candidate
            appCatalog = AppCatalog { loadAppCatalog() }.also { it.warm() }
            lastDispatchError = "none"
            candidate.start()
            remoteVoice = VoiceOutput(this)
            bridgeState.value = LocalBridgeState(true, candidate.token, "Listening on 127.0.0.1:$PORT")
        } catch (_: Exception) {
            appCatalog?.close()
            appCatalog = null
            remoteVoice?.close()
            remoteVoice = null
            server?.close()
            server = null
            bridgeState.value = LocalBridgeState(message = "Bridge could not start. Check if port $PORT is already in use, then try again.")
        }
    }

    private fun executeCommand(action: String, body: String): JSONObject {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked && action != "stop_speech")
            return ScreenController.error("phone_locked")
        val args = try { JSONObject(body) } catch (_: Exception) { return ScreenController.error("invalid_json") }
        fun keys(vararg names: String) = args.keys().asSequence().toSet() == names.toSet()
        fun string(name: String, max: Int) = args.opt(name) is String && args.getString(name).length <= max
        val result = when (action) {
            "screen" -> if (bridgeState.value.screenEnabled) screen.observe() else ScreenController.error("enable_screen_control_in_bridge_app")
            "click", "long-click", "scroll", "set-text" -> {
                if (!bridgeState.value.screenEnabled) return ScreenController.error("enable_screen_control_in_bridge_app")
                val extra = when (action) { "scroll" -> arrayOf("direction"); "set-text" -> arrayOf("text"); else -> emptyArray() }
                if (!keys("snapshot", "node", *extra) || !string("snapshot", 64) || !string("node", 16) ||
                    (action == "set-text" && !string("text", 2000)) ||
                    (action == "scroll" && args.optString("direction") !in listOf("forward", "backward")))
                    return ScreenController.error("invalid_arguments")
                screen.act(action, args)
            }
            "device" -> deviceState()
            "open-package" -> {
                if (!keys("package") || !string("package", 180)) return ScreenController.error("invalid_arguments")
                val pkg = args.getString("package")
                if (!pkg.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) return ScreenController.error("invalid_package")
                // getLaunchIntentForPackage validates the exact package without a full catalog query.
                ScreenController.accepted(packageManager.getLaunchIntentForPackage(pkg)?.let { launchIntent(it) } ?: false)
            }
            "dial" -> {
                if (!keys("number") || !string("number", 21) || !args.getString("number").matches(Regex("\\+?[0-9]{3,20}")))
                    return ScreenController.error("invalid_number_use_digits_and_optional_plus")
                ScreenController.accepted(launchIntent(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", args.getString("number"), null))))
            }
            "speak" -> {
                if (!keys("text") || !string("text", 2000)) return ScreenController.error("invalid_arguments")
                val voice = remoteVoice ?: VoiceOutput(this).also { remoteVoice = it }
                if (!voice.ready) return ScreenController.error("voice_warming_up_try_speak_after_ready")
                ScreenController.accepted(voice.speak(args.getString("text")))
            }
            else -> {
                val ok = when (action) {
                    "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                    "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                    "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                    "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                    "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                    "settings" -> launchSettings()
                    "stop_speech" -> { remoteVoice?.stop(); true }
                    else -> if (action.startsWith("open_app:")) launchAllowedApp(action.removePrefix("open_app:")) else false
                }
                ScreenController.accepted(ok)
            }
        }
        if (action !in listOf("screen", "apps", "device")) {
            screen.clear()
            bridgeState.value = bridgeState.value.copy(lastAction = "$action: request handled; observe screen to verify")
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun loadAppCatalog(): AppCatalog.Snapshot {
        val apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
            .distinctBy { it.activityInfo.packageName }
        var fallbacks = 0
        val entries = apps.take(150).map {
            val pkg = it.activityInfo.packageName
            val label = try { it.loadLabel(packageManager).toString() }
            catch (_: RuntimeException) { fallbacks++; pkg }
            AppCatalog.Entry(pkg, label)
        }.sortedBy { it.label } // Sort plain strings; load each label only once.
        return AppCatalog.Snapshot(entries, apps.size > 150, fallbacks)
    }
    private fun readAppCatalog(candidate: LocalBridgeServer): JSONObject {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) return ScreenController.error("phone_locked")
        val catalog = appCatalog ?: return ScreenController.error("apps_catalog_unavailable")
        val result = catalog.read(2800)
        // Stop/session rotation must discard results from a read already in flight.
        if (server !== candidate || !candidate.isRunning || appCatalog !== catalog)
            return ScreenController.error("bridge_stopped")
        val data = result.snapshot ?: return ScreenController.error(result.error ?: "apps_query_failed")
        return JSONObject().put("apps", JSONArray(data.apps.map {
            JSONObject().put("package", it.packageName).put("label", it.label)
        })).put("truncated", data.truncated).put("label_fallbacks", data.labelFallbacks)
            .put("cache_seconds", 30)
    }
    private fun deviceState(): JSONObject {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return JSONObject().put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE).put("sdk", Build.VERSION.SDK_INT)
            .put("battery_percent", if (level >= 0 && scale > 0) level * 100 / scale else -1)
            .put("screen_control_enabled", bridgeState.value.screenEnabled)
            .put("voice_ready", remoteVoice?.ready == true)
            .put("voice_status", remoteVoice?.status ?: "not_started")
            .put("voice_language", remoteVoice?.language ?: "unset")
            .put("app_catalog_state", appCatalog?.state() ?: "stopped")
            .put("last_dispatch_error", lastDispatchError)
            .put("notice", "Basic state only; this does not diagnose hardware, carrier or app failures.")
    }

    private fun launchSettings(): Boolean = launchIntent(Intent(Settings.ACTION_SETTINGS))

    private fun launchAllowedApp(alias: String): Boolean {
        val packageName = APP_PACKAGES[alias] ?: return false
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return launchIntent(intent)
    }

    private fun launchIntent(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun stopSession() {
        appCatalog?.close()
        appCatalog = null
        screen.clear()
        remoteVoice?.close()
        remoteVoice = null
        server?.close()
        server = null
        bridgeState.value = LocalBridgeState()
    }

    private fun disconnect() {
        stopSession()
        if (instance === this) {
            instance = null
            connectedState.value = false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }
    override fun onDestroy() { disconnect(); super.onDestroy() }

    companion object {
        private val APP_PACKAGES = mapOf(
            "whatsapp" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "messenger" to "com.facebook.orca",
            "telegram" to "org.telegram.messenger",
            "chrome" to "com.android.chrome",
            "termux" to "com.termux"
        )
        const val PORT = 18765
        private var instance: BridgeAccessibilityService? = null
        private val connectedState = MutableStateFlow(false)
        val connected = connectedState.asStateFlow()
        private val bridgeState = MutableStateFlow(LocalBridgeState())
        val bridge = bridgeState.asStateFlow()
        fun home(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        fun back(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun startBridge() {
            val service = instance
            if (service != null) service.startSession()
            else bridgeState.value = LocalBridgeState(message = "Enable Accessibility first")
        }
        fun allowScreenControl(allowed: Boolean) {
            instance?.screen?.clear()
            bridgeState.value = bridgeState.value.copy(screenEnabled = allowed && bridgeState.value.running)
        }
        fun refreshRemoteVoice() {
            val service = instance ?: return
            if (service.server?.isRunning == true) {
                service.remoteVoice?.close()
                service.remoteVoice = VoiceOutput(service)
            }
        }
        fun stopBridge() { instance?.stopSession() }
    }
}
