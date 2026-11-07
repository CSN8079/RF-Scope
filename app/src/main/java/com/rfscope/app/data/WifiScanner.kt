package com.rfscope.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

class WifiScanner(context: Context) {
    private val appContext = context.applicationContext

    /**
     * Call from a background dispatcher. startScan is throttled by the platform
     * and may return false while cached results are still available.
     */
    fun scan(): WifiScanOutcome {
        if (!hasWifiPermissions()) {
            return WifiScanOutcome(
                status = WifiScanStatus.PERMISSION_REQUIRED,
                detailEn = "Precise location and Nearby Wi-Fi permission are required.",
                detailZh = "需要精确位置与附近 Wi-Fi 设备权限。",
            )
        }

        if (!isLocationEnabled()) {
            return WifiScanOutcome(
                status = WifiScanStatus.LOCATION_DISABLED,
                detailEn = "Turn on system Location before scanning Wi-Fi.",
                detailZh = "扫描 Wi-Fi 前请开启系统定位。",
            )
        }

        val wifiManager = appContext.getSystemService(WifiManager::class.java)
            ?: return WifiScanOutcome(
                status = WifiScanStatus.ERROR,
                detailEn = "Wi-Fi service is unavailable.",
                detailZh = "设备不提供 Wi-Fi 服务。",
            )

        val started = runCatching { wifiManager.startScan() }.getOrDefault(false)
        if (started) {
            // Scan results are delivered asynchronously; this is a small skeleton
            // delay. Production code should also observe SCAN_RESULTS_AVAILABLE_ACTION.
            runCatching { Thread.sleep(700) }
        }

        val results = runCatching { wifiManager.scanResults }.getOrElse { error ->
            return WifiScanOutcome(
                status = WifiScanStatus.ERROR,
                detailEn = error.message ?: "Wi-Fi scan was rejected.",
                detailZh = "Wi-Fi 扫描请求失败或被系统拒绝。",
            )
        }

        val networks = results
            .asSequence()
            .filter { it.frequency > 0 }
            .sortedByDescending { it.level }
            .map(::toNetwork)
            .toList()

        return WifiScanOutcome(
            status = if (networks.isEmpty() && !started) {
                WifiScanStatus.THROTTLED
            } else {
                WifiScanStatus.READY
            },
            networks = networks,
            detailEn = if (!started) {
                "Scan was throttled; showing cached results."
            } else {
                null
            },
            detailZh = if (!started) {
                "扫描被系统限流；当前显示缓存结果。"
            } else {
                null
            },
        )
    }

    private fun toNetwork(result: ScanResult): WifiNetwork {
        val band = bandName(result.frequency)
        return WifiNetwork(
            ssid = result.SSID?.takeIf { it.isNotBlank() } ?: "<hidden>",
            bssid = result.BSSID ?: "<redacted>",
            frequencyMhz = result.frequency,
            channel = channelFor(result.frequency),
            widthMhz = widthFor(result.channelWidth),
            levelDbm = result.level,
            band = band,
            hintEn = hintEn(band),
            hintZh = hintZh(band),
        )
    }

    private fun hasWifiPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val nearbyWifi = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return fineLocation && nearbyWifi
    }

    private fun isLocationEnabled(): Boolean {
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return false
        return manager.isLocationEnabled
    }

    private fun bandName(frequencyMhz: Int): String = when (frequencyMhz) {
        in 2400..2500 -> "2.4 GHz"
        in 4900..5925 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        else -> "UNKNOWN"
    }

    private fun channelFor(frequencyMhz: Int): Int? = when (frequencyMhz) {
        2484 -> 14
        in 2412..2472 -> (frequencyMhz - 2407) / 5
        in 4910..5895 -> (frequencyMhz - 5000) / 5
        in 5925..7125 -> (frequencyMhz - 5950) / 5
        else -> null
    }

    private fun widthFor(channelWidth: Int): Int? = when (channelWidth) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> 20
        ScanResult.CHANNEL_WIDTH_40MHZ -> 40
        ScanResult.CHANNEL_WIDTH_80MHZ -> 80
        ScanResult.CHANNEL_WIDTH_160MHZ -> 160
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
        else -> null
    }

    private fun hintEn(band: String): String = when (band) {
        "2.4 GHz" -> "Congested band; Bluetooth and microwave ovens can interfere."
        "5 GHz" -> "Usually cleaner, but wall penetration is weaker."
        "6 GHz" -> "Wide and clean channels, but coverage is shorter."
        else -> "Unknown band."
    }

    private fun hintZh(band: String): String = when (band) {
        "2.4 GHz" -> "信道拥堵，易受蓝牙/微波炉干扰。"
        "5 GHz" -> "信道通常较干净，但穿透力差。"
        "6 GHz" -> "带宽大、干扰少，但覆盖距离短。"
        else -> "未知频段。"
    }
}

