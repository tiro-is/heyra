package `is`.tiro.heyra

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PreferencesFragment : PreferenceFragmentCompat() {
    private val _tag = "Heyra_" + this::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        preferenceScreen.findPreference<EditTextPreference>("server_address")?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val serverUri = Uri.parse(newValue as String)
                val isValid =
                    (serverUri.scheme == "grpc" || serverUri.scheme == "grpcs") && !serverUri.host.isNullOrEmpty()
                if (!isValid) {
                    activity?.run {
                        AlertDialog.Builder(this).run {
                            setMessage(R.string.pref_server_address_not_valid)
                            setNeutralButton("Ok") { _, _ -> }
                            create()
                        }
                    }?.show()
                }
                isValid
            }

        preferenceScreen.findPreference<Preference>("reset")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                preferenceManager.sharedPreferences.edit {
                    clear()
                    apply()
                    apply()
                }
                true
            }
    }
}
