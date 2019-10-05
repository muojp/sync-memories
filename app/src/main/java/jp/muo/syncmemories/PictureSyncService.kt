package jp.muo.syncmemories

import android.app.KeyguardManager
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


class PicturesSyncService : JobIntentService() {
    companion object {
        private const val USB_DETACH_INTENT = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        private const val TAG = "SyncService"
        private const val JOB_ID = 1330
        private const val NOTIFICATION_CHANNEL_ID = "progress"
        private const val NOTIFICATION_CHANNEL_NAME = "Data Sync progress"
        private const val NOTIFICATION_ID = 1
        private const val PREF_KEY_SOURCE = "srcRoot"
        private val DESTINATIONS = mapOf("JPG" to "destJpegRoot", "ARW" to "destRawRoot")
        private val MIME_MAP = mapOf("JPG" to "image/jpeg", "ARW" to "image/arw")
        private const val STORAGE_AVAILABILITY_CHECK_EVERY = 100L
        private const val STORAGE_AVAILABILITY_CHECK_ITER = 15

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
        }
    }
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
    }

    private var notification: Notification? = null
    private fun prepareNotification(msg: String) {
        if (notification == null) {
            createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME)
            notification =
                notificationBuilder.apply { setContentTitle(msg) }.setOnlyAlertOnce(true).build()
        }
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
        notification = null
    }

    private fun DocumentFile.subdirectory(name: String): DocumentFile? {
        if (isDirectory) {
            findFile(name)?.let {
                if (it.exists()) {
                    return it
                }
            }
        }
        return null
    }

    data class FileInfoCache(val name: String, val size: Long, val doc: DocumentFile)
    private class DestinationInfo(
        val destFileDir: DocumentFile
    ) {
        val fileListCache: Map<String, FileInfoCache> by lazy {
            destFileDir.listFiles()
                .map { o -> o.name!! to FileInfoCache(o.name!!, o.length(), o) }.toMap()
        }
    }

    private fun syncDcimDirectory(dcimRoot: DocumentFile) {
        val destMap = mutableMapOf<String, DestinationInfo>()
        DESTINATIONS.forEach { (extension, destPrefKey) ->
            val destRoot = prefs.getString(destPrefKey, "")!!
            if (destRoot == "") {
                showToast("$extension destination not set. Skipped.")
                return
            }
            val destFileDir = DocumentFile.fromTreeUri(applicationContext, Uri.parse(destRoot))
            if (destFileDir == null || !destFileDir.exists() || !destFileDir.isDirectory) {
                throw Exception("destination directory not available")
            }
            destMap[extension] = DestinationInfo(destFileDir)
        }
        // find DSC-series subdirectories
        val dirs =
            dcimRoot.listFiles().filter { o -> o.isDirectory() && o.name!!.endsWith("MSDCF") }
        dirs.forEach dirloop@{
            updateNotification {
                it.setContentTitle(getString(R.string.building_source_list))
                it.setProgress(0, 0, true)
            }
            val files =
                it.listFiles().map { o -> o.name!! to o }.toMap().toSortedMap()
            DESTINATIONS.forEach extloop@{ (extension, _) ->
                val targetedFiles = files.keys.filter {
                    it.endsWith(extension, true)
                }
                val dirname = it.name
                val nbFiles = targetedFiles.count()
                val destInfo = destMap[extension] ?: return@extloop
                updateNotification {
                    it.setProgress(0, 0, true)
                    it.setContentTitle(getString(R.string.scanning_dest_directory))
                }
                targetedFiles.forEachIndexed fileloop@{ i, key ->
                    val srcFile = files[key]
                    if (srcFile == null || !srcFile.isFile) {
                        return@fileloop
                    }
                    val destFile = destInfo.fileListCache[srcFile.name!!]
                    // val destFile = destFileDir.findFile(srcFile.name!!)
                    updateNotification {
                        val cnt = i + 1
                        it.setProgress(nbFiles, cnt, false)
                        it.setContentText(srcFile.name)
                        it.setContentTitle(
                            getString(
                                R.string.copying_files,
                                dirname,
                                cnt,
                                nbFiles
                            )
                        )
                    }
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
                    copyFile(srcFile, destInfo.destFileDir, extension)
                }
            }
        }
    }

    private fun copyFile(
        srcFile: DocumentFile, destFileDir: DocumentFile, extension: String
    ): Boolean {
        // Perform file copy
        val mimeType = MIME_MAP.getOrDefault(extension, "")
        if (mimeType.isEmpty()) {
            // Unsupported file type
            return false
        }

        destFileDir.createFile(mimeType, srcFile.name!!)?.let { destFile ->
            Log.d(TAG, "srcFilePath: ${srcFile.uri}")
            Log.d(TAG, "newFilePath: ${destFile.uri}")
            val inStream = contentResolver.openInputStream(srcFile.uri)
            val outStream = contentResolver.openOutputStream(destFile.uri)
            if (inStream == null || outStream == null) {
                showToast(getString(R.string.error_opening_streams))
                return false
            }
            inStream.copyTo(outStream)
            outStream.close()
            inStream.close()
            return true
        }
        return false
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

    private fun findDcimDirectoryOnAttachedStorage(multipleAttempt: Boolean = true): DocumentFile? {
        val srcRoot = prefs.getString(PREF_KEY_SOURCE, "")
        if (srcRoot == "") {
            return null
        }
        val srcFileRef = DocumentFile.fromTreeUri(applicationContext, Uri.parse(srcRoot))
        if (srcFileRef == null) {
            showToast(getString(R.string.error_source_media_not_attached))
            return null
        }
        if (multipleAttempt) {
            for (i in 0..STORAGE_AVAILABILITY_CHECK_ITER) {
                if (!srcFileRef.exists()) {
                    Thread.sleep(STORAGE_AVAILABILITY_CHECK_EVERY)
                }
            }
        }
        if (!srcFileRef.exists()) {
            // still not mounted.
            showToast(getString(R.string.error_source_media_not_attached))
            return null
        }
        return srcFileRef.subdirectory(getString(R.string.dcim_dirname))
    }

    private fun cancelSync() {
        throw Exception("Cancel not implemented")
    }

    override fun onHandleWork(intent: Intent) {
        registerReceiverForDetaching()
        waitForMediaDiscovery()?.let { dcimRoot ->
            prepareNotification(getString(R.string.searching_for_images))
            syncDcimDirectory(dcimRoot)
        }
        cleanup()
    }

    private fun waitForMediaDiscovery(): DocumentFile? {
        var dir = findDcimDirectoryOnAttachedStorage(multipleAttempt = false)
        val kgm =
            applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (kgm.isDeviceLocked) {
            prepareNotification(getString(R.string.alert_unlock_device))
            while (kgm.isDeviceLocked) {
                Thread.sleep(200)
                dir = findDcimDirectoryOnAttachedStorage(multipleAttempt = false)
                if (dir != null) {
                    break
                }
            }
        }
        return dir ?: findDcimDirectoryOnAttachedStorage()
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(this)
        }
    }
}
