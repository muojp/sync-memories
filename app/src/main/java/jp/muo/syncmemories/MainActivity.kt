package jp.muo.syncmemories

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.lang.StringBuilder
import androidx.documentfile.provider.DocumentFile
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri



@RuntimePermissions
class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "SyncMemories"
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun acquirePermissions() {
        Toast.makeText(this, "hey", Toast.LENGTH_SHORT).show()
        val str = "content://com.android.externalstorage.documents/tree/1EEE-3510%3A"
        val fileRef = DocumentFile.fromTreeUri(this, Uri.parse(str))?.let {
            Log.d(TAG, "SD access enabled")
            if (it.isDirectory) {
                val d = it.findFile("DCIM")?.let {
                    if (it.exists()) {
                        Log.d(TAG, "found DCIM")
                    }
                }
            }
        }
        // startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 42);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun chooseTargetDirectory() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 44);
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_check_permission.setOnClickListener {
            acquirePermissionsWithPermissionCheck()
        }
        btn_choose_target.setOnClickListener {
            chooseTargetDirectoryWithPermissionCheck()
        }
        PicturesSyncService.invoke(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode != Activity.RESULT_OK)
            return
        val treeUri = resultData!!.data as Uri
        Log.d(TAG, treeUri.toString())
        val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        grantUriPermission(
            packageName,
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}
