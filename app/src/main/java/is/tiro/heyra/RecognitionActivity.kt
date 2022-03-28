package `is`.tiro.heyra

import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RecognitionActivity : AppCompatActivity(), RecognitionListener {
    private val _tag = "Heyra_" + this::class.java.simpleName

    private var requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // TODO(rkjaran): Do something more intelligent here
        if (isGranted) {
            Log.d(_tag, "RECORD_AUDIO granted")
        } else {
            AlertDialog.Builder(this)
                .run {
                    setTitle(R.string.error_insufficient_permission_title)
                    setMessage(R.string.error_insufficient_permission_message)
                    setNeutralButton("Ok", DialogInterface.OnClickListener { _, _ -> })
                    create()
                }
                .show()

        }
    }

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private lateinit var listenBtn: View
    private lateinit var resultsTextView: TextView


    override fun onReadyForSpeech(params: Bundle) {
        Log.d(_tag, "ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(_tag, "beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.d(_tag, "rms changed")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.d(_tag, "buffer received")
    }

    override fun onEndOfSpeech() {
        Log.d(_tag, "end of speech")
    }

    override fun onError(error: Int) {
        Log.d(_tag, "error: $error")
        finishListening()
        resultsTextView.text = when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.error_speech_timeout)
            else -> getString(R.string.error_speech_unknown)
        }
    }

    override fun onResults(results: Bundle) {
        Log.d(_tag, "results")
        val resultsArray = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val topResult = resultsArray?.get(0) ?: ""
        resultsTextView.text = topResult
        finishListening()

        // Return the results to the calling activity
        when (intent.action) {
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH -> {
                // TODO(rkjaran): This doesn't work when Chrome is the caller, but works for Android
                Log.d(_tag, "recognize speech intent: $intent")
                intent
                    .putExtra(RecognizerIntent.EXTRA_RESULTS, resultsArray)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
            // RecognizerIntent.ACTION_WEB_SEARCH ->
            else -> {
                Log.d(_tag, "web search intent: $intent")
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                searchIntent.putExtra(SearchManager.QUERY, topResult)
                startActivity(searchIntent)
                finish()
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle) {
        val resultsArray = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        resultsTextView.text = resultsArray?.get(0) ?: ""
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.d(_tag, "event")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(_tag, "Creating with intent ${intent.action}")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listenBtn = findViewById<Button>(R.id.button_start_listening)
        listenBtn.setOnClickListener {
            if (listening) {
                stopListening()
            } else {
                startListening()
            }
        }

        resultsTextView = findViewById<TextView>(R.id.text_recognition_results)

        createSpeechRecognizer()
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    private fun createSpeechRecognizer() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> {
                recognizer = SpeechRecognizer.createSpeechRecognizer(
                    this,
                    ComponentName.createRelative(this, ".RecognitionService")
                )

                if (recognizer == null) {
                    Log.e(_tag, "SpeechRecognizer not created!")
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        recognizer?.let {
            it.setRecognitionListener(this)
            it.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to me!")
            )

            listenBtn.isActivated = true
            listening = true
        }
    }

    private fun stopListening() {
        recognizer?.stopListening()
    }

    private fun finishListening() {
        listenBtn.isActivated = false
        listening = false
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.destroy()
    }
}
