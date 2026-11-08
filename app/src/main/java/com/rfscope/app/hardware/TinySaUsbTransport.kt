package com.rfscope.app.hardware

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.SystemClock
import com.rfscope.app.data.TinySaAverageMode
import com.rfscope.app.data.TinySaSpectrum
import java.io.ByteArrayOutputStream

class TinySaUsbTransport(context: Context) {
    private val appContext = context.applicationContext

    private var connection: UsbDeviceConnection? = null
    private var inputEndpoint: UsbEndpoint? = null
    private var outputEndpoint: UsbEndpoint? = null
    private var claimedInterfaces: List<UsbInterface> = emptyList()

    @Synchronized
    fun connect(): Result<Unit> = runCatching {
        if (connection != null) return@runCatching

        val manager = appContext.getSystemService(UsbManager::class.java)
            ?: error("USB manager unavailable")
        val device = manager.deviceList.values.firstOrNull { it.isTinySa() }
            ?: error("TinySA not found")
        if (!manager.hasPermission(device)) error("USB permission not granted")

        val interfaces = (0 until device.interfaceCount).map { device.getInterface(it) }
        val dataInterface = interfaces.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA }
            ?: interfaces.firstOrNull { it.hasBulkPair() }
            ?: error("CDC data interface not found")
        val controlInterface = interfaces.firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }

        val opened = manager.openDevice(device) ?: error("Unable to open TinySA")
        try {
            if (controlInterface != null) opened.claimInterface(controlInterface, true)
            if (!opened.claimInterface(dataInterface, true)) error("Unable to claim CDC data interface")

            val input = (0 until dataInterface.endpointCount)
                .map { dataInterface.getEndpoint(it) }
                .firstOrNull {
                    it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        it.direction == UsbConstants.USB_DIR_IN
                } ?: error("CDC bulk IN endpoint not found")
            val output = (0 until dataInterface.endpointCount)
                .map { dataInterface.getEndpoint(it) }
                .firstOrNull {
                    it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        it.direction == UsbConstants.USB_DIR_OUT
                } ?: error("CDC bulk OUT endpoint not found")

            if (controlInterface != null) {
                configureLine(opened, controlInterface)
            }

            connection = opened
            inputEndpoint = input
            outputEndpoint = output
            claimedInterfaces = listOfNotNull(controlInterface, dataInterface)
            Thread.sleep(120)
        } catch (error: Throwable) {
            opened.close()
            throw error
        }
    }

    @Synchronized
    fun disconnect() {
        val current = connection
        claimedInterfaces.forEach { iface -> runCatching { current?.releaseInterface(iface) } }
        runCatching { current?.close() }
        claimedInterfaces = emptyList()
        connection = null
        inputEndpoint = null
        outputEndpoint = null
    }

    @Synchronized
    fun scan(startHz: Long, stopHz: Long, points: Int = 200, rbwKhz: Int? = null, averageMode: TinySaAverageMode = TinySaAverageMode.OFF): Result<TinySaSpectrum> = runCatching {
        require(startHz in 1L until stopHz) { "Invalid frequency range" }
        require(points in 2..1000) { "Invalid point count" }

        val opened = connection ?: error("TinySA not connected")
        val input = inputEndpoint ?: error("CDC input endpoint missing")
        val output = outputEndpoint ?: error("CDC output endpoint missing")

        sendCommand(opened, output, if (rbwKhz == null) "rbw auto\r" else "rbw $rbwKhz\r")
        sendCommand(opened, output, when (averageMode) {
            TinySaAverageMode.OFF -> "calc off\r"
            TinySaAverageMode.AVER4 -> "calc aver4\r"
            TinySaAverageMode.AVER16 -> "calc aver16\r"
        })

        val command = "scanraw $startHz $stopHz $points 1\r".toByteArray(Charsets.US_ASCII)
        val written = opened.bulkTransfer(output, command, command.size, 2000)
        if (written < 0) error("TinySA command write failed")

        val expectedBytes = points * 3 + 2
        val frame = readFrame(opened, input, expectedBytes, timeoutMs = 8000)
        val result = parseSpectrum(frame, startHz, stopHz, points)
        result
    }

    @Synchronized
    fun abort() {
        val opened = connection ?: return
        val output = outputEndpoint ?: return
        val command = "abort\r".toByteArray(Charsets.US_ASCII)
        runCatching { opened.bulkTransfer(output, command, command.size, 500) }
    }

    private fun sendCommand(connection: UsbDeviceConnection, endpoint: UsbEndpoint, command: String) {
        val bytes = command.toByteArray(Charsets.US_ASCII)
        connection.bulkTransfer(endpoint, bytes, bytes.size, 1000)
        Thread.sleep(25)
    }

    private fun configureLine(connection: UsbDeviceConnection, control: UsbInterface) {
        val lineCoding = byteArrayOf(
            0x00, 0xC2.toByte(), 0x01, 0x00, // 115200 baud, little endian
            0x00, // one stop bit
            0x00, // no parity
            0x08, // eight data bits
        )
        connection.controlTransfer(
            0x21,
            0x20,
            0,
            control.id,
            lineCoding,
            lineCoding.size,
            1000,
        )
        connection.controlTransfer(
            0x21,
            0x22,
            0x0003,
            control.id,
            null,
            0,
            1000,
        )
    }

    private fun readFrame(
        connection: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        expectedBytes: Int,
        timeoutMs: Long,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        val deadline = SystemClock.elapsedRealtime() + timeoutMs

        while (SystemClock.elapsedRealtime() < deadline) {
            val read = connection.bulkTransfer(endpoint, buffer, buffer.size, 500)
            if (read > 0) {
                output.write(buffer, 0, read)
                val bytes = output.toByteArray()
                val start = bytes.indexOfFirst { it == '{'.code.toByte() }
                if (start >= 0 && bytes.size >= start + expectedBytes) {
                    return bytes.copyOfRange(start, start + expectedBytes)
                }
            }
        }
        error("TinySA scan response timeout")
    }

    private fun parseSpectrum(
        frame: ByteArray,
        startHz: Long,
        stopHz: Long,
        points: Int,
    ): TinySaSpectrum {
        if (frame.size != points * 3 + 2 || frame.first() != '{'.code.toByte() || frame.last() != '}'.code.toByte()) {
            error("Invalid TinySA scan frame")
        }

        val frequencies = DoubleArray(points)
        val powers = DoubleArray(points)
        val span = stopHz - startHz
        for (index in 0 until points) {
            val offset = 1 + index * 3
            if (frame[offset] != 'x'.code.toByte()) error("Invalid TinySA sample marker")
            val raw = (frame[offset + 1].toInt() and 0xFF) or
                ((frame[offset + 2].toInt() and 0xFF) shl 8)
            val signed = if (raw and 0x8000 != 0) raw - 0x10000 else raw
            frequencies[index] = startHz + span * index / (points - 1.0)
            powers[index] = signed / 32.0 - 174.0
        }

        val peakIndex = powers.indices.maxByOrNull { powers[it] } ?: 0
        val sorted = powers.sortedArray()
        val noiseFloor = sorted[(sorted.size * 0.2).toInt().coerceIn(0, sorted.lastIndex)]
        return TinySaSpectrum(
            startHz = startHz,
            stopHz = stopHz,
            points = points,
            frequenciesHz = frequencies,
            powersDbm = powers,
            peakIndex = peakIndex,
            peakHz = frequencies[peakIndex],
            peakDbm = powers[peakIndex],
            noiseFloorDbm = noiseFloor,
            capturedAtMs = System.currentTimeMillis(),
        )
    }

    private fun UsbDevice.isTinySa(): Boolean =
        (vendorId == TINYSA_VID && productId == TINYSA_PID) ||
            (0 until interfaceCount).any { index -> getInterface(index).hasBulkPair() }

    private fun UsbInterface.hasBulkPair(): Boolean {
        var hasIn = false
        var hasOut = false
        for (index in 0 until endpointCount) {
            val endpoint = getEndpoint(index)
            if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
            if (endpoint.direction == UsbConstants.USB_DIR_IN) hasIn = true
            if (endpoint.direction == UsbConstants.USB_DIR_OUT) hasOut = true
        }
        return hasIn && hasOut
    }

    private companion object {
        const val TINYSA_VID = 0x0483
        const val TINYSA_PID = 0x5740
    }
}

