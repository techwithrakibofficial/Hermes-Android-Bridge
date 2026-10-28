package io.github.techwithrakibofficial.hermesbridge

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatLine(val role: String, val text: String, val id: String = UUID.randomUUID().toString(), val conversationId: String = "")
data class ChatSummary(val id: String, val title: String)
data class ToolApproval(val id: String, val title: String, val canAllow: Boolean)
data class CompanionState(
    val lines: List<ChatLine> = emptyList(), val conversations: List<ChatSummary> = emptyList(), val conversationId: String = "", val phase: String = "idle", val status: String = "প্রথমে Bridge setup ও companion server চালু করুন।",
    val connected: Boolean = false, val model: String = "Existing Hermes", val busy: Boolean = false,
    val partial: String = "", val automatic: Boolean = false, val speakReplies: Boolean = true,
    val networkSpeech: Boolean = false, val language: String = "bn-BD", val approval: ToolApproval? = null,
    val progress: List<String> = emptyList()
)

class CompanionRuntime private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("companion", Context.MODE_PRIVATE)
    private val gate = VoiceTurnGate()
    private val api = CompanionApi()
    private val input = SpeechInput(context)
    private var voice = VoiceOutput(context).also { it.refresh() }
    private val audio = context.getSystemService(AudioManager::class.java)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener { change -> if (change < 0) pauseVoice("Audio focus changed; voice paused") }.build()
    private var conversation = prefs.getString("conversation", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("conversation", it).apply() }
    private var active = prefs.getString("active_job", null)
    private var jobToken = ""
    private var polling: Job? = null
    private var canCapture = false
    private val suppressedJobs = mutableSetOf<String>()
    private var voiceUntil = 0L
    private var history = readHistory()
    private val mutable = MutableStateFlow(CompanionState(lines = history.filter { it.conversationId == conversation }, conversations = summaries(), conversationId = conversation, busy = active != null,
        speakReplies = prefs.getBoolean("speak", true), networkSpeech = prefs.getBoolean("network_speech", false),
        language = prefs.getString("language", "bn-BD") ?: "bn-BD"))
    val state = mutable.asStateFlow()
    var onIdle: (() -> Unit)? = null

    init {
        scope.launch {
            BridgeAccessibilityService.bridge.collect { bridge ->
                if (!bridge.running && (state.value.phase == "listening" || state.value.automatic || state.value.busy)) stopTask("Android bridge stopped")
            }
        }
        scope.launch {
            SpeechOutputGate.speaking.collect { speaking ->
                if (speaking && gate.state() == VoiceTurnGate.State.LISTENING) pauseVoice("অন্য speech চলছে। শেষ হলে mic চাপুন।")
            }
        }
    }
    private fun readHistory(): List<ChatLine> = try {
        val rows = JSONArray(prefs.getString("messages", "[]"))
        (0 until rows.length()).map { val row = rows.getJSONObject(it); ChatLine(row.getString("role"), row.getString("text"), row.getString("id"), row.optString("conversation", conversation)) }.takeLast(100)
    } catch (_: Exception) { emptyList() }
    private fun summaries(): List<ChatSummary> = history.map { it.conversationId }.distinct().takeLast(10).reversed().map { id ->
        ChatSummary(id, history.firstOrNull { it.conversationId == id && it.role == "user" }?.text?.take(60) ?: "নতুন conversation")
    }
    private fun line(role: String, text: String) {
        history = (history + ChatLine(role, text.take(24000), conversationId = conversation)).takeLast(100)
        val keep = history.map { it.conversationId }.distinct().takeLast(10).toSet()
        history = history.filter { it.conversationId in keep }
        mutable.value = state.value.copy(lines = history.filter { it.conversationId == conversation }, conversations = summaries(), conversationId = conversation)
        prefs.edit().putString("messages", JSONArray(history.map { JSONObject().put("role", it.role).put("text", it.text).put("id", it.id).put("conversation", it.conversationId) }).toString()).apply()
    }
    fun selectChat(id: String) {
        if (active != null || history.none { it.conversationId == id }) return
        pauseVoice("Conversation selected")
        conversation = id; prefs.edit().putString("conversation", id).apply()
        mutable.value = state.value.copy(lines = history.filter { it.conversationId == id }, conversationId = id)
        status("idle", "আগের conversation চালিয়ে যেতে পারেন।")
    }
    private fun token() = BridgeAccessibilityService.bridge.value.takeIf { it.running }?.token.orEmpty()
    private fun status(phase: String, text: String) { mutable.value = state.value.copy(phase = phase, status = text, automatic = gate.automatic()) }
    fun options(speak: Boolean = state.value.speakReplies, network: Boolean = state.value.networkSpeech, language: String = state.value.language) {
        pauseVoice("Voice preferences updated; tap mic when ready")
        prefs.edit().putBoolean("speak", speak).putBoolean("network_speech", network).putString("language", language).apply()
        mutable.value = state.value.copy(speakReplies = speak, networkSpeech = network, language = language)
    }
    fun refreshVoice() {
        if (gate.state() == VoiceTurnGate.State.SPEAKING) return
        voice.close()
        voice = VoiceOutput(context).also { it.refresh() }
    }
    fun connect() {
        val auth = token()
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.call(auth, "/v1/health") }
                mutable.value = state.value.copy(connected = true, model = response.optString("model", "Hermes"))
                if (active != null && polling?.isActive != true) { jobToken = auth; poll(active!!, gate.request()) }
                else if (active == null) status("idle", "Hermes connected। লিখুন বা mic চাপুন।")
            } catch (e: Exception) {
                mutable.value = state.value.copy(connected = false)
                status("paused", readable(e.message))
            }
        }
    }
    private fun readable(message: String?): String = when {
        message?.contains("provider_credit_limit") == true -> "Provider credit/budget limit। Voice paused; একই কাজ আবার পাঠাবেন না।"
        message?.contains("provider_rate_limit") == true -> "Provider rate limit। কিছুক্ষণ পরে নতুন চেষ্টা করুন; auto retry বন্ধ।"
        message?.contains("pair") == true -> "বর্তমান bridge token দিয়ে Termux pairing নবায়ন করুন।"
        message?.contains("agent_busy") == true -> "Hermes-এর আগের companion task চলছে। তার ফল যাচাই করুন।"
        message?.contains("ECONNREFUSED") == true || message?.contains("connect", true) == true -> "Companion server পাওয়া যায়নি। Termux launcher চালু করে Reconnect চাপুন।"
        else -> (message ?: "Connection interrupted; reconnect to inspect the same task").take(300)
    }
    fun send(text: String) {
        val value = text.trim()
        if (value.isEmpty() || active != null) return
        input.stop()
        val ticket = gate.request()
        voice.stop()
        val auth = token()
        if (auth.isBlank()) { pauseVoice("Bridge setup থেকে local bridge Start করুন।"); return }
        val request = UUID.randomUUID().toString()
        active = request; jobToken = auth
        prefs.edit().putString("active_job", request).apply()
        line("user", value)
        mutable.value = state.value.copy(busy = true, partial = "", progress = emptyList(), approval = null)
        status("thinking", "Hermes কাজ করছে…")
        polling = scope.launch {
            try {
                withContext(Dispatchers.IO) { api.call(auth, "/v1/jobs", JSONObject().put("request_id", request).put("conversation_id", conversation).put("text", value.take(8000))) }
                poll(request, ticket)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is CompanionApi.Rejected && e.code in 400..499 && e.message != "request_id_conflict") {
                    active = null; prefs.edit().remove("active_job").apply()
                    mutable.value = state.value.copy(busy = false)
                }
                // Ambiguous transport failure keeps ID for GET reconciliation; never resend text.
                gate.pause(); mutable.value = state.value.copy(connected = false)
                status("paused", readable(e.message) + " Reconnect দিয়ে একই task-এর অবস্থা দেখুন।")
                onIdle?.invoke()
            }
        }
    }
    private fun poll(id: String, ticket: Long) {
        polling = scope.launch {
            var failures = 0
            while (active == id) {
                try {
                    val response = withContext(Dispatchers.IO) { api.call(token().ifBlank { jobToken }, "/v1/jobs/$id") }
                    failures = 0
                    if (active != id) return@launch
                    val phase = response.optString("state")
                    val rows = response.optJSONArray("progress") ?: JSONArray()
                    val approval = response.optJSONObject("approval")
                    mutable.value = state.value.copy(connected = true, progress = (0 until rows.length()).map { rows.optString(it) },
                        approval = approval?.let { ToolApproval(it.getString("id"), it.getString("title"), it.optBoolean("can_allow")) })
                    if (phase in listOf("completed", "failed", "cancelled", "interrupted")) {
                        active = null; prefs.edit().remove("active_job").apply()
                        mutable.value = state.value.copy(busy = false, approval = null)
                        if (phase == "completed") {
                            val reply = response.optString("reply")
                            line("assistant", reply)
                            if (id !in suppressedJobs && gate.ticket() == ticket && state.value.speakReplies && !response.optBoolean("external_speech") && voice.ready && requestFocus()) speak(reply, ticket)
                            else {
                                gate.ready(ticket)
                                if (gate.automatic()) pauseVoice("উত্তর screen-এ আছে। Voice প্রস্তুত করে mic চাপুন।")
                                else status("idle", "উত্তর এসেছে।")
                                onIdle?.invoke()
                            }
                        } else {
                            gate.pause(); status("paused", readable(response.optString("error", phase)))
                            onIdle?.invoke()
                        }
                        return@launch
                    }
                    status(if (phase == "approval") "approval" else "thinking", if (phase == "approval") "Tool চালাতে আপনার সিদ্ধান্ত দরকার।" else "Hermes কাজ করছে…")
                    delay(1000)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    failures++
                    if (failures >= 3) {
                        if (e is CompanionApi.Rejected && e.message == "job_not_found") {
                            active = null; prefs.edit().remove("active_job").apply()
                            mutable.value = state.value.copy(busy = false)
                            line("notice", "Task record পাওয়া যায়নি। ফোনের ফল যাচাই করুন; কোনো prompt auto resend হয়নি।")
                        }
                        gate.pause(); mutable.value = state.value.copy(connected = false)
                        status("paused", readable(e.message) + " Task ID রাখা আছে; Reconnect করুন।")
                        onIdle?.invoke(); return@launch
                    }
                    delay(failures * 1000L) // Read-only reconciliation only, never repeat POST.
                }
            }
        }
    }
    fun approve(allow: Boolean) {
        val id = active ?: return
        val approval = state.value.approval ?: return
        scope.launch {
            try { withContext(Dispatchers.IO) { api.call(token(), "/v1/jobs/$id/approval", JSONObject().put("approval_id", approval.id).put("allow", allow)) } }
            catch (e: Exception) { status("approval", readable(e.message)) }
        }
    }
    private fun requestFocus(): Boolean {
        if (audio.mode in listOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)) return false
        return try { audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED } catch (_: Exception) { false }
    }
    fun allowCapture(allowed: Boolean) { canCapture = allowed }
    fun automatic(enabled: Boolean) { gate.setAutomatic(enabled); mutable.value = state.value.copy(automatic = enabled) }
    fun listen() {
        if (active != null || !canCapture) return
        val automatic = gate.automatic()
        gate.stop(); input.stop(); voice.stop(); SpeechOutputGate.stopAll()
        gate.setAutomatic(automatic)
        if (!requestFocus()) { pauseVoice("Audio focus/call is busy; mic paused"); return }
        val ticket = gate.listen()
        if (ticket < 0) return
        if (voiceUntil == 0L) {
            voiceUntil = android.os.SystemClock.elapsedRealtime() + 15 * 60_000L
            handler.postDelayed({ if (voiceUntil > 0 && android.os.SystemClock.elapsedRealtime() >= voiceUntil) pauseVoice("১৫ মিনিট voice session শেষ। আবার শুরু করতে mic চাপুন।") }, 15 * 60_000L)
        }
        status("listening", "শুনছি… কথা শেষ হলে একটি turn পাঠানো হবে।")
        input.listen(state.value.language, state.value.networkSpeech,
            { partial -> if (ticket == gate.ticket()) mutable.value = state.value.copy(partial = partial) },
            { text ->
                if (gate.finalResult(ticket)) {
                    if (text.lowercase().trim() in listOf("stop", "থামো", "বন্ধ করো", "থামুন")) stopTask("Voice stopped") else send(text)
                }
            },
            { error -> if (ticket == gate.ticket()) pauseVoice(error) })
    }
    private fun speak(text: String, ticket: Long) {
        if (!gate.speak(ticket)) return
        val clean = text.replace(Regex("```[\\s\\S]*?```"), " Code screen-এ দেওয়া আছে। ").replace(Regex("https?://\\S+"), " link screen-এ আছে ").take(6000)
        val chunks = SpeechChunks.split(clean, 1800)
        status("speaking", "বলছি… কথা বলতে mic চাপলে এই speech থামবে।")
        fun next(index: Int) {
            if (gate.ticket() != ticket || gate.state() != VoiceTurnGate.State.SPEAKING) return
            if (index >= chunks.size) {
                val again = gate.speechDone(ticket, true)
                status("idle", "উত্তর শেষ।")
                if (again) handler.postDelayed({
                    if (gate.ticket() == ticket && gate.automatic() && canCapture && !SpeechOutputGate.speaking.value) listen()
                }, 500) else { runCatching { audio.abandonAudioFocusRequest(focus) }; onIdle?.invoke() }
                return
            }
            if (!voice.speak(chunks[index]) finished@ { ok ->
                    if (gate.ticket() != ticket) return@finished
                    if (ok) next(index + 1) else pauseVoice("Speech stopped; mic paused")
                }) pauseVoice("Voice unavailable; reply is on screen")
        }
        next(0)
    }
    fun pauseVoice(reason: String) {
        gate.pause(); input.stop(); voice.stop(); voiceUntil = 0
        handler.removeCallbacksAndMessages(null)
        runCatching { audio.abandonAudioFocusRequest(focus) }
        mutable.value = state.value.copy(partial = "", automatic = false)
        if (active == null) status("paused", reason)
        if (active == null) onIdle?.invoke()
    }
    fun stopTask(reason: String = "Stop requested") {
        pauseVoice(reason); SpeechOutputGate.stopAll()
        val id = active ?: return
        suppressedJobs.add(id)
        polling?.cancel()
        val auth = token().ifBlank { jobToken }
        status("stopping", "Stop পাঠানো হচ্ছে। আগে শুরু হওয়া action হয়ে থাকতে পারে।")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.call(auth, "/v1/jobs/$id/cancel", JSONObject()) }
                poll(id, gate.ticket())
            } catch (e: Exception) { status("paused", "Local mic/speech থেমেছে। Task Stop confirm হয়নি; Reconnect করুন।"); onIdle?.invoke() }
        }
    }
    fun newChat(clear: Boolean = false) {
        if (active != null) return
        pauseVoice("New conversation")
        conversation = UUID.randomUUID().toString()
        prefs.edit().putString("conversation", conversation).apply()
        if (clear) { history = emptyList(); prefs.edit().remove("messages").apply(); mutable.value = state.value.copy(lines = emptyList(), conversations = emptyList(), conversationId = conversation) }
        else line("notice", "নতুন conversation")
        status("idle", "নতুন conversation প্রস্তুত।")
    }
    companion object {
        @Volatile private var instance: CompanionRuntime? = null
        fun get(context: Context): CompanionRuntime = instance ?: synchronized(this) {
            instance ?: CompanionRuntime(context.applicationContext).also { instance = it }
        }
    }
}
