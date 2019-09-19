package jp.muo.syncmemories

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
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
        val REQ_SRC = 10001
        val REQ_DEST = 10002
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun acquireSrcDirectory() {
        Toast.makeText(context, "hey", Toast.LENGTH_SHORT).show()
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_SRC);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun acquireDestDirectory() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_DEST);
    }

    fun checkGrantedDiskAccessRight() {
        val str = "content://com.android.externalstorage.documents/tree/1EEE-3510%3A"
        val fileRef = DocumentFile.fromTreeUri(context!!, Uri.parse(str))?.let {
            Log.d(MainActivity.TAG, "SD access enabled")
            if (it.isDirectory) {
                val d = it.findFile("DCIM")?.let {
                    if (it.exists()) {
                        Log.d(MainActivity.TAG, "found DCIM")
                    }
                }
            }
        }
    }

    val editTextMap = mutableMapOf<Preference, EditText>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        editTextMap.clear()
        val srcRoot: EditTextPreference = findPreference("srcRoot")!!
        val destRoot: EditTextPreference = findPreference("destRoot")!!
        srcRoot.apply {
            setOnPreferenceClickListener({
                acquireSrcDirectoryWithPermissionCheck()
                true
            })
            setOnBindEditTextListener {
                editTextMap[srcRoot] = it
            }
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        }

        destRoot.apply {
            setOnPreferenceClickListener({
                acquireDestDirectoryWithPermissionCheck()
                true
            })
            setOnBindEditTextListener {
                editTextMap[destRoot] = it
            }
            summaryProvider =
                EditTextPreference.SimpleSummaryProvider.getInstance()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        var pref: EditTextPreference? = null
        when (requestCode) {
            REQ_SRC -> pref = findPreference("srcRoot")!!
            REQ_DEST -> pref = findPreference("destRoot")!!
        }
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
        pref?.apply {
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