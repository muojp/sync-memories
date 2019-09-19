package jp.muo.syncmemories

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File

class PicturesSyncService : JobIntentService() {
    companion object {
        val USB_DETACH_INTENT = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        val INJECT_DUMMY = true
        val TAG = "SyncService"
        val JOB_ID = 1330
        val NOTIFICATION_CHANNEL_ID = "progress"
        val NOTIFICATION_CHANNEL_NAME = "Data Sync progress"
        val NOTIFICATION_ID = 1

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

    val notificationManager by lazy { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val notificationBuilder by lazy {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
            setAutoCancel(true)
            setDefaults(Notification.DEFAULT_ALL)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("Title")
            setContentInfo("Info")
        }
    }

    var notification: Notification? = null
    private fun prepareNotification() {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)
        notification = notificationBuilder.apply { setContentText("Text0") }.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(op: (NotificationCompat.Builder) -> Unit) {
        // val notification = notificationBuilder.apply(op).build()
        notification?.let { notificationManager.notify(NOTIFICATION_ID, it) }
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

    var rootDirs: MutableList<File>? = null

    private fun acquireRootDirs() {
        val storageFiles = ContextCompat.getExternalFilesDirs(applicationContext, null)
        storageFiles.forEach { Log.d(TAG, "RawPath: ${it.absolutePath}") }
        rootDirs = storageFiles.filter { o ->
            o.isDirectory() && o.totalSpace != 0L &&
                    o.listFiles()!!.filter { p -> p.isDirectory() && p.name == "DCIM" }.count() == 1
        }.toMutableList()
        if (rootDirs!!.count() == 0 && INJECT_DUMMY) {
            val dummyRoot = File("/storage/emulated/0/dummy")
            rootDirs?.add(dummyRoot)
        }
    }

    private fun isMediaConnected(): Boolean {
        rootDirs?.forEach { Log.d(TAG, "Path: ${it.absolutePath}") }
        return rootDirs?.count() != 0
    }

    var receiver: BroadcastReceiver? = null

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
        showToast("Starting PictureSyncService.")
        Thread.sleep(2000)
        acquireRootDirs()
        if (!isMediaConnected()) {
            return
        }
        registerReceiverForDetaching()
        prepareNotification()
        Thread.sleep(2000)
        updateNotification({ o ->
            run {
                o.setContentText(rootDirs!!.firstOrNull()!!.absolutePath)
                o.setProgress(100, 3, false)
            }
        })
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
