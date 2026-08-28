package com.scripthost.bridge

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.eclipsesource.v8.JavaCallback
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.security.PermissionManager
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Sound Bridge - Exposes audio playback to scripts.
 *
 * Playback needs no Android permission, so there is no script permission
 * check (unlike the other bridges). `Sound.playTone` is fire-and-forget:
 * it returns immediately and PCM synthesis + playback run on a background
 * executor. All active tracks are stopped and released in [unregister].
 */
class SoundBridge(
    context: Context,
    permissionManager: PermissionManager,
    scriptId: String
) : ScriptBridge {

    private val playbackExecutor = Executors.newCachedThreadPool()
    private val activeTracks = Collections.synchronizedList(mutableListOf<AudioTrack>())

    override fun register(runtime: V8) {
        val soundObject = V8Object(runtime)
        runtime.add("Sound", soundObject)
        // Supports playTone(frequencyHz, durationMs) and
        // playTone(frequencyHz, durationMs, volume)
        soundObject.registerJavaMethod(JavaCallback { _, parameters ->
            dispatchPlayTone(parameters)
            null
        }, "playTone")
        soundObject.release()
    }

    override fun unregister() {
        playbackExecutor.shutdownNow()
        val tracks = activeTracks.toList()
        activeTracks.clear()
        tracks.forEach { track ->
            try {
                track.stop()
            } catch (e: Exception) {
                // Already stopped/released by the playback task
            }
            try {
                track.release()
            } catch (e: Exception) {
                // Already released
            }
        }
    }

    /**
     * Dispatch a `Sound.playTone` call based on the number of JS arguments.
     * Invalid arguments (frequency <= 0, durationMs <= 0, NaN, non-numeric)
     * are silently ignored.
     */
    private fun dispatchPlayTone(parameters: V8Array) {
        try {
            if (parameters.length() < 2) return
            val frequencyHz = parameters.getDouble(0)
            val durationMs = parameters.getDouble(1)
            val volume = if (parameters.length() >= 3) parameters.getDouble(2) else 1.0
            if (frequencyHz.isNaN() || durationMs.isNaN() || volume.isNaN()) return
            if (frequencyHz <= 0 || durationMs <= 0 || durationMs > Int.MAX_VALUE) return
            playTone(frequencyHz, durationMs.toInt(), volume.toFloat())
        } catch (e: Exception) {
            // Non-numeric arguments: silently ignored
        }
    }

    /**
     * Synthesize the tone and play it on a background executor via a static
     * [AudioTrack]; the track is released when playback completes.
     */
    private fun playTone(frequencyHz: Double, durationMs: Int, volume: Float) {
        val pcm = synthesizeSine(frequencyHz, durationMs, SAMPLE_RATE, volume)
        if (pcm.isEmpty()) return
        playbackExecutor.execute {
            val track = try {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    pcm.size * 2, // 16-bit PCM: 2 bytes per sample
                    AudioTrack.MODE_STATIC
                )
            } catch (e: Exception) {
                return@execute
            }
            activeTracks.add(track)
            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                // MODE_STATIC has no reliable completion callback; the
                // duration is known, so wait it out (plus a small tail)
                // before releasing. Interrupted when unregister() runs.
                Thread.sleep(durationMs + PLAYBACK_TAIL_MS)
            } catch (e: Exception) {
                // Interrupted during unregister or audio failure
            } finally {
                activeTracks.remove(track)
                try {
                    track.stop()
                } catch (e: Exception) {
                    // Not playing / already stopped
                }
                try {
                    track.release()
                } catch (e: Exception) {
                    // Already released by unregister()
                }
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val FADE_MS = 5
        private const val PLAYBACK_TAIL_MS = 50L

        /**
         * Synthesize a 16-bit PCM mono sine wave with a short (~5 ms) linear
         * fade-in/out to avoid speaker clicks. [volume] is clamped to 0.0-1.0.
         * Pure math (V8- and Android-free) so it is JVM-unit-testable.
         */
        internal fun synthesizeSine(
            frequencyHz: Double,
            durationMs: Int,
            sampleRate: Int,
            volume: Float
        ): ShortArray {
            val sampleCount = (durationMs.toLong() * sampleRate / 1000L).toInt()
            if (sampleCount <= 0) return ShortArray(0)
            val samples = ShortArray(sampleCount)
            val amplitude = Short.MAX_VALUE * volume.coerceIn(0f, 1f)
            val fadeSamples = min(sampleCount / 2, sampleRate * FADE_MS / 1000)
            for (i in 0 until sampleCount) {
                var envelope = 1.0f
                if (fadeSamples > 0) {
                    if (i < fadeSamples) {
                        envelope = i.toFloat() / fadeSamples
                    } else if (i >= sampleCount - fadeSamples) {
                        envelope = (sampleCount - 1 - i).toFloat() / fadeSamples
                    }
                }
                val angle = 2.0 * PI * frequencyHz * i / sampleRate
                samples[i] = (sin(angle) * amplitude * envelope).toInt().toShort()
            }
            return samples
        }
    }
}
