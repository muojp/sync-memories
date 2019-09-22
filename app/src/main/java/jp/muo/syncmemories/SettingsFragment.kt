package jp.muo.syncmemories

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        val TAG = "SettingsFragment"
        val ASSOC =
            mapOf<String, Int>("srcRoot" to 10001, "destJpegRoot" to 10002, "destRawRoot" to 10003)
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun acquireDirectory(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), code);
    }

    val editTextMap = mutableMapOf<Preference, EditText>()

    private fun setupEditTextPreference(key: String) {
        findPreference<EditTextPreference>(key)?.apply {
            setOnPreferenceClickListener {
                acquireDirectoryWithPermissionCheck(ASSOC[key]!!)
                true
            }
            setOnBindEditTextListener {
                editTextMap[this] = it
            }
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        editTextMap.clear()
        for (key in ASSOC.keys) {
            setupEditTextPreference(key)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        val q = ASSOC.filterValues { it == requestCode }
        if (q.isEmpty())
            return
        val kv = q.toList()[0]
        val pref: EditTextPreference = findPreference(kv.first)!!
        val treeUri = resultData!!.data as Uri
        Log.d(TAG, treeUri.toString())
        val pickedDir = DocumentFile.fromTreeUri(context!!, treeUri)
        context!!.grantUriPermission(
            context!!.packageName,
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context!!.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        pref.apply {
            editTextMap[this]?.setText(treeUri.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}