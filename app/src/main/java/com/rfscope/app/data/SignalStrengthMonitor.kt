package com.rfscope.app.data

import android.content.Context
import android.os.Build
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.util.concurrent.Executor

data class SignalMetrics(
    val rsrp: Int?,
    val rsrq: Int?,
    val sinr: Int?,
    val source: String,
)

class SignalStrengthMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val executor: Executor = appContext.mainExecutor
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var manager: TelephonyManager? = null

    fun start(onMetrics: (SignalMetrics) -> Unit): Result<Unit> = runCatching {
        stop()
        val base = appContext.getSystemService(TelephonyManager::class.java)
            ?: error("Telephony service unavailable")
        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
        val tm = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            base.createForSubscriptionId(subId)
        } else {
            base
        }
        manager = tm

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    parse(signalStrength)?.let(onMetrics)
                }
            }
            tm.registerTelephonyCallback(executor, callback)
            telephonyCallback = callback
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    signalStrength?.let { parse(it)?.let(onMetrics) }
                }
            }
            @Suppress("DEPRECATION")
            tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            phoneStateListener = listener
        }
    }

    fun stop() {
        val tm = manager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                runCatching { tm?.unregisterTelephonyCallback(callback) }
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { listener ->
                runCatching { tm?.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        }
        telephonyCallback = null
        phoneStateListener = null
        manager = null
    }

    private fun parse(signalStrength: SignalStrength): SignalMetrics? {
        val cells = signalStrength.cellSignalStrengths
        val nr = cells.filterIsInstance<CellSignalStrengthNr>().firstOrNull()
        if (nr != null) {
            return SignalMetrics(
                rsrp = nr.ssRsrp.validDbm(),
                rsrq = nr.ssRsrq.validDbm(),
                sinr = nr.ssSinr.validDbm(),
                source = "NR callback",
            )
        }
        val lte = cells.filterIsInstance<CellSignalStrengthLte>().firstOrNull()
            ?: return null
        return SignalMetrics(
            rsrp = lte.rsrp.validDbm(),
            rsrq = lte.rsrq.validDbm(),
            sinr = lte.rssnr.validDbm(),
            source = "LTE callback",
        )
    }

    private fun Int.validDbm(): Int? =
        takeUnless { it == Int.MAX_VALUE || it == CellInfoUnavailableValue }
}

private const val CellInfoUnavailableValue = Int.MAX_VALUE
