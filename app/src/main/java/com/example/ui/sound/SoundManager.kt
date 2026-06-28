package com.example.ui.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

object RetroSoundPlayer {
    fun playCoinSound() {
        playTone(frequencyStart = 660.0, frequencyEnd = 990.0, durationMs = 120, isSquare = false)
    }

    fun playCrashSound() {
        playTone(frequencyStart = 220.0, frequencyEnd = 60.0, durationMs = 450, isSquare = true)
    }

    fun playPowerupSound() {
        playTone(frequencyStart = 440.0, frequencyEnd = 880.0, durationMs = 250, isSquare = false)
    }

    fun playClickSound() {
        playTone(frequencyStart = 800.0, frequencyEnd = 800.0, durationMs = 40, isSquare = false)
    }

    fun playBoostSound() {
        playTone(frequencyStart = 300.0, frequencyEnd = 1200.0, durationMs = 300, isSquare = true)
    }

    private fun playTone(
        frequencyStart: Double,
        frequencyEnd: Double,
        durationMs: Int,
        isSquare: Boolean = false
    ) {
        Thread {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@Thread
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentFreq = frequencyStart + (frequencyEnd - frequencyStart) * progress
                    val angle = 2.0 * Math.PI * i / (sampleRate / currentFreq)
                    val value = if (isSquare) {
                        if (sin(angle) >= 0.0) 6000.0 else -6000.0
                    } else {
                        sin(angle) * 10000.0
                    }
                    buffer[i] = value.toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
