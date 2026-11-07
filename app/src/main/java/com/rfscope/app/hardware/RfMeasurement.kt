package com.rfscope.app.hardware

import kotlinx.coroutines.flow.StateFlow

data class SpectrumSweep(
    val frequencyHz: DoubleArray,
    val powerDbm: DoubleArray,
) {
    init {
        require(frequencyHz.size == powerDbm.size) { "frequency and power arrays must match" }
    }

    val size: Int get() = frequencyHz.size
}

enum class HardwareConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Transport abstraction for TinySA Ultra over BLE/USB serial.
 * A concrete transport owns framing, checksum, timeout and reconnection.
 */
interface RfHardwareTransport {
    val connectionState: StateFlow<HardwareConnectionState>

    suspend fun connect(): Result<Unit>

    suspend fun disconnect()

    suspend fun sweep(
        startHz: Long,
        stopHz: Long,
        points: Int,
    ): Result<SpectrumSweep>
}

/** Reserved interface for the HT8 directional antenna / rotation controller. */
interface DirectionalAntennaController {
    suspend fun setAzimuthDegrees(degrees: Double): Result<Unit>

    suspend fun readRssiDbm(): Result<Double>
}

object RfAnalysis {
    fun snrDb(signalDbm: Double, noiseFloorDbm: Double): Double = signalDbm - noiseFloorDbm

    fun peakIndex(powerDbm: DoubleArray, from: Int = 0, until: Int = powerDbm.size): Int? {
        if (from < 0 || until > powerDbm.size || from >= until) return null
        var index = from
        for (i in from + 1 until until) {
            if (powerDbm[i] > powerDbm[index]) index = i
        }
        return index
    }

    /**
     * Sample-domain -3 dB width estimate. Interpolation across threshold
     * crossings should be added for production-grade measurements.
     */
    fun bandwidthAtMinus3Db(sweep: SpectrumSweep, peak: Int? = null): Double? {
        if (sweep.size < 2) return null
        val peakAt = peak ?: peakIndex(sweep.powerDbm) ?: return null
        val threshold = sweep.powerDbm[peakAt] - 3.0

        var left = peakAt
        while (left > 0 && sweep.powerDbm[left - 1] >= threshold) left--

        var right = peakAt
        while (right < sweep.size - 1 && sweep.powerDbm[right + 1] >= threshold) right++

        return sweep.frequencyHz[right] - sweep.frequencyHz[left]
    }

    fun harmonicOrder(
        fundamentalHz: Double,
        observedHz: Double,
        tolerancePercent: Double = 1.5,
    ): Int? {
        if (fundamentalHz <= 0.0 || observedHz <= 0.0) return null
        val order = (observedHz / fundamentalHz).toInt().coerceAtLeast(2)
        val expected = fundamentalHz * order
        val errorPercent = kotlin.math.abs(observedHz - expected) / expected * 100.0
        return if (errorPercent <= tolerancePercent) order else null
    }
}

class MaxHold(initialSize: Int? = null) {
    private var held: DoubleArray? = initialSize?.let { DoubleArray(it) { Double.NEGATIVE_INFINITY } }

    fun update(powerDbm: DoubleArray): DoubleArray {
        val current = held
        held = if (current == null || current.size != powerDbm.size) {
            powerDbm.copyOf()
        } else {
            DoubleArray(powerDbm.size) { i -> maxOf(current[i], powerDbm[i]) }
        }
        return held!!.copyOf()
    }

    fun reset(size: Int? = null) {
        held = size?.let { DoubleArray(it) { Double.NEGATIVE_INFINITY } }
    }
}

class RunningAverage(private var alpha: Double = 0.25) {
    private var average: DoubleArray? = null

    init {
        require(alpha in 0.0..1.0) { "alpha must be in [0, 1]" }
    }

    fun update(powerDbm: DoubleArray): DoubleArray {
        val previous = average
        average = if (previous == null || previous.size != powerDbm.size) {
            powerDbm.copyOf()
        } else {
            DoubleArray(powerDbm.size) { i ->
                previous[i] + alpha * (powerDbm[i] - previous[i])
            }
        }
        return average!!.copyOf()
    }

    fun reset() {
        average = null
    }
}


