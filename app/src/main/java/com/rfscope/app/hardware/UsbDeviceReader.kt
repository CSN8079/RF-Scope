package com.rfscope.app.hardware

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.rfscope.app.data.UsbDeviceSummary
import com.rfscope.app.data.UsbScanSummary

class UsbDeviceReader(context: Context) {
    private val appContext = context.applicationContext

    fun scan(): UsbScanSummary {
        val manager = appContext.getSystemService(UsbManager::class.java)
            ?: return UsbScanSummary(hostSupported = false)

        val devices = manager.deviceList.values
            .asSequence()
            .map { device -> device.toSummary(manager.hasPermission(device)) }
            .sortedByDescending { it.likelyTinySa }
            .toList()

        return UsbScanSummary(
            hostSupported = true,
            devices = devices,
        )
    }

    private fun UsbDevice.toSummary(permissionGranted: Boolean): UsbDeviceSummary {
        val serialInterface = (0 until interfaceCount).any { index ->
            val usbInterface = getInterface(index)
            usbInterface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                usbInterface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA
        }
        return UsbDeviceSummary(
            deviceName = deviceName,
            vendorId = vendorId,
            productId = productId,
            manufacturer = manufacturerName,
            product = productName,
            hasSerialInterface = serialInterface,
            permissionGranted = permissionGranted,
            likelyTinySa = vendorId == STMICROELECTRONICS_VENDOR_ID || serialInterface,
        )
    }

    private companion object {
        const val STMICROELECTRONICS_VENDOR_ID = 0x0483
    }
}
