package com.rfscope.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Reads the currently serving NR cell when Android/OEM exposes it.
 *
 * NSA caveat: many devices expose only the LTE anchor through CellInfo, even
 * while TelephonyManager reports NETWORK_TYPE_NR. This reader never invents an
 * NR band/frequency when CellIdentityNr is absent.
 */
class CellInfoReader(context: Context) {
    private val appContext = context.applicationContext

    fun read(): CellularSnapshot {
        if (!hasPreciseLocationPermission()) {
            return CellularSnapshot(
                status = CellReadStatus.PERMISSION_REQUIRED,
                detailEn = "Precise location and phone-state permissions are required.",
                detailZh = "需要精确位置与电话状态权限。",
            )
        }

        if (!isLocationEnabled()) {
            return CellularSnapshot(
                status = CellReadStatus.LOCATION_DISABLED,
                detailEn = "Turn on system Location; Android gates cell identity behind it.",
                detailZh = "请开启系统定位；Android 将小区身份信息与定位权限绑定。",
            )
        }

        return runCatching {
            readUnchecked()
        }.getOrElse { error ->
            CellularSnapshot(
                status = CellReadStatus.ERROR,
                detailEn = error.message ?: "Telephony API rejected the request.",
                detailZh = "Telephony API 请求失败或被系统限制。",
            )
        }
    }

    private fun readUnchecked(): CellularSnapshot {
        val baseManager = appContext.getSystemService(TelephonyManager::class.java)
            ?: return CellularSnapshot(
                status = CellReadStatus.ERROR,
                detailEn = "Telephony service is unavailable on this device.",
                detailZh = "此设备不提供电话服务。",
            )

        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        val manager = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            runCatching { baseManager.createForSubscriptionId(subId) }.getOrDefault(baseManager)
        } else {
            baseManager
        }

        val dataNetworkType = runCatching { manager.dataNetworkType }.getOrNull()
        val networkType = networkTypeLabel(dataNetworkType)
        val cells = runCatching { manager.allCellInfo }.getOrNull().orEmpty()

        val nrCells = cells.filterIsInstance<CellInfoNr>()
        val servingNr = nrCells.firstOrNull { it.isRegistered } ?: nrCells.firstOrNull()

        if (servingNr == null) {
            val nrRadioDetected = dataNetworkType == TelephonyManager.NETWORK_TYPE_NR
            return CellularSnapshot(
                operator = resolveOperator(manager, null),
                networkType = networkType,
                status = CellReadStatus.NO_NR_CELL,
                detailEn = if (nrRadioDetected) {
                    "NR data radio detected, but CellInfoNr is hidden by the device/ROM."
                } else {
                    "No NR serving cell. NSA networks may expose only the LTE anchor."
                },
                detailZh = if (nrRadioDetected) {
                    "已检测到 NR 数据网络，但设备/ROM 未暴露 CellInfoNr。"
                } else {
                    "未读取到 NR 服务小区；NSA 网络可能仅暴露 LTE 锚点。"
                },
            )
        }

        val identity = servingNr.cellIdentity as? CellIdentityNr
            ?: return CellularSnapshot(
                operator = resolveOperator(manager, null),
                networkType = networkType,
                status = CellReadStatus.NO_NR_CELL,
                detailEn = "CellInfoNr has no CellIdentityNr payload.",
                detailZh = "CellInfoNr 中没有 CellIdentityNr 数据。",
            )

        val signal = servingNr.cellSignalStrength as? CellSignalStrengthNr
        val operator = resolveOperator(manager, identity)
        val nrarfcn = identity.nrarfcn.takeUnless { it == CellInfo.UNAVAILABLE || it < 0 }
        val frequencyMhz = NrArfcn.toMhz(nrarfcn)
        val reportedBands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            identity.bands
        } else {
            intArrayOf()
        }
        val band = BandCatalog.resolve(frequencyMhz, operator, reportedBands)

        return CellularSnapshot(
            operator = operator,
            networkType = networkType,
            band = band,
            frequencyMhz = frequencyMhz,
            nrarfcn = nrarfcn,
            pci = identity.pci.takeUnless { it == CellInfo.UNAVAILABLE || it < 0 },
            tac = identity.tac.takeUnless {
                it == CellInfo.UNAVAILABLE || it < 0
            },
            rsrp = signal?.ssRsrp.validDbm(),
            rsrq = signal?.ssRsrq.validDbm(),
            sinr = signal?.ssSinr.validDbm(),
            status = CellReadStatus.READY,
            detailEn = if (servingNr.isRegistered) {
                "Serving NR cell"
            } else {
                "NR cell found but not flagged as registered"
            },
            detailZh = if (servingNr.isRegistered) {
                "当前 NR 服务小区"
            } else {
                "发现 NR 小区，但系统未标记为已注册"
            },
        )
    }

    private fun resolveOperator(
        manager: TelephonyManager,
        identity: CellIdentityNr?,
    ): OperatorInfo? {
        val fromCell = OperatorCatalog.resolve(identity?.mccString, identity?.mncString)
        if (fromCell != null) return fromCell

        val plmn = runCatching { manager.networkOperator }.getOrNull()
        return OperatorCatalog.resolveFromPlmn(plmn)
    }

    private fun hasPreciseLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine && coarse
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = appContext.getSystemService(LocationManager::class.java)
            ?: return false
        return locationManager.isLocationEnabled
    }

    private fun networkTypeLabel(type: Int?): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
        TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
        TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
        TelephonyManager.NETWORK_TYPE_UNKNOWN, null -> "UNKNOWN"
        else -> "TYPE_$type"
    }

    private fun Int?.validDbm(): Int? =
        this?.takeUnless { it == CellInfo.UNAVAILABLE || it == Int.MAX_VALUE }
}


