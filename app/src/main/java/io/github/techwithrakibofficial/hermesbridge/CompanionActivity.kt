package io.github.techwithrakibofficial.hermesbridge

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class CompanionActivity : ComponentActivity() {
    private lateinit var runtime: CompanionRuntime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtime = CompanionRuntime.get(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme(
            primary = Color(0xFF66E2C5), secondary = Color(0xFFA7B8FF),
            background = Color(0xFF0C1120), surface = Color(0xFF161F33), onPrimary = Color(0xFF00372D)
        )) { Home() } }
    }
    override fun onResume() { super.onResume(); runtime.refreshVoice(); runtime.connect() }

    private fun copy(text: String) {
        val clip = ClipData.newPlainText("Hermes companion", text)
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
    private fun startMic(automatic: Boolean) {
        try { CompanionService.start(this, CompanionService.LISTEN, automatic = automatic) }
        catch (_: Exception) { runtime.pauseVoice("Open the app and check microphone/foreground permissions") }
    }
    @Composable private fun Home() {
        val state by runtime.state.collectAsStateWithLifecycle()
        val bridge by BridgeAccessibilityService.bridge.collectAsStateWithLifecycle()
        var page by rememberSaveable { mutableIntStateOf(0) }
        var draft by rememberSaveable { mutableStateOf("") }
        var requestedAutomatic by remember { mutableStateOf(false) }
        var micDisclosure by remember { mutableStateOf(false) }
        var clearDialog by remember { mutableStateOf(false) }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted[Manifest.permission.RECORD_AUDIO] == true || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startMic(requestedAutomatic)
            else runtime.pauseVoice("Mic permission denied. Text chat remains available.")
        }
        fun requestMic(automatic: Boolean) {
            requestedAutomatic = automatic
            if (!getPreferences(MODE_PRIVATE).getBoolean("mic_consent", false)) { micDisclosure = true; return }
            val wanted = mutableListOf<String>()
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) wanted.add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
            if (wanted.isEmpty()) startMic(automatic) else permission.launch(wanted.toTypedArray())
        }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("HERMES", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text("তোমার companion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    TextButton(enabled = !state.busy, onClick = { runtime.newChat() }) { Text("New chat") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (bridge.running) "Android ●" else "Android setup needed", color = if (bridge.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Text(if (state.connected) "Hermes ●" else "Hermes offline", color = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Text(state.model, Modifier.padding(horizontal = 18.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                TabRow(selectedTabIndex = page) {
                    listOf("Chat", "Voice", "Connection").forEachIndexed { index, label -> Tab(selected = page == index, onClick = { page = index }, text = { Text(label) }) }
                }
                if (page == 0) {
                    val scroll = rememberLazyListState()
                    LaunchedEffect(state.lines.lastOrNull()?.id) {
                        if (state.lines.isNotEmpty()) scroll.animateScrollToItem(state.lines.size - 1)
                    }
                    LazyColumn(state = scroll, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                        if (state.lines.isEmpty()) item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("বলুন, আজ কী করব?", style = MaterialTheme.typography.headlineMedium)
                                Text("গল্প, পরিকল্পনা, coding বা ফোনের কাজ। প্রথমে Connection থেকে বর্তমান bridge ও companion server যুক্ত করুন।")
                                Text("শুধু task গ্রহণ করা মানেই কাজ সম্পন্ন নয়; screen/result যাচাই করতে বলুন।", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        items(state.lines, key = { it.id }) { message ->
                            Column(Modifier.fillMaxWidth().background(if (message.role == "user") Color(0xFF203A4C) else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(14.dp)) {
                                Text(if (message.role == "user") "তুমি" else if (message.role == "notice") "Conversation" else "Hermes", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                SelectionContainer { Text(message.text, Modifier.padding(top = 6.dp)) }
                                TextButton(onClick = { copy(message.text) }) { Text("Copy") }
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (page == 1) {
                            item { Text("কথা → কাজ → উত্তর", style = MaterialTheme.typography.headlineMedium) }
                            item { Text("এক পালায় শুনবে, তারপর উত্তর বলবে। কথা শেষের final result একবার পাঠানো হবে। নিজের speech চললে mic বন্ধ। Silence/error হলে pause হবে।") }
                            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("উত্তরের পরে আবার শুনবে", Modifier.weight(1f))
                                Switch(checked = state.automatic, enabled = bridge.running && state.connected && !state.busy, onCheckedChange = { if (it) requestMic(true) else runtime.pauseVoice("Conversation mode paused") })
                            } }
                            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("উত্তর পড়ে শোনাবে", Modifier.weight(1f)); Switch(checked = state.speakReplies, onCheckedChange = { runtime.options(speak = it) })
                            } }
                            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(selected = state.language == "bn-BD", onClick = { runtime.options(language = "bn-BD") }, label = { Text("বাংলা mic") })
                                FilterChip(selected = state.language == "en-US", onClick = { runtime.options(language = "en-US") }, label = { Text("English mic") })
                            } }
                            item { Row {
                                Checkbox(checked = state.networkSpeech, onCheckedChange = { runtime.options(network = it) })
                                Text("Network speech allow করি। Android speech service audio online পাঠাতে পারে। Off রাখলে supported on-device recognition লাগবে।", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
                            } }
                            item { Button(onClick = { runtime.pauseVoice("Voice setup opened"); startActivity(Intent(this@CompanionActivity, MainActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("কণ্ঠ, language ও speed বাছুন") } }
                            item { OutlinedButton(onClick = { try { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) } catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) } }, modifier = Modifier.fillMaxWidth()) { Text("Android speech settings") } }
                            item { Text("App বাড়তি beep দেয় না। Recognizer-এর নিজস্ব sound ফোন/engine নিয়ন্ত্রণ করে। Busy/error-তে auto restart নেই। Voice session ১৫ মিনিটে pause হবে; mic চাপলে নতুন session।", style = MaterialTheme.typography.bodySmall) }
                        } else {
                            item { Text("Existing Hermes, একই ফোনে", style = MaterialTheme.typography.headlineMedium) }
                            item { Text("Android bridge: ${bridge.message}\nCompanion: ${state.status}") }
                            item { Button(onClick = { runtime.pauseVoice("Bridge setup opened"); startActivity(Intent(this@CompanionActivity, MainActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Open Bridge setup") } }
                            item { OutlinedButton(onClick = { try { CompanionService.start(this@CompanionActivity, CompanionService.RESUME) } catch (_: Exception) { runtime.connect() } }, modifier = Modifier.fillMaxWidth()) { Text("Reconnect / check current task") } }
                            item { OutlinedButton(onClick = { copy("proot-distro login ubuntu -- /root/.local/share/hermes-android-bridge/hermes-companion-all") }, modifier = Modifier.fillMaxWidth()) { Text("Copy Termux companion launcher") } }
                            item { Text("প্রথমবার pinned companion setup চালাতে হবে। Gateway/Telegram/cron আলাদা চলবে। App-এ provider API key দিতে হয় না। Bridge restart করলে pairing নবায়ন করুন।", style = MaterialTheme.typography.bodySmall) }
                            item { Text("Chat history শুধু app-এর private storage-এ থাকে; Hermes নিজস্ব session history-ও রাখে। Microphone audio file save হয় না। Network speech/AI provider-এর নিজস্ব data policy প্রযোজ্য।", style = MaterialTheme.typography.bodySmall) }
                            item { TextButton(enabled = !state.busy, onClick = { clearDialog = true }) { Text("Clear app chat history") } }
                            item { Text("0.5.0 • Development / phone acceptance build\nFree model-এর quota থাকে। Carrier call-এর ভিতরে AI audio নেই।", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(state.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 3)
                    state.progress.takeLast(2).forEach { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    if (state.partial.isNotBlank()) Text(state.partial, Modifier.padding(vertical = 6.dp), maxLines = 3)
                    if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = draft, onValueChange = { draft = it.take(8000) }, modifier = Modifier.weight(1f), maxLines = 4, label = { Text("তোমার কথা / কাজ") }, enabled = !state.busy)
                        Button(enabled = draft.isNotBlank() && !state.busy && bridge.running && state.connected, onClick = {
                            val text = draft; draft = ""
                            try { CompanionService.start(this@CompanionActivity, CompanionService.SEND, text) } catch (_: Exception) { draft = text; runtime.pauseVoice("Service could not start; keep app open") }
                        }) { Text("Send") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(enabled = !state.busy && bridge.running && state.connected && state.phase != "listening", modifier = Modifier.weight(1f), onClick = { requestMic(false) }) { Text(if (state.phase == "speaking") "Interrupt & mic" else "Mic • কথা বলুন") }
                        Button(onClick = { runtime.stopTask() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Stop") }
                    }
                }
            }
        }
        if (micDisclosure) AlertDialog(onDismissRequest = { micDisclosure = false }, title = { Text("Microphone ব্যবহার করবেন?") },
            text = { Text("আপনি mic চাপলে Android speech service আপনার কথা text করবে এবং final text Hermes-এ একবার পাঠাবে। Network speech allow করলে audio service provider-এর কাছে যেতে পারে। Mic চালু থাকলে notification থাকবে। Stop-এ mic/speech থামবে।") },
            confirmButton = { TextButton(onClick = { micDisclosure = false; getPreferences(MODE_PRIVATE).edit().putBoolean("mic_consent", true).apply(); requestMic(requestedAutomatic) }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { micDisclosure = false }) { Text("Cancel") } })
        state.approval?.let { approval -> AlertDialog(onDismissRequest = {}, title = { Text("Tool approval") },
            text = { SelectionContainer { Text(approval.title) } },
            confirmButton = { TextButton(enabled = approval.canAllow, onClick = { runtime.approve(true) }) { Text("Allow once") } },
            dismissButton = { TextButton(onClick = { runtime.approve(false) }) { Text("Deny") } }) }
        if (clearDialog) AlertDialog(onDismissRequest = { clearDialog = false }, title = { Text("App history মুছবেন?") },
            text = { Text("এই app-এর saved chat মুছবে। Hermes-এর memory/session archive মুছবে না।") },
            confirmButton = { TextButton(onClick = { clearDialog = false; runtime.newChat(clear = true) }) { Text("Clear app history") } },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancel") } })
    }
}
