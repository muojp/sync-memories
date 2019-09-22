package jp.muo.syncmemories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class UsbStorageReceiver : BroadcastReceiver() {
    companion object {
        private const val TARGET_USB_INTENT = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TARGET_USB_INTENT) {
            return
        }
        Toast.makeText(context, "USB Storage Connected: ", Toast.LENGTH_SHORT).show()
        PicturesSyncService.invoke(context)
    }
}