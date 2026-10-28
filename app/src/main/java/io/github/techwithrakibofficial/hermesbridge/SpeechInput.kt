package io.github.techwithrakibofficial.hermesbridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Exactly one recognition request; never restarts itself after an error/silence. */
class SpeechInput(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0L
    fun stop() {
        generation++
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
    fun listen(language: String, networkAllowed: Boolean, partial: (String) -> Unit,
               result: (String) -> Unit, failure: (String) -> Unit): Boolean {
        if (recognizer != null) return false
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            failure("Microphone permission required"); return false
        }
        if (SpeechOutputGate.speaking.value) { failure("Speech is playing; tap mic after it finishes"); return false }
        val ticket = ++generation
        fun fail(message: String) { if (ticket == generation) { stop(); failure(message) } }
        val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (!networkAllowed && !onDevice) {
            failure("On-device recognition is unavailable. Enable network speech explicitly or use text."); return false
        }
        try {
            val instance = if (!networkAllowed && Build.VERSION.SDK_INT >= 31)
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) { failure("Install/enable an Android speech recognition service"); return false }
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            recognizer = instance
            instance.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (ticket == generation) partial("শুনছি…") }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit // Never retain audio.
                override fun onEndOfSpeech() { if (ticket == generation) partial("কথা বুঝে নিচ্ছি…") }
                override fun onError(error: Int) {
                    val reason = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কথা পাওয়া যায়নি। আবার বলতে mic চাপুন।"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy. Mic paused; tap again when ready."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission was denied/revoked"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "এই ভাষার recognition data নেই। Speech settings দেখুন বা network speech allow করুন।"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech network unavailable; mic paused"
                        else -> "Speech recognition stopped (code $error). Tap mic to resume."
                    }
                    fail(reason)
                }
                override fun onResults(results: Bundle?) {
                    if (ticket != generation) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    stop()
                    if (text.isBlank()) failure("কথা পাওয়া যায়নি। Mic চাপুন।") else result(text.take(8000))
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (ticket == generation) partial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().take(500))
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            instance.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !networkAllowed)
            })
            handler.postDelayed({
                if (ticket == generation) {
                    instance.stopListening()
                    handler.postDelayed({ fail("Speech result timed out. Mic paused; tap to resume.") }, 8000)
                }
            }, 25000)
            return true
        } catch (_: Exception) { fail("Speech service unavailable. Use text or check Android speech settings."); return false }
    }
}
