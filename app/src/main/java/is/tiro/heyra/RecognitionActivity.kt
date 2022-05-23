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
import java.util.*

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
    private lateinit var listenBtn: View
    private var listening: Boolean
        get() = listenBtn.isActivated
        set(value) {
            listenBtn.isActivated = value
        }
    private lateinit var resultsTextView: TextView

    override fun onReadyForSpeech(params: Bundle) {
        Log.d(_tag, "ready for speech")
        listening = true
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
            SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.error_speech_no_match)
            else -> getString(R.string.error_speech_unknown)
        }
    }

    override fun onResults(results: Bundle) {
        Log.d(_tag, "results")
        val resultsArray = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf<String>()
        val topResult = if (resultsArray.size > 0) resultsArray.get(0) else ""
        resultsTextView.text = topResult
        finishListening()

        if (intent.action == RecognizerIntent.ACTION_RECOGNIZE_SPEECH) {
            // NOTE: Chrome uses EXTRA_WEB_SEARCH_ONLY incorrectly (according to the documentation)
            Log.d(_tag, "recognize speech intent: $intent")
            val retIntent = Intent().apply {
                putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, resultsArray)
                putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, arrayListOf(Collections.nCopies(resultsArray.size, -1f)))
                putExtra(SearchManager.QUERY, topResult)

            }
            setResult(Activity.RESULT_OK, retIntent)
            finish()
        } else {
            Log.d(_tag, "web search intent: $intent")
            startWebSearch(topResult)
            finish()
        }
    }

    private fun startWebSearch(topResult: String) {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
        searchIntent.putExtra(SearchManager.QUERY, topResult)
        startActivity(searchIntent)
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
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtras(intent.extras ?: Bundle())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
        }
        resultsTextView.text = intent.getStringExtra(RecognizerIntent.EXTRA_PROMPT) ?: ""
    }

    private fun stopListening() {
        recognizer?.stopListening()
        listening = false
    }

    private fun finishListening() {
        listening = false
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.destroy()
    }
}
