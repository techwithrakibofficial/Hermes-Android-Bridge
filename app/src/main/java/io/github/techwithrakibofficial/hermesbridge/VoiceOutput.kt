package io.github.techwithrakibofficial.hermesbridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.*
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** All app TTS instances share one output owner to prevent speaker/microphone feedback. */
object SpeechOutputGate {
    private var owner: VoiceOutput? = null
    private val active = MutableStateFlow(false)
    val speaking = active.asStateFlow()
    fun claim(output: VoiceOutput) {
        if (owner !== output) owner?.stop()
        owner = output
        active.value = true
    }
    fun stopAll() { owner?.stop() }
    fun release(output: VoiceOutput) {
        if (owner === output) { owner = null; active.value = false }
    }
}

/** App-scoped preferences; never changes system/Termux speech settings. */
class VoiceOutput(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("voice", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var generation = 0
    private var disposed = false
    private var utterance: String? = null
    private var completion: ((Boolean) -> Unit)? = null
    private var initialized = false
    var engines by mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList())
        private set
    var engineName by mutableStateOf(preferences.getString("engine", "") ?: "")
        private set
    var language by mutableStateOf(preferences.getString("language", "bn-BD") ?: "bn-BD")
        private set
    var allowNetwork by mutableStateOf(preferences.getBoolean("network_voices", false))
        private set
    var rate by mutableFloatStateOf(preferences.getFloat("speech_rate", 0.70f).coerceIn(0.25f, 1.25f))
        private set
    var voices by mutableStateOf<List<Voice>>(emptyList())
        private set
    var selectedVoice by mutableStateOf<Voice?>(null)
        private set
    var ready by mutableStateOf(false)
        private set
    var status by mutableStateOf("Starting speech engine…")
        private set
    val engineLabel: String get() = engines.find { it.name == engineName }?.label ?: "Phone default engine"

    fun refresh() { startEngine(engineName.takeIf { it.isNotBlank() }) }

    private fun startEngine(requested: String?) {
        if (disposed) return
        val ticket = ++generation
        initialized = false
        ready = false
        voices = emptyList()
        selectedVoice = null
        stop()
        runCatching { tts?.shutdown() }
        status = "Loading speech engine…"
        tts = TextToSpeech(appContext, { result ->
            // Always post so the constructor has assigned tts before this runs.
            handler.post {
                if (disposed || ticket != generation) return@post
                val engine = tts ?: return@post
                engines = engine.engines.sortedBy { it.label }
                if (requested == null) {
                    // Only choose an installed engine; no installation or system change.
                    val preferred = engines.find { it.name == "com.google.android.tts" }?.name
                        ?: engine.defaultEngine
                    if (!preferred.isNullOrBlank()) {
                        engineName = preferred
                        preferences.edit().putString("engine", preferred).apply()
                        startEngine(preferred)
                        return@post
                    }
                }
                if (requested != null && engines.none { it.name == requested }) {
                    status = "Selected engine is missing. Choose an installed engine."
                    return@post
                }
                if (result != TextToSpeech.SUCCESS) {
                    status = "Speech engine could not start. Choose another engine or install voice data."
                    return@post
                }
                initialized = true
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    private fun finish(id: String?, ok: Boolean, message: String) { handler.post {
                        if (disposed || ticket != generation || id != utterance) return@post
                        status = message
                        utterance = null
                        SpeechOutputGate.release(this@VoiceOutput)
                        val callback = completion; completion = null
                        callback?.invoke(ok)
                    } }
                    override fun onStart(utteranceId: String?) { handler.post {
                        if (!disposed && ticket == generation && utteranceId == utterance) status = "Speaking…"
                    } }
                    override fun onDone(utteranceId: String?) = finish(utteranceId, true, "Speech completed. Confirm the voice sounds clear.")
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId, false, "Speech stopped")
                    @Deprecated("Required by Android")
                    override fun onError(utteranceId: String?) = finish(utteranceId, false, "Speech failed. Check voice data.")
                    override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, false, "Speech failed (code $errorCode). Check voice data.")
                })
                loadVoices()
            }
        }, requested)
    }

    fun selectEngine(name: String) {
        if (engines.none { it.name == name }) return
        engineName = name
        preferences.edit().putString("engine", name).apply()
        startEngine(name)
    }

    fun selectLanguage(tag: String) {
        stop()
        language = tag
        preferences.edit().putString("language", tag).apply()
        loadVoices()
    }

    fun setNetworkAllowed(allowed: Boolean) {
        stop()
        allowNetwork = allowed
        preferences.edit().putBoolean("network_voices", allowed).apply()
        loadVoices()
    }

    private fun voiceKey() = "selected_voice:$engineName:$language:$allowNetwork"

    private fun loadVoices() {
        val engine = tts ?: return
        ready = false
        selectedVoice = null
        if (!initialized) return
        val locale = Locale.forLanguageTag(language)
        voices = engine.voices.orEmpty().filter {
            it.locale.language == locale.language &&
                (allowNetwork || !it.isNetworkConnectionRequired) &&
                !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }.sortedWith(compareByDescending<Voice> { it.locale.country == locale.country }
            .thenByDescending { it.quality }.thenBy { it.isNetworkConnectionRequired }.thenBy { it.name })
        val saved = preferences.getString(voiceKey(), null)
        val choice = voices.find { it.name == saved } ?: voices.firstOrNull()
        if (choice == null) {
            status = "এই ভাষার usable voice নেই। Voice data install করুন, অন্য engine বাছুন বা online voice allow করুন।"
        } else selectVoice(choice)
    }

    fun voiceLabel(voice: Voice): String {
        val number = voices.indexOfFirst { it.name == voice.name } + 1
        val place = voice.locale.getDisplayName(Locale.forLanguageTag("bn"))
        return "$place • Voice $number • ${if (voice.isNetworkConnectionRequired) "Online" else "Offline"}"
    }

    fun selectVoice(voice: Voice) {
        stop()
        if (voices.none { it.name == voice.name }) return
        if (tts?.setVoice(voice) == TextToSpeech.SUCCESS) {
            selectedVoice = voice
            ready = true
            preferences.edit().putString(voiceKey(), voice.name).apply()
            status = "Voice ready. Test করে শুনুন।"
        } else {
            selectedVoice = null
            ready = false
            status = "এই voice চালু হয়নি। অন্য voice বেছে নিন।"
        }
    }

    fun updateRate(value: Float) {
        stop()
        rate = value.coerceIn(0.25f, 1.25f)
    }
    fun saveRate() { preferences.edit().putFloat("speech_rate", rate).apply() }

    fun speakTest() {
        val text = if (language.startsWith("bn"))
            "আমি আছি, বলো আজ তোমার দিনটা কেমন গেল? চলো একসঙ্গে একটু গল্প করি, তারপর তোমার কাজের পরিকল্পনাটাও গুছিয়ে নেব।"
        else "I'm here. Tell me how your day went. We can talk for a while and plan your next steps together."
        speak(text)
    }

    fun speak(text: String, onFinished: ((Boolean) -> Unit)? = null): Boolean {
        val engine = tts ?: return false
        val voice = selectedVoice ?: return false
        if (!ready || text.length > 2000) return false
        if ((!allowNetwork && voice.isNetworkConnectionRequired) ||
            engine.setVoice(voice) != TextToSpeech.SUCCESS ||
            engine.setSpeechRate(rate) != TextToSpeech.SUCCESS ||
            engine.setPitch(1.0f) != TextToSpeech.SUCCESS) {
            status = "Voice, speed বা pitch apply হয়নি। অন্য voice চেষ্টা করুন।"
            return false
        }
        stop()
        val id = UUID.randomUUID().toString()
        utterance = id
        completion = onFinished
        SpeechOutputGate.claim(this)
        status = "Speech queued…"
        val accepted = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.SUCCESS
        if (!accepted) {
            utterance = null; completion = null
            SpeechOutputGate.release(this)
            status = "Speech could not start. Check the selected voice data."
        }
        return accepted
    }

    fun stop() {
        val callback = completion
        completion = null
        utterance = null
        runCatching { tts?.stop() }
        SpeechOutputGate.release(this)
        if (ready) status = "Speech stopped"
        callback?.invoke(false)
    }
    fun close() {
        disposed = true
        generation++
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }
}
