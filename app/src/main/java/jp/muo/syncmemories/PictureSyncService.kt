package jp.muo.syncmemories

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.io.File

class PicturesSyncService : JobIntentService() {
    companion object {
        private const val USB_DETACH_INTENT = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val TAG = "SyncService"
        private const val JOB_ID = 1330
        private const val NOTIFICATION_CHANNEL_ID = "progress"
        private const val NOTIFICATION_CHANNEL_NAME = "Data Sync progress"
        private const val NOTIFICATION_ID = 1

        fun invoke(context: Context) {
            val intent = Intent(context, PicturesSyncService::class.java)
            enqueueWork(context, PicturesSyncService::class.java, JOB_ID, intent)
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    private val notificationManager by lazy { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
            setAutoCancel(true)
            setDefaults(Notification.DEFAULT_ALL)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("Title")
        }
    }
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
    }

    private var notification: Notification? = null
    private fun prepareNotification() {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)
        notification = notificationBuilder.apply { setContentText("Text0") }.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(op: (NotificationCompat.Builder) -> Unit) {
        val notification = notificationBuilder.apply(op).setOnlyAlertOnce(true).build()
        notification?.let {
            notificationManager.notify(NOTIFICATION_ID, it)
        }
    }

    private fun cleanup() {
        cleanupNotification()
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }
    }

    private fun cleanupNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private var rootDirs: MutableList<File>? = null

    fun DocumentFile.subdirectory(name: String): DocumentFile? {
        if (isDirectory) {
            val d = findFile(name)?.let {
                if (it.exists()) {
                    return it
                }
            }
        }
        return null
    }

    private fun acquireRootDirs() {
        val srcRoot = prefs.getString("srcRoot", "")!!
        val fileRef = DocumentFile.fromTreeUri(applicationContext, Uri.parse(srcRoot))
        fileRef?.subdirectory("DCIM")?.let {
            // find DSC-series subdirectories
            val dirs = it.listFiles().filter { o -> o.isDirectory() && o.name!!.endsWith("MSDCF") }
            dirs.forEach {
                it.listFiles().filter { o -> o.isFile() && o.name!!.endsWith("JPG", true) }
                    .forEach { file ->
                        Log.d(TAG, file.name)
                    }
            }
        }
        showToast(prefs.getString("destRoot", "")!!)
    }

    private fun isMediaConnected(): Boolean {
        rootDirs?.forEach { Log.d(TAG, "Path: ${it.absolutePath}") }
        return rootDirs?.count() != 0
    }

    private var receiver: BroadcastReceiver? = null

    private fun registerReceiverForDetaching() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    USB_DETACH_INTENT -> {
                        unregisterReceiver(this)
                        cancelSync()
                    }
                }
            }
        }
        registerReceiver(receiver, IntentFilter(USB_DETACH_INTENT))
    }

    private fun cancelSync() {
        throw Exception("Cancel not implemented")
    }

    override fun onHandleWork(intent: Intent) {
        acquireRootDirs()
        if (!isMediaConnected()) {
            return
        }
        registerReceiverForDetaching()
        prepareNotification()
        Thread.sleep(2000)
        updateNotification { o ->
            run {
                // o.setContentText(rootDirs!!.firstOrNull()!!.absolutePath)
                o.setProgress(100, 3, false)
            }
        }
        Thread.sleep(3000)
        cleanup()
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(this)
        }
    }
}
