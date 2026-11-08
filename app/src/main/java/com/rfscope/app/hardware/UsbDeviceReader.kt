package com.rfscope.app.hardware

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.rfscope.app.data.UsbDeviceSummary
import com.rfscope.app.data.UsbScanSummary

class UsbDeviceReader(context: Context) {
    private val appContext = context.applicationContext

    fun scan(): UsbScanSummary {
        val manager = usbManager() ?: return UsbScanSummary(hostSupported = false)
        val devices = manager.deviceList.values
            .asSequence()
            .map { device -> device.toSummary(manager.hasPermission(device)) }
            .sortedByDescending { it.likelyTinySa }
            .toList()
        return UsbScanSummary(hostSupported = true, devices = devices)
    }

    fun requestPermissionForLikelyTinySa(pendingIntent: PendingIntent): Boolean {
        val manager = usbManager() ?: return false
        val device = manager.deviceList.values.firstOrNull { it.isLikelyTinySa() }
            ?: return false
        manager.requestPermission(device, pendingIntent)
        return true
    }

    private fun usbManager(): UsbManager? =
        appContext.getSystemService(UsbManager::class.java)

    private fun UsbDevice.toSummary(permissionGranted: Boolean): UsbDeviceSummary {
        val serialInterface = hasSerialInterface()
        return UsbDeviceSummary(
            deviceName = deviceName,
            vendorId = vendorId,
            productId = productId,
            manufacturer = manufacturerName,
            product = productName,
            hasSerialInterface = serialInterface,
            permissionGranted = permissionGranted,
            likelyTinySa = isLikelyTinySa(),
        )
    }

    private fun UsbDevice.isLikelyTinySa(): Boolean =
        (vendorId == STMICROELECTRONICS_VENDOR_ID && productId == STM32_VIRTUAL_COM_PRODUCT_ID) || hasSerialInterface()

    private fun UsbDevice.hasSerialInterface(): Boolean =
        (0 until interfaceCount).any { index ->
            val usbInterface = getInterface(index)
            usbInterface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                usbInterface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA
        }

    private companion object {
        const val STMICROELECTRONICS_VENDOR_ID = 0x0483
        const val STM32_VIRTUAL_COM_PRODUCT_ID = 0x5740
    }
}

