package org.ayala.wheel

import platform.Speech.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.darwin.*

actual fun createSpeechRecognizer(): SpeechRecognizer = IOSSpeechRecognizer()

class IOSSpeechRecognizer : SpeechRecognizer {
    private val speechRecognizer = SFSpeechRecognizer(NSLocale("he"))
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    override fun isAvailable(): Boolean {
        return speechRecognizer?.isAvailable() == true
    }

    override fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onResultCallback = onResult
        onErrorCallback = onError

        SFSpeechRecognizer.requestAuthorization { authStatus ->
            when (authStatus) {
                SFSpeechRecognizerAuthorizationStatusAuthorized -> {
                    startRecognition()
                }
                else -> {
                    onError("Speech recognition not authorized")
                }
            }
        }
    }

    private fun startRecognition() {
        val inputNode = audioEngine.inputNode
        val recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        recognitionRequest.shouldReportPartialResults = true

        recognitionTask = speechRecognizer?.recognitionTask(
            with = recognitionRequest,
            resultHandler = { result, error ->
                if (error != null) {
                    onErrorCallback?.invoke(error.localizedDescription)
                    stopListening()
                } else {
                    val transcription = result?.bestTranscription?.formattedString
                    if (transcription != null) {
                        onResultCallback?.invoke(transcription)
                    }
                }
            }
        )

        val recordingFormat = inputNode.outputFormatForBus(0)
        inputNode.installTapOnBus(
            0,
            bufferSize = 1024,
            format = recordingFormat
        ) { buffer, _ ->
            recognitionRequest.appendAudioPCMBuffer(buffer)
        }

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
    }

    override fun stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0)
        recognitionTask?.finish()
        recognitionTask = null
    }
}