package jp.muo.syncmemories

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat

class PicturesSyncService : JobIntentService() {
    companion object {
        fun invoke(context: Context) {
            val intent = Intent(context, PicturesSyncService::class.java)
            enqueueWork(context, PicturesSyncService::class.java, 1330, intent)
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    val notificationManager by lazy { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private fun prepareNotification() {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, "progress").apply {
            setAutoCancel(true)
            setDefaults(Notification.DEFAULT_ALL)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("Title")
            setContentText("Text")
            setContentInfo("Info")
        }
        createNotificationChannel("progress", "Data Sync progress")
        val notification = notificationBuilder.build()
        notificationManager.notify(1, notification)
        startForeground(1, notification)
    }

    private fun cleanupNotification() {
        notificationManager.cancel(1)
    }

    override fun onHandleWork(intent: Intent) {
        showToast("Starting PictureSyncService.")
        prepareNotification()
        Thread.sleep(5000)
        cleanupNotification()
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(this)
        }
    }
}
