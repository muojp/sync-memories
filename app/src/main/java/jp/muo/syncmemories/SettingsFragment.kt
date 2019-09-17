package jp.muo.syncmemories

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        val TAG = "SettingsFragment"
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun acquirePermissions() {
        Toast.makeText(context, "hey", Toast.LENGTH_SHORT).show()
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
        // startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 42);
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val srcRoot : Preference = findPreference("srcRoot")!!
        srcRoot.setOnPreferenceClickListener({
            Log.d(TAG, "HHHHH?")
            acquirePermissionsWithPermissionCheck()
            true
        })
        /*
                btn_check_permission.setOnClickListener {
            acquirePermissionsWithPermissionCheck()
        }
        btn_choose_target.setOnClickListener {
            chooseTargetDirectoryWithPermissionCheck()
        }

         */
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}