package io.github.techwithrakibofficial.hermesbridge

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

data class LocalBridgeState(
    val running: Boolean = false,
    val token: String = "",
    val message: String = "Bridge stopped",
    val lastAction: String = "No remote action requested"
)

class BridgeAccessibilityService : AccessibilityService() {
    @Volatile private var server: LocalBridgeServer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connectedState.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { stopSession() }

    private fun startSession() {
        if (server?.isRunning == true) return
        stopSession()
        try {
            lateinit var candidate: LocalBridgeServer
            candidate = LocalBridgeServer(PORT) { action ->
                val task = FutureTask<Boolean> {
                    if (server !== candidate || !candidate.isRunning) false
                    else {
                        val accepted = when (action) {
                            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                            "settings" -> launchSettings()
                            else -> if (action.startsWith("open_app:")) launchAllowedApp(action.removePrefix("open_app:")) else false
                        }
                        bridgeState.value = bridgeState.value.copy(
                            lastAction = "$action: " + if (accepted) "accepted by Android; check phone screen" else "rejected by Android"
                        )
                        accepted
                    }
                }
                mainHandler.post(task)
                try { task.get(1200, TimeUnit.MILLISECONDS) }
                catch (e: Exception) {
                    task.cancel(false)
                    mainHandler.removeCallbacks(task)
                    throw IllegalStateException("Action not confirmed", e)
                }
            }
            server = candidate
            candidate.start()
            bridgeState.value = LocalBridgeState(true, candidate.token, "Listening on 127.0.0.1:$PORT")
        } catch (_: Exception) {
            server?.close()
            server = null
            bridgeState.value = LocalBridgeState(message = "Bridge could not start. Check if port $PORT is already in use, then try again.")
        }
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
        fun stopBridge() { instance?.stopSession() }
    }
}
