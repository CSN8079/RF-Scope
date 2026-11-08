package com.rfscope.app.hardware

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Maps the measured peak level to an audible oscillator tone.
 * Stronger signals raise both pitch and loudness.
 */
class SignalTonePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var track: AudioTrack? = null

    @Volatile private var targetFrequency = 440.0
    @Volatile private var targetAmplitude = 0.2

    fun start() {
        if (job?.isActive == true) return
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuffer.coerceAtLeast(4096))
            .build()
        track = audioTrack
        audioTrack.play()
        job = scope.launch {
            val buffer = ShortArray(512)
            var phase = 0.0
            var frequency = targetFrequency
            var amplitude = targetAmplitude
            while (isActive) {
                frequency += (targetFrequency - frequency) * 0.08
                amplitude += (targetAmplitude - amplitude) * 0.08
                val step = 2.0 * PI * frequency / SAMPLE_RATE
                for (index in buffer.indices) {
                    buffer[index] = (sin(phase) * amplitude * Short.MAX_VALUE).toInt().toShort()
                    phase += step
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                }
                val written = audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                if (written < 0 || !isActive) break
            }
        }
    }

    fun updateLevel(levelDbm: Double, referenceDbm: Double = -20.0) {
        val reference = referenceDbm.coerceIn(-100.0, -10.0)
        val floor = reference - 80.0
        val normalized = ((levelDbm.coerceIn(floor, reference) - floor) / 80.0)
        targetFrequency = 220.0 + normalized * 1500.0
        targetAmplitude = 0.05 + normalized * 0.55
    }

    fun stop() {
        job?.cancel()
        job = null
        val audioTrack = track
        track = null
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.release() }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    companion object {
        const val SAMPLE_RATE = 44_100

        fun frequencyForLevel(levelDbm: Double, referenceDbm: Double = -20.0): Double {
            val reference = referenceDbm.coerceIn(-100.0, -10.0)
            val floor = reference - 80.0
            val normalized = ((levelDbm.coerceIn(floor, reference) - floor) / 80.0)
            return 220.0 + normalized * 1500.0
        }
    }
}

