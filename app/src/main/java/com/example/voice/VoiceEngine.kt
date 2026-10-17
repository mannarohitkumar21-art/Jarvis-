package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.audit.AuditLogger
import com.example.model.AuditCategory
import com.example.model.AuditSeverity
import com.example.permission.JarvisPermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SupportedLanguage(val code: String, val displayName: String, val locale: Locale) {
    ENGLISH("en", "English", Locale.US),
    BENGALI("bn", "বাংলা (Bengali)", Locale.forLanguageTag("bn-BD")),
    HINDI("hi", "हिन्दी (Hindi)", Locale.forLanguageTag("hi-IN"))
}

class VoiceEngine(
    private val context: Context,
    private val permissionManager: JarvisPermissionManager
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundRmsDb = MutableStateFlow(0f)
    val soundRmsDb: StateFlow<Float> = _soundRmsDb.asStateFlow()

    private val _currentLanguage = MutableStateFlow(SupportedLanguage.ENGLISH)
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null

    init {
        initializeTts()
    }

    fun initializeTts() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = applyLanguage(_currentLanguage.value)
            _isTtsReady.value = (result >= TextToSpeech.LANG_AVAILABLE)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    AuditLogger.log(
                        category = AuditCategory.VOICE,
                        severity = AuditSeverity.WARNING,
                        action = "TTS Playback Error",
                        details = "ErrorCode: $errorCode",
                        outcome = "PLAYBACK_FAILED"
                    )
                }
            })
            AuditLogger.log(
                category = AuditCategory.VOICE,
                severity = AuditSeverity.INFO,
                action = "Init TextToSpeech",
                details = "TTS Engine initialized successfully with lang: ${_currentLanguage.value.displayName}",
                outcome = "READY"
            )
        } else {
            _isTtsReady.value = false
            AuditLogger.log(
                category = AuditCategory.VOICE,
                severity = AuditSeverity.ALERT,
                action = "Init TextToSpeech Failed",
                details = "Init status code: $status",
                outcome = "FAILED"
            )
        }
    }

    fun setLanguage(language: SupportedLanguage): Boolean {
        _currentLanguage.value = language
        val res = applyLanguage(language)
        return res >= TextToSpeech.LANG_AVAILABLE
    }

    private fun applyLanguage(language: SupportedLanguage): Int {
        val t = tts ?: return TextToSpeech.LANG_NOT_SUPPORTED
        val check = t.isLanguageAvailable(language.locale)
        if (check >= TextToSpeech.LANG_AVAILABLE) {
            t.language = language.locale
        } else {
            // Fallback to default or English if language packet missing
            t.language = Locale.US
        }
        return check
    }

    fun checkLanguageSupport(language: SupportedLanguage): Int {
        return tts?.isLanguageAvailable(language.locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!_isTtsReady.value || tts == null) {
            return
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "jarvis_msg_${System.currentTimeMillis()}")
        }
        tts?.speak(text, queueMode, params, "jarvis_msg")
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    // --- Speech Recognition ---

    fun startListening(): Boolean {
        if (!permissionManager.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            _voiceError.value = "Microphone permission required for voice recognition."
            return false
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceError.value = "Speech recognition service unavailable on this device."
            return false
        }

        stopSpeaking() // interrupt any ongoing speech before listening

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _voiceError.value = null
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _soundRmsDb.value = rmsdB
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out"
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        else -> "Recognition error ($error)"
                    }
                    _voiceError.value = errorMsg
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topText = matches?.firstOrNull()?.trim()
                    if (!topText.isNullOrEmpty()) {
                        _lastRecognizedText.value = topText
                        AuditLogger.log(
                            category = AuditCategory.VOICE,
                            severity = AuditSeverity.INFO,
                            action = "Speech Recognized",
                            details = "Text: '$topText' in ${_currentLanguage.value.displayName}",
                            outcome = "RECOGNIZED"
                        )
                        onSpeechRecognized?.invoke(topText)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, _currentLanguage.value.code)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            return true
        } catch (e: Exception) {
            _voiceError.value = e.message ?: "Failed to start speech recognizer"
            _isListening.value = false
            return false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
