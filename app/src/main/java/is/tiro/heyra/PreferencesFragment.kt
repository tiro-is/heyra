package `is`.tiro.heyra

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.PermissionChecker
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class PreferencesFragment : PreferenceFragmentCompat() {
    companion object {
        const val TAG = "PreferencesFragment"
        const val KEY_SERVER_ADDRESS = "server_address"
        const val KEY_RESET = "reset"
        const val KEY_PERMISSIONS_STATUS = "permissions_status"
        const val KEY_INPUT_METHOD_STATUS = "input_method_status"
        const val KEY_PRIVACY = "privacy"
        const val KEY_NOTICES = "notices"
        const val KEY_VERSION = "version"
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

    override fun onResume() {
        super.onResume()
        updateStatusSummaries()
    }

    private fun updateStatusSummaries() {
        val statusKeys = arrayOf(KEY_INPUT_METHOD_STATUS, KEY_PERMISSIONS_STATUS)
        val hasPermissions = when (
            PermissionChecker.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.RECORD_AUDIO
            )
        ) {
            PermissionChecker.PERMISSION_GRANTED -> true
            else -> false
        }
        val inputMethodActive = requireActivity().applicationContext.let { ctx ->
            val imm = requireActivity().applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.any { info ->
                info.packageName == ctx.packageName
            }
        }

        statusKeys.forEach { key ->
            preferenceScreen.findPreference<Preference>(key)?.also { pref ->
                when (key) {
                    KEY_INPUT_METHOD_STATUS -> {
                        pref.summary = if (inputMethodActive) {
                            getString(R.string.pref_input_method_status_summary_active)
                        } else {
                            getString(R.string.pref_input_method_status_summary_inactive)
                        }
                    }
                    KEY_PERMISSIONS_STATUS -> {
                        pref.summary = if (hasPermissions) {
                            getString(R.string.pref_permissions_status_summary)
                        } else {
                            getString(R.string.pref_insufficient_permissions_status_summary)
                        }
                    }
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
        }

        preferenceScreen.findPreference<Preference>(KEY_INPUT_METHOD_STATUS)?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_INPUT_METHOD_SETTINGS
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
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

        preferenceScreen.findPreference<Preference>(KEY_PRIVACY)?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tiro.is/personuverndarstefna/#heyra")))
                true
            }
        }

        preferenceScreen.findPreference<Preference>(KEY_NOTICES)?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.pref_notices_title))
                true
            }

        preferenceScreen.findPreference<Preference>(KEY_VERSION)?.summary =
            "Heyra: ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}\n" +
            "OS: Android ${Build.VERSION.RELEASE}\n" +
            "Device: ${Build.BRAND} ${Build.MODEL}"

        updateStatusSummaries()
    }
}
