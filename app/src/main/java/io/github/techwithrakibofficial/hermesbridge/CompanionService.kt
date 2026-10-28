package io.github.techwithrakibofficial.hermesbridge

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** Started only by a visible UI action; never restarts recording after process death. */
class CompanionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var runtime: CompanionRuntime
    private var microphone = false
    private var wake: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        runtime = CompanionRuntime.get(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Hermes companion task / voice", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Visible user-started tasks and microphone. Stop ends local voice and requests task cancellation."
            setSound(null, null)
            enableVibration(false)
        })
        runtime.onIdle = { if (!runtime.state.value.busy && !runtime.state.value.automatic && runtime.state.value.phase != "listening" && runtime.state.value.phase != "speaking") {
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        } }
        scope.launch { runtime.state.collectLatest { state ->
            if (state.busy || state.automatic || state.phase in listOf("listening", "speaking"))
                manager.notify(NOTIFICATION, notification(state.status))
        } }
    }
    private fun notification(status: String): Notification {
        val open = PendingIntent.getActivity(this, 1, Intent(this, CompanionActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 2, Intent(this, CompanionService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_hermes)
            .setContentTitle(if (microphone) "Hermes voice / task" else "Hermes task")
            .setContentText(status.take(100)).setContentIntent(open).setOngoing(true)
            .setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == STOP) {
            runtime.stopTask(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            return START_NOT_STICKY
        }
        if (intent.action == LISTEN && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) microphone = true
        try {
            val notice = notification("Starting companion…")
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notice,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or (if (microphone && Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0))
            else startForeground(NOTIFICATION, notice)
        } catch (_: Exception) {
            runtime.pauseVoice("Foreground permission unavailable. Open app and check permissions.")
            stopSelf(); return START_NOT_STICKY
        }
        if (wake?.isHeld != true) {
            try { wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Hermes:Companion").also { it.acquire(16 * 60_000L) } }
            catch (_: Exception) { }
        }
        runtime.allowCapture(microphone)
        when (intent.action) {
            LISTEN -> { runtime.automatic(intent.getBooleanExtra("automatic", false)); runtime.listen() }
            SEND -> runtime.send(intent.getStringExtra("text").orEmpty())
            RESUME -> runtime.connect()
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) { runtime.stopTask("Foreground time limit"); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onTaskRemoved(rootIntent: Intent?) { runtime.stopTask("App task closed"); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() {
        runtime.onIdle = null
        runCatching { if (wake?.isHeld == true) wake?.release() }
        wake = null
        runtime.allowCapture(false)
        runtime.serviceEnded()
        scope.cancel()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        const val SEND = "hermes.companion.SEND"
        const val LISTEN = "hermes.companion.LISTEN"
        const val STOP = "hermes.companion.STOP"
        const val RESUME = "hermes.companion.RESUME"
        private const val CHANNEL = "hermes-companion"
        private const val NOTIFICATION = 15
        fun start(context: Context, action: String, text: String = "", automatic: Boolean = false) {
            val intent = Intent(context, CompanionService::class.java).setAction(action).putExtra("text", text).putExtra("automatic", automatic)
            if (action == STOP) context.startService(intent) else context.startForegroundService(intent)
        }
    }
}
