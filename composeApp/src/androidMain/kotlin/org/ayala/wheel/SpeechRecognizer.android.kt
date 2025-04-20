package org.ayala.wheel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

actual fun createSpeechRecognizer(): org.ayala.wheel.SpeechRecognizer = AndroidSpeechRecognizer()

class AndroidSpeechRecognizer : org.ayala.wheel.SpeechRecognizer {
    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var activity: ComponentActivity? = null
    
    companion object {
        private const val TAG = "SpeechRecognizer"
        private const val PERMISSION_REQUEST_CODE = 1234
    }

    @Composable
    override fun Initialize() {
        val context = LocalContext.current
        activity = context as? ComponentActivity
        Log.d(TAG, "Initialized with activity: ${activity != null}")
    }

    private fun hasPermission(): Boolean {
        return activity?.let { 
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    private fun requestPermission() {
        activity?.let { activity ->
            Log.d(TAG, "Requesting RECORD_AUDIO permission")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun isAvailable(): Boolean {
        val available = activity?.let { android.speech.SpeechRecognizer.isRecognitionAvailable(it) } ?: false
        Log.d(TAG, "Speech recognition available: $available")
        return available
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentActivity = activity ?: run {
            val error = "Activity not available"
            Log.e(TAG, error)
            onError(error)
            return
        }

        if (!hasPermission()) {
            Log.d(TAG, "No RECORD_AUDIO permission, requesting...")
            requestPermission()
            onError("Please grant microphone permission and try again")
            return
        }

        Log.d(TAG, "Starting speech recognition...")
        onResultCallback = onResult
        onErrorCallback = onError

        speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(currentActivity).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "Got final results: $matches")
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    } else {
                        Log.w(TAG, "Received empty results")
                    }
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "Got partial results: $matches")
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        android.speech.SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
                        android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        android.speech.SpeechRecognizer.ERROR_SERVER -> "Server error"
                        android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error: $error"
                    }
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    onError(errorMessage)
                }

                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Log audio levels less frequently to avoid spam
                    if (rmsdB > 1) {
                        Log.v(TAG, "Sound level changed: $rmsdB")
                    }
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    Log.d(TAG, "Buffer received: ${buffer?.size ?: 0} bytes")
                }
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }
                
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {
                    Log.d(TAG, "Speech event: type=$eventType, params=$params")
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL") // Hebrew ISO code
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            Log.d(TAG, "Starting speech recognizer with intent")
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            val error = "Failed to start speech recognition: ${e.message}"
            Log.e(TAG, error, e)
            onError(error)
        }
    }

    override fun stopListening() {
        Log.d(TAG, "Stopping speech recognition")
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        } finally {
            speechRecognizer = null
        }
    }
}