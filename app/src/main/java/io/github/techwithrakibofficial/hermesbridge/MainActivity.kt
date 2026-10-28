package io.github.techwithrakibofficial.hermesbridge

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var speechRate by mutableFloatStateOf(0.70f)
    private val voicePreferences by lazy { getSharedPreferences("voice", MODE_PRIVATE) }
    private var speechState by mutableStateOf("Initializing speech engine…")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechRate = voicePreferences.getFloat("speech_rate", 0.70f).coerceIn(0.25f, 1.25f)
        tts = TextToSpeech(this) { result ->
            runOnUiThread {
                val language = if (result == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.US) else null
                ttsReady = language != null && language >= TextToSpeech.LANG_AVAILABLE
                if (ttsReady) tts?.setSpeechRate(speechRate)
                speechState = if (ttsReady) "Speech engine ready" else "Speech unavailable. Check installed TTS engine/language."
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = updateSpeech("Speaking…")
            override fun onDone(utteranceId: String?) = updateSpeech("Speech completed. Confirm you heard it.")
            @Deprecated("Required by Android")
            override fun onError(utteranceId: String?) = updateSpeech("Speech failed")
        })
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF5EEAD4), background = Color(0xFF0B1220),
                surface = Color(0xFF131E30), onPrimary = Color(0xFF00382E)
            )) {
                BridgeScreen()
            }
        }
    }

    private fun updateSpeech(message: String) { runOnUiThread { speechState = message } }

    @Composable
    private fun BridgeScreen() {
        val connected by BridgeAccessibilityService.connected.collectAsStateWithLifecycle()
        var lastAction by rememberSaveable { mutableStateOf("No action requested") }
        var showDisclosure by remember { mutableStateOf(false) }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("HERMES / ANDROID", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                Text("Your agent.\nYour phone.", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text("Companion setup • v0.1.1", color = MaterialTheme.colorScheme.onSurfaceVariant)
                StatusCard("01  Accessibility", if (connected) "Service connected" else "Service not connected") {
                    Text("Home ও Back action পরীক্ষা করতে এই service নিজে enable করুন। এই version screen content পড়ে না।")
                    Button(onClick = { showDisclosure = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Accessibility settings")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(enabled = connected, modifier = Modifier.weight(1f), onClick = {
                            lastAction = "Home requested"
                            lastAction = if (BridgeAccessibilityService.home()) "Home accepted by Android; verify visually" else "Home rejected by Android"
                        }) { Text("Test Home") }
                        OutlinedButton(enabled = connected, modifier = Modifier.weight(1f), onClick = {
                            lastAction = "Back requested"
                            lastAction = if (BridgeAccessibilityService.back()) "Back accepted by Android; verify visually" else "Back rejected by Android"
                        }) { Text("Test Back") }
                    }
                    Text(lastAction, style = MaterialTheme.typography.bodySmall)
                }
                StatusCard("02  Voice output", speechState) {
                    Text("এটি Android app-এর আলাদা TTS test। আগের Termux TTS configuration পরিবর্তন করবে না।")
                    Text("কথার গতি: ${String.format(Locale.US, "%.2f", speechRate)}", style = MaterialTheme.typography.titleSmall)
                    Text("বামে নিলে ধীরে, ডানে নিলে দ্রুত বলবে। Speed বদলে আবার Test চাপুন।", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            tts?.stop()
                            speechRate = it
                            speechState = if (ttsReady) "Speed changed. Tap Test voice output." else "Speech engine not ready"
                        },
                        onValueChangeFinished = {
                            voicePreferences.edit().putFloat("speech_rate", speechRate).apply()
                        },
                        valueRange = 0.25f..1.25f,
                        steps = 19
                    )
                    TextButton(onClick = {
                        tts?.stop()
                        speechRate = 0.70f
                        voicePreferences.edit().putFloat("speech_rate", speechRate).apply()
                        speechState = if (ttsReady) "Slower default restored. Tap Test voice output." else "Speech engine not ready"
                    }) { Text("Reset speed • 0.70") }
                    Button(enabled = ttsReady, onClick = {
                        if (tts?.setSpeechRate(speechRate) != TextToSpeech.SUCCESS) {
                            speechState = "Speech engine could not apply this speed"
                        } else {
                            speechState = "Speech queued…"
                            val result = tts?.speak("Hermes Android Bridge is ready for device testing.",
                                TextToSpeech.QUEUE_FLUSH, null, "device-test")
                            if (result != TextToSpeech.SUCCESS) speechState = "Speech could not be queued"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Test voice output") }
                    TextButton(onClick = { tts?.stop(); speechState = "Speech stopped" }) { Text("Stop speech") }
                }
                StatusCard("03  Hermes connection", "Not connected • next milestone") {
                    Text("Secure local bridge ও chat/voice input এখনও যুক্ত হয়নি। এই app থেকে এখন Hermes command পাঠানো যাবে না।")
                }
                Text("Developer Options প্রয়োজন নেই। কোনো setting আপনার EMI policy দ্বারা blocked থাকলে bypass করবেন না; পরীক্ষার ফল জানান।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showDisclosure) AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Enable manual action tests?") },
            text = { Text("এই development version শুধু আপনার tap করা Home ও Back test চালায়। Screen পড়া, typing, gesture বা Hermes remote action নেই। Settings থেকে service যেকোনো সময় বন্ধ করতে পারবেন।") },
            confirmButton = { TextButton(onClick = {
                showDisclosure = false
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                catch (_: ActivityNotFoundException) { lastAction = "Accessibility settings unavailable on this device" }
                catch (_: SecurityException) { lastAction = "Accessibility settings blocked by device policy" }
            }) { Text("Continue to settings") } },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text("Cancel") } }
        )
    }

    @Composable
    private fun StatusCard(title: String, status: String, content: @Composable ColumnScope.() -> Unit) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            content()
        }
    }

    override fun onStop() {
        tts?.stop()
        if (ttsReady) speechState = "Speech engine ready"
        super.onStop()
    }

    override fun onDestroy() { tts?.shutdown(); tts = null; super.onDestroy() }
}
