package `is`.tiro.heyra

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log

class GetLanguageDetailsReceiver : BroadcastReceiver() {
    private val _tag = this::class.java.simpleName
    override fun onReceive(
        context: Context?,
        intent: Intent?
    ) {
        Log.d(_tag, "Emitting language details")
        // Hardcode default language for now
        val supportedLanguages = arrayListOf<String>("is-IS")
        val defaultLanguage = supportedLanguages[0]
        val bundle = Bundle()
        bundle.putString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, defaultLanguage)
        bundle.putStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, supportedLanguages)
        setResultExtras(bundle)
    }
}