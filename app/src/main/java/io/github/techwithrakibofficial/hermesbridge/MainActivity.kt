package io.github.techwithrakibofficial.hermesbridge

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.os.PersistableBundle
import android.widget.Toast
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.net.Uri
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
    private lateinit var voice: VoiceOutput

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voice = VoiceOutput(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF5EEAD4), background = Color(0xFF0B1220),
                surface = Color(0xFF131E30), onPrimary = Color(0xFF00382E)
            )) {
                BridgeScreen()
            }
        }
    }

    @Composable
    private fun BridgeScreen() {
        val connected by BridgeAccessibilityService.connected.collectAsStateWithLifecycle()
        val bridge by BridgeAccessibilityService.bridge.collectAsStateWithLifecycle()
        var showBridgeConsent by remember { mutableStateOf(false) }
        var lastAction by rememberSaveable { mutableStateOf("No action requested") }
        var showDisclosure by remember { mutableStateOf(false) }
        var showEngines by remember { mutableStateOf(false) }
        var showVoices by remember { mutableStateOf(false) }
        if (showEngines) VoicePicker("Choose speech engine",
            voice.engines.map { it.label }, { showEngines = false }) {
            voice.selectEngine(voice.engines[it].name)
            showEngines = false
        }
        if (showVoices) VoicePicker("Choose voice",
            voice.voices.map { voice.voiceLabel(it) }, { showVoices = false }) {
            voice.selectVoice(voice.voices[it])
            showVoices = false
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("HERMES / ANDROID", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                Text("Your agent.\nYour phone.", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text("Companion setup • v0.3.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                StatusCard("02  Voice output", voice.status) {
                    Text("সুন্দর উচ্চারণের জন্য সঠিক language ও voice বাছুন। Voice-এর মান installed engine-এর ওপর নির্ভর করে।")
                    Text("Engine: ${voice.engineLabel}", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(enabled = voice.engines.isNotEmpty(), onClick = { showEngines = true },
                        modifier = Modifier.fillMaxWidth()) { Text("Choose speech engine") }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(selected = voice.language == "bn-BD", onClick = { voice.selectLanguage("bn-BD") }, label = { Text("বাংলা") })
                        FilterChip(selected = voice.language == "en-US", onClick = { voice.selectLanguage("en-US") }, label = { Text("English") })
                    }
                    Text(voice.selectedVoice?.let { voice.voiceLabel(it) } ?: "No usable voice selected",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled = voice.voices.isNotEmpty(), onClick = { showVoices = true },
                        modifier = Modifier.fillMaxWidth()) { Text("Choose voice • ${voice.voices.size} available") }
                    Row {
                        Checkbox(checked = voice.allowNetwork, onCheckedChange = { voice.setNetworkAllowed(it) })
                        Text("Online voice allow করুন। Engine text online পাঠাতে পারে এবং internet লাগবে।",
                            modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("কথার গতি: ${String.format(Locale.US, "%.2f", voice.rate)}", style = MaterialTheme.typography.titleSmall)
                    Slider(value = voice.rate, onValueChange = { voice.updateRate(it) },
                        onValueChangeFinished = { voice.saveRate() }, valueRange = 0.25f..1.25f, steps = 19)
                    TextButton(onClick = { voice.updateRate(0.70f); voice.saveRate() }) { Text("Reset speed • 0.70") }
                    Button(enabled = voice.ready, onClick = { voice.speakTest() },
                        modifier = Modifier.fillMaxWidth()) { Text("Test selected voice") }
                    TextButton(onClick = { voice.stop() }) { Text("Stop speech") }
                    OutlinedButton(onClick = { openVoiceData() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Install voice data")
                    }
                    TextButton(onClick = {
                        openSettingsIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")))
                    }) { Text("Google voice • install / update") }
                    TextButton(onClick = { voice.refresh() }) { Text("Refresh engines & voices") }
                    Text("Voice ও speed save থাকবে। Termux বা system TTS setting পরিবর্তন করা হয় না।",
                        style = MaterialTheme.typography.bodySmall)
                }
                StatusCard("03  Local Android bridge", bridge.message) {
                    Text("Hermes/Termux থেকে secret token দিয়ে Home, Back, Settings ও নির্দিষ্ট app খোলার command চালাতে পারবেন। App-এর নিজস্ব chat UI এখনও যুক্ত হয়নি।")
                    Button(enabled = connected && !bridge.running, onClick = { showBridgeConsent = true },
                        modifier = Modifier.fillMaxWidth()) { Text("Start local bridge") }
                    if (bridge.running) {
                        Text("Local address: http://127.0.0.1:18765", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            val command = "export HERMES_BRIDGE_TOKEN='${bridge.token}' HERMES_BRIDGE_URL='http://127.0.0.1:18765'"
                            val clip = ClipData.newPlainText("Hermes local pairing", command)
                            clip.description.extras = PersistableBundle().apply {
                                putBoolean("android.content.extra.IS_SENSITIVE", true)
                            }
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
                            Toast.makeText(this@MainActivity, "Pairing command copied. Paste only in your Termux.", Toast.LENGTH_LONG).show()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Copy Termux pairing command") }
                        Text("Copied command শুধু নিজের Termux-এ paste করুন। এতে secret token থাকে। Stop করলে token বাতিল হবে।",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { BridgeAccessibilityService.stopBridge() },
                            modifier = Modifier.fillMaxWidth()) { Text("Stop local bridge") }
                    }
                    Text("Available: Home • Back • Settings • Open supported apps", style = MaterialTheme.typography.bodySmall)
                    Text("App খোলা যাবে। Screen পড়া, click/type বা message পাঠানো এখনও যুক্ত হয়নি।", style = MaterialTheme.typography.bodySmall)
                    Text(bridge.lastAction, style = MaterialTheme.typography.bodySmall)
                    Text("Android request গ্রহণ করলেও ফোনের screen দেখে action নিশ্চিত করুন।",
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Developer Options প্রয়োজন নেই। কোনো setting আপনার EMI policy দ্বারা blocked থাকলে bypass করবেন না; পরীক্ষার ফল জানান।",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showBridgeConsent) AlertDialog(
            onDismissRequest = { showBridgeConsent = false },
            title = { Text("Allow local Android commands?") },
            text = { Text("Bridge চালু থাকলে এই ফোনের secret token থাকা client Home, Back, Settings ও WhatsApp/Facebook/Messenger/Telegram/Chrome/Termux খুলতে পারবে। এটি শুধু 127.0.0.1-এ শোনে। আপনি Stop চাপলে, Accessibility বন্ধ করলে বা process বন্ধ হলে session শেষ হবে।") },
            confirmButton = { TextButton(onClick = {
                showBridgeConsent = false
                BridgeAccessibilityService.startBridge()
            }) { Text("Start bridge") } },
            dismissButton = { TextButton(onClick = { showBridgeConsent = false }) { Text("Cancel") } }
        )
        if (showDisclosure) AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Enable manual action tests?") },
            text = { Text("এই version Home/Back button test এবং আপনি local bridge Start করলে authenticated Android command চালায়। Screen পড়া, typing, message পাঠানো বা gesture নেই। Settings থেকে service যেকোনো সময় বন্ধ করতে পারবেন।") },
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

    @Composable
    private fun VoicePicker(title: String, items: List<String>, dismiss: () -> Unit, select: (Int) -> Unit) {
        AlertDialog(onDismissRequest = dismiss, title = { Text(title) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { index, label ->
                        TextButton(onClick = { select(index) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = dismiss) { Text("Close") } })
    }

    private fun openSettingsIntent(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { openGeneralSettings() }
        catch (_: SecurityException) { openGeneralSettings() }
    }

    private fun openGeneralSettings() {
        try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        catch (_: ActivityNotFoundException) { android.widget.Toast.makeText(this, "Settings unavailable", android.widget.Toast.LENGTH_LONG).show() }
        catch (_: SecurityException) { android.widget.Toast.makeText(this, "Settings blocked by device policy", android.widget.Toast.LENGTH_LONG).show() }
    }

    private fun openVoiceData() {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        if (voice.engineName.isNotBlank()) intent.setPackage(voice.engineName)
        openSettingsIntent(intent)
    }

    override fun onResume() { super.onResume(); voice.refresh() }
    override fun onStop() { voice.stop(); super.onStop() }
    override fun onDestroy() { voice.close(); super.onDestroy() }
}
