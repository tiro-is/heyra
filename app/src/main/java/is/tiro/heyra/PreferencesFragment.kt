package `is`.tiro.heyra

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.PermissionChecker
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class PreferencesFragment : PreferenceFragmentCompat() {
    companion object {
        const val TAG = "PreferencesFragment"
        const val KEY_SERVER_ADDRESS = "server_address"
        const val KEY_RESET = "reset"
        const val KEY_PERMISSIONS_STATUS = "permissions_status"
    }

    private fun notify(messageId: Int) {
        activity?.run {
            AlertDialog.Builder(this).run {
                setMessage(messageId)
                setNeutralButton("Ok") { _, _ -> }
                create()
            }
        }?.show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val hasPermissions = when (
            PermissionChecker.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            PermissionChecker.PERMISSION_GRANTED -> true
            else -> false
        }

        setPreferencesFromResource(R.xml.preferences, rootKey)

        preferenceScreen.findPreference<EditTextPreference>(KEY_SERVER_ADDRESS)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                val serverUri = Uri.parse(newValue as String)
                val isValid = (
                    serverUri.scheme in arrayOf(
                        "grpc",
                        "grpcs"
                    )
                    ) && !serverUri.host.isNullOrEmpty()
                if (!isValid) {
                    notify(R.string.pref_server_address_not_valid)
                }
                isValid
            }

        preferenceScreen.findPreference<Preference>(KEY_PERMISSIONS_STATUS)?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:is.tiro.heyra")
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }
            it.summary = if (hasPermissions) {
                getString(R.string.pref_permissions_status_summary)
            } else {
                getString(R.string.pref_insufficient_permissions_status_summary)
            }
        }

        preferenceScreen.findPreference<Preference>(KEY_RESET)?.onPreferenceClickListener =
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
