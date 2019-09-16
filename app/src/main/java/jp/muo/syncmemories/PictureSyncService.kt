package jp.muo.syncmemories

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat


/*
class PicturesSyncService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(applicationContext, "onStartCommand.", Toast.LENGTH_LONG).show()
        return super.onStartCommand(intent, flags, startId)
    }
}
*/

class PicturesSyncService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        Toast.makeText(applicationContext, "Starting PictureSyncService.", Toast.LENGTH_LONG).show()
        /*
        val notificationBuilder = NotificationCompat.Builder(this, "progress").apply {
            setAutoCancel(true)
            setDefaults(Notification.DEFAULT_ALL)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle("Title")
            setContentText("Text")
            setContentInfo("Info")
        }
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder.build()
        notificationManager.notify(1, notification)
        startForeground(1, notification)
        Thread.sleep(5000)
        notificationManager.cancel(1)
         */
    }
}
