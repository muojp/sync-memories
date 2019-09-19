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



class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "SyncMemories"
    }


    fun chooseTargetDirectory() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 44)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_holder, SettingsFragment())
            .commit()
        setContentView(R.layout.activity_main)
        PicturesSyncService.invoke(this)
    }
}
