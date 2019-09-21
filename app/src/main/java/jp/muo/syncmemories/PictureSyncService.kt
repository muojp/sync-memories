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
        private val DESTINATIONS = mapOf("JPG" to "destJpegRoot", "ARW" to "destRawRoot")
        private val MIME_MAP = mapOf("JPG" to "image/jpeg", "ARW" to "image/arw")
        private const val CHECK_EVERY = 100L
        private const val CHECK_ITER = 15

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
    private fun prepareNotification(msg: String) {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)
        notification = notificationBuilder.apply { setContentTitle(msg) }.build()
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

    private fun testshot() {
        val srcRoot = prefs.getString("srcRoot", "")!!
        if (srcRoot == "") {
            return
        }
        val srcFileRef = DocumentFile.fromTreeUri(applicationContext, Uri.parse(srcRoot))
        if (srcFileRef == null) {
            showToast("source media not attached.")
            return
        }
        for (i in 0..CHECK_ITER) {
            if (!srcFileRef.exists()) {
                Thread.sleep(CHECK_EVERY)
            }
        }
        if (!srcFileRef.exists()) {
            // still not mounted.
            showToast("source media not attached.")
            return
        }
        DESTINATIONS.forEach { (extension, prefKey) ->
            val destRoot = prefs.getString(prefKey, "")!!
            if (destRoot == "") {
                showToast("$extension destination not set. Skipped.")
                return
            }
            val destFileDir = DocumentFile.fromTreeUri(applicationContext, Uri.parse(destRoot))
            if (destFileDir == null || !destFileDir.exists() || !destFileDir.isDirectory()) {
                throw Exception("destination directory not available")
            }
            data class FileInfoCache(val name: String, val size: Long, val doc: DocumentFile)

            val destListCache =
                destFileDir.listFiles()
                    .map { o -> o.name!! to FileInfoCache(o.name!!, o.length(), o) }.toMap()
            srcFileRef.subdirectory("DCIM")?.let {
                // find DSC-series subdirectories
                val dirs =
                    it.listFiles().filter { o -> o.isDirectory() && o.name!!.endsWith("MSDCF") }
                dirs.forEach dirloop@{
                    updateNotification {
                        it.setContentTitle("Building source file list")
                        it.setProgress(10, 0, true)
                    }
                    val files =
                        it.listFiles().map { o -> o.name!! to o }.toMap().toSortedMap()
                    val targettedFiles = files.keys.filter {
                        it.endsWith(extension, true)
                    }
                    val dirname = it.name
                    val nbFiles = targettedFiles.count()
                    if (nbFiles != 0) {
                        updateNotification { it.setContentTitle("Copying: $dirname (${targettedFiles.count()}) files") }
                    }
                    targettedFiles.forEachIndexed fileloop@{ i, key ->
                        val srcFile = files[key]
                        if (srcFile == null || !srcFile.isFile) {
                            return@fileloop
                        }
                        updateNotification {
                            val cnt = i + 1
                            it.setProgress(nbFiles, cnt, false)
                            it.setContentText(srcFile.name)
                            it.setContentTitle("Copying: $dirname ($cnt/${nbFiles}) files")
                        }
                        val destFile = destListCache[srcFile.name!!]
                        // val destFile = destFileDir.findFile(srcFile.name!!)
                        if (destFile != null && destFile.doc.exists()) {
                            // file already exists
                            if (destFile.doc.length() < srcFile.length()) {
                                // Incomplete file found. Delete current one and copy again.
                                destFile.doc.delete()
                            } else {
                                // Complete file found. Do nothing.
                                return@fileloop
                            }
                        }
                        // Perform file copy
                        val newFile =
                            destFileDir.createFile(MIME_MAP[extension]!!, srcFile.name!!)!!
                        Log.d(TAG, "srcFilePath: ${srcFile.uri}")
                        Log.d(TAG, "newFilePath: ${newFile.uri}")
                        val inStream = contentResolver.openInputStream(srcFile.uri)
                        val outStream = contentResolver.openOutputStream(newFile.uri)
                        if (inStream == null || outStream == null) {
                            showToast("Something went wrong. Failed to open input/output stream")
                            return
                        }
                        inStream.copyTo(outStream)
                        outStream.close()
                        inStream.close()
                    }
                }
            }
            showToast(prefs.getString("destRoot", "")!!)
        }
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
        registerReceiverForDetaching()
        prepareNotification("Searching for image files")
        testshot()
        if (!isMediaConnected()) {
            cleanup()
            return
        }
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
