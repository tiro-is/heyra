package `is`.tiro.heyra

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.preference.PreferenceManager
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import `is`.tiro.speech.v1alpha.RecognitionConfig
import `is`.tiro.speech.v1alpha.SpeechGrpcKt
import `is`.tiro.speech.v1alpha.StreamingRecognitionConfig
import `is`.tiro.speech.v1alpha.StreamingRecognizeRequest
import `is`.tiro.speech.v1alpha.StreamingRecognizeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RecognitionService : android.speech.RecognitionService() {
    private val _tag = "Heyra_" + this::class.java.simpleName
    private var listener: Callback? = null
    private var finalResultsReceived = false
    private var ready = false
    private var cancelled = false
    private lateinit var channel: ManagedChannel
    private lateinit var stub: SpeechGrpcKt.SpeechCoroutineStub
    private lateinit var onSharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener
    private var recorder: AudioRecord? = null

    override fun onCreate() {
        super.onCreate()
        val serverAddress = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString("server_address", "grpcs://speech.tiro.is:443") as String
        channel = createChannel(serverAddress)
        stub = SpeechGrpcKt.SpeechCoroutineStub(channel)

        onSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                if (key == "server_address") {
                    channel.shutdownNow().awaitTermination(50, TimeUnit.MILLISECONDS)
                    createChannel(
                        preferences.getString(
                            "server_address",
                            "grpcs://speech.tiro.is:443"
                        ) as String
                    )
                }
            }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(
                onSharedPreferenceChangeListener
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(
                onSharedPreferenceChangeListener
            )
        channel.shutdown()
    }

    override fun onStartListening(intent: Intent, recognitionListener: Callback) {
        Log.d(_tag, "Started listening")
        finalResultsReceived = false
        cancelled = false

        when (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            PERMISSION_GRANTED -> {
                recorder = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .build()
                    )
                    .build()

                listener = recognitionListener

                val shouldReturnPartialResults = intent.getBooleanExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
                val maxAlternatives = intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                CoroutineScope(Dispatchers.IO).launch {
                    stub.streamingRecognize(generateRequests(maxAlternatives)).collect { response ->
                        Log.d(_tag, "Got response: $response")
                        response.run {
                            if (speechEventType == StreamingRecognizeResponse.SpeechEventType.END_OF_SINGLE_UTTERANCE && !finalResultsReceived) {
                                recognitionListener.endOfSpeech()
                                ready = false
                                when {
                                    cancelled -> {
                                        recognitionListener.error(SpeechRecognizer.ERROR_NO_MATCH)
                                    }
                                    else -> {
                                        Log.d(_tag, "No speech, timeout.")
                                        recognitionListener.error(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                                    }
                                }
                            }

                            if (resultsCount > 0) {
                                // Assume we receive single result, or none
                                val result = resultsList[0]
                                val resultBundle = Bundle().apply {
                                    putStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION,
                                        ArrayList(
                                            result.alternativesList.map { alt ->
                                                alt.transcript
                                            }
                                        )
                                    )
                                }
                                if (result.isFinal) {
                                    recognitionListener.results(resultBundle)
                                    recognitionListener.endOfSpeech()
                                    finalResultsReceived = true
                                    ready = false
                                } else if (shouldReturnPartialResults) {
                                    recognitionListener.partialResults(resultBundle)
                                }
                            }
                        }
                    }
                    if (ready) {
                        recognitionListener.endOfSpeech()
                        recognitionListener.error(SpeechRecognizer.ERROR_NO_MATCH)
                    }
                }
            }
            else -> {
                recognitionListener.error(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            }
        }
    }

    override fun onCancel(listener: Callback) {
        Log.d(_tag, "Cancelled!")
        ready = false
        stopListening()
        channel.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
    }

    override fun onStopListening(listener: Callback) {
        Log.d(_tag, "Stopping listening")
        cancelled = true
        stopListening()
    }

    private fun stopListening() {
        recorder?.run {
            if (state != AudioRecord.STATE_UNINITIALIZED) {
                stop()
                release()
            }
        }
    }

    private fun generateRequests(
        maxAlternatives: Int = 1,
        interimResults: Boolean = true,
        enableAutomaticPunctuation: Boolean = true,
    ): Flow<StreamingRecognizeRequest> = flow {
        recorder?.let { recorder ->
            recorder.startRecording()
            // first request contains the config
            emit(
                StreamingRecognizeRequest.newBuilder()
                    .setStreamingConfig(
                        StreamingRecognitionConfig.newBuilder()
                            .setInterimResults(interimResults)
                            .setSingleUtterance(true) // TODO(rkjaran): Make configurable
                            .setConfig(
                                RecognitionConfig.newBuilder()
                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                    .setLanguageCode("is-IS")
                                    .setEnableAutomaticPunctuation(enableAutomaticPunctuation)
                                    .setSampleRateHertz(recorder.sampleRate)
                                    .setEnableWordTimeOffsets(false)
                                    .setMaxAlternatives(maxAlternatives)
                            )
                    ).build()
            )
            Log.d(_tag, "Emitted config request")

            listener?.run {
                readyForSpeech(Bundle())
                ready = true
                // TODO(rkjaran): actually detect this...
                beginningOfSpeech()
            }

            val content = ByteArray(
                2 * AudioRecord.getMinBufferSize(
                    recorder.sampleRate, recorder.channelConfiguration, recorder.audioFormat
                )
            )

            while (true) {
                val size = recorder.read(content, 0, content.size, AudioRecord.READ_BLOCKING)
                if (size < 0) {
                    break
                }
                emit(
                    StreamingRecognizeRequest.newBuilder()
                        .setAudioContent(ByteString.copyFrom(content))
                        .build()
                )
            }
        }
    }
}
