package `is`.tiro.heyra

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.PermissionChecker

class InputMethodService :
    android.inputmethodservice.InputMethodService(),
    android.speech.RecognitionListener {
    private companion object {
        const val TAG = "InputMethodService"
    }

    private var recognizer: SpeechRecognizer? = null
    private lateinit var listenBtn: View
    private var listening: Boolean
        get() = listenBtn.isActivated
        set(value) {
            listenBtn.isActivated = value
        }
    private lateinit var resultsTextView: TextView
    private lateinit var deleteWordBtn: ImageButton
    private lateinit var settingsBtn: ImageButton
    private lateinit var actionBtn: ImageButton
    private var currentHypothesis = ""

    override fun onReadyForSpeech(params: Bundle) {
        Log.d(TAG, "ready for speech")
        listening = true
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "beginning of speech")
    }

    override fun onRmsChanged(p: Float) {
        Log.d(TAG, "rms changed")
    }

    override fun onBufferReceived(buf: ByteArray) {
        Log.d(TAG, "buffer received")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "end of speech")
    }

    override fun onError(error: Int) {
        Log.d(TAG, "error: $error")
        finishListening()
        resultsTextView.text = when (error) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.error_speech_timeout)
            SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.error_speech_no_match)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.error_insufficient_permission_message)
            else -> getString(R.string.error_speech_unknown)
        }
    }

    private fun handleResults(results: Bundle, isFinal: Boolean = false) {
        Log.d(TAG, "results")
        val resultsArray = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?: arrayListOf<String>()
        val topResult = if (resultsArray.size > 0) resultsArray[0] else ""

        currentInputConnection.also { ic ->
            val padding = ic.getTextBeforeCursor(currentHypothesis.length + 1, 1).let {
                if (it == null || it.length == currentHypothesis.length || it.startsWith(" "))
                    ""
                else
                    " "
            }
            currentHypothesis = padding + topResult
            ic.setComposingText(currentHypothesis, 1)
            if (isFinal) {
                ic.commitText(currentHypothesis, 1)
            }
        }
    }

    override fun onResults(results: Bundle) {
        handleResults(results, isFinal = true)
        finishListening()
    }

    override fun onPartialResults(partialResults: Bundle) {
        handleResults(partialResults)
    }

    override fun onEvent(event: Int, bundle: Bundle?) {
        Log.d(TAG, "event: $event")
    }

    private fun createSpeechRecognizer() {
        when (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            PermissionChecker.PERMISSION_DENIED, PermissionChecker.PERMISSION_DENIED_APP_OP -> {
                resultsTextView.text = getString(R.string.error_insufficient_permission_message)
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:is.tiro.heyra")
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            else -> {
                if (recognizer == null) {
                    recognizer = SpeechRecognizer.createSpeechRecognizer(
                        this,
                        ComponentName.createRelative(this, ".RecognitionService")
                    )
                }
            }
        }
    }

    private fun startListening() {
        recognizer?.let {
            it.setRecognitionListener(this)
            it.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
            resultsTextView.text = ""
        }
    }

    private fun stopListening() {
        recognizer?.stopListening()
        listening = false
    }

    private fun finishListening() {
        listening = false
        currentHypothesis = ""
    }

    private fun deleteLastWord() {
        val textBeforeCursor = currentInputConnection.getTextBeforeCursor(999, 0) ?: ""
        if (textBeforeCursor.isEmpty())
            return
        if (textBeforeCursor.last() == ' ') {
            currentInputConnection.deleteSurroundingText(1, 0)
        } else {
            val index = textBeforeCursor.reversed().indexOf(' ')
            currentInputConnection.deleteSurroundingText(
                if (index < 0) textBeforeCursor.length else index,
                0
            )
        }
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView")
        return layoutInflater.inflate(R.layout.input, null).apply {
            listenBtn = findViewById<Button>(R.id.button_start_listening)
            listenBtn.setOnClickListener {
                if (listening) {
                    stopListening()
                } else {
                    startListening()
                }
            }

            deleteWordBtn = findViewById<ImageButton>(R.id.delete_word_button)
            deleteWordBtn.setOnClickListener {
                deleteLastWord()
            }
            deleteWordBtn.setOnLongClickListener {
                currentInputConnection.deleteSurroundingText(999, 0)
            }

            settingsBtn = findViewById<ImageButton>(R.id.button_settings)
            settingsBtn.setOnClickListener {
                startActivity(
                    Intent(context, PreferencesActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }

            actionBtn = findViewById<ImageButton>(R.id.button_action)
            actionBtn.setOnClickListener {
                val imeActionId = currentInputEditorInfo.let { info ->
                    if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0)
                        EditorInfo.IME_ACTION_NONE
                    else
                        info.imeOptions and EditorInfo.IME_MASK_ACTION
                }
                if (imeActionId != EditorInfo.IME_ACTION_NONE)
                    currentInputConnection.performEditorAction(imeActionId)
                else
                    currentInputConnection.commitText("\n", 1)
            }

            resultsTextView = findViewById<TextView>(R.id.text_recognition_results)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        Log.d(TAG, "onStartInputView")
        createSpeechRecognizer()
        startListening()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (listening) {
            stopListening()
        }
        finishListening()
    }
}
