package jp.muo.syncmemories

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


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
            .replace(R.id.fragment_settings, SettingsFragment())
            .replace(R.id.fragment_controllers, ControllersFragment())
            .commit()
        setContentView(R.layout.activity_main)
//        PicturesSyncService.invoke(this)
    }
}
