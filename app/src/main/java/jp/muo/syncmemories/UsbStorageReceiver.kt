package jp.muo.syncmemories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class UsbStorageReceiver : BroadcastReceiver() {
    val TARGET_USB_INTENT = "android.hardware.usb.action.USB_DEVICE_ATTACHED"

    override fun onReceive(p0: Context, p1: Intent) {
        if (p1.action != TARGET_USB_INTENT) {
            return
        }
        Toast.makeText(p0, "USB Storage Connected: ", Toast.LENGTH_LONG).show()
    }
}