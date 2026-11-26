package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object MsnSoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val SAMPLE_RATE = 44100

    fun playSignIn(enabled: Boolean) {
        if (!enabled) return
        // Classic 4-note ascending MSN crystal chime
        playNotes(
            listOf(
                ToneNote(523.25, 0.00, 0.24, 0.35), // C5
                ToneNote(659.25, 0.10, 0.26, 0.35), // E5
                ToneNote(783.99, 0.20, 0.30, 0.35), // G5
                ToneNote(1046.50, 0.32, 0.55, 0.45) // C6
            )
        )
    }

    fun playContactOnline(enabled: Boolean) {
        if (!enabled) return
        playNotes(
            listOf(
                ToneNote(659.25, 0.00, 0.18, 0.28),
                ToneNote(830.61, 0.09, 0.22, 0.28),
                ToneNote(987.77, 0.18, 0.36, 0.32)
            )
        )
    }

    fun playNewMessage(enabled: Boolean) {
        if (!enabled) return
        // Classic 2-note crisp MSN message alert
        playNotes(
            listOf(
                ToneNote(783.99, 0.00, 0.10, 0.40),
                ToneNote(1174.66, 0.08, 0.22, 0.45)
            )
        )
    }

    fun playNudge(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        if (vibrateEnabled) {
            triggerNudgeVibration(context)
        }
        if (!soundEnabled) return
        // Classic energetic MSN nudge buzz
        val notes = mutableListOf<ToneNote>()
        var t = 0.0
        for (i in 0 until 6) {
            notes.add(ToneNote(310.0 + (i % 2) * 140.0, t, 0.06, 0.50))
            notes.add(ToneNote(620.0 + (i % 2) * 200.0, t, 0.06, 0.30))
            t += 0.075
        }
        playNotes(notes)
    }

    fun playWink(enabled: Boolean) {
        if (!enabled) return
        playNotes(
            listOf(
                ToneNote(587.33, 0.00, 0.12, 0.30),
                ToneNote(739.99, 0.07, 0.12, 0.30),
                ToneNote(880.00, 0.14, 0.14, 0.32),
                ToneNote(1174.66, 0.22, 0.35, 0.38)
            )
        )
    }

    fun playGameMove(enabled: Boolean) {
        if (!enabled) return
        playNotes(listOf(ToneNote(680.0, 0.0, 0.08, 0.28)))
    }

    fun playGameWin(enabled: Boolean) {
        if (!enabled) return
        playNotes(
            listOf(
                ToneNote(523.25, 0.00, 0.14, 0.35),
                ToneNote(659.25, 0.11, 0.14, 0.35),
                ToneNote(783.99, 0.22, 0.14, 0.35),
                ToneNote(1046.50, 0.34, 0.45, 0.45)
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun triggerNudgeVibration(context: Context) {
        try {
            val pattern = longArrayOf(0, 75, 45, 85, 45, 110, 45, 90)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    v?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w("MsnSound", "Vibration unavailable: ${e.message}")
        }
    }

    private data class ToneNote(
        val freqHz: Double,
        val startSec: Double,
        val durationSec: Double,
        val volume: Double
    )

    private fun playNotes(notes: List<ToneNote>) {
        scope.launch {
            try {
                val totalSec = notes.maxOfOrNull { it.startSec + it.durationSec } ?: 0.5
                val totalSamples = (totalSec * SAMPLE_RATE).toInt().coerceAtLeast(1)
                val pcm = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var sample = 0.0
                    for (note in notes) {
                        val dt = t - note.startSec
                        if (dt >= 0.0 && dt < note.durationSec) {
                            val attack = (dt / 0.012).coerceAtMost(1.0)
                            val decay = exp(-4.5 * (dt / note.durationSec))
                            val wave = sin(2.0 * PI * note.freqHz * dt) +
                                0.25 * sin(4.0 * PI * note.freqHz * dt)
                            sample += wave * attack * decay * note.volume
                        }
                    }
                    val clamped = (sample.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt()
                    pcm[i] = clamped.toShort()
                }
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                val sleepMs = (totalSec * 1000).toLong() + 150L
                Thread.sleep(sleepMs)
                track.release()
            } catch (e: Exception) {
                Log.w("MsnSound", "AudioTrack playback skipped: ${e.message}")
            }
        }
    }
}

class VoiceClipRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0L

    fun startRecording(): Boolean {
        return try {
            stopAndDiscard()
            val file = File(context.cacheDir, "msn_voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(24000)
            rec.setAudioSamplingRate(16000)
            rec.setMaxDuration(30_000)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            startMs = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to start recording", e)
            stopAndDiscard()
            false
        }
    }

    fun stopAndEncode(): Pair<String, Long>? {
        val file = outputFile ?: return null
        val duration = (System.currentTimeMillis() - startMs).coerceAtLeast(300L)
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val bytes = file.readBytes()
            file.delete()
            outputFile = null
            if (bytes.isEmpty() || bytes.size > 620_000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP) to duration
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to stop recording", e)
            stopAndDiscard()
            null
        }
    }

    fun stopAndDiscard() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}

class VoiceClipPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var currentId: String? = null

    fun play(messageId: String, base64: String, onFinished: () -> Unit) {
        stop()
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "play_voice_$messageId.m4a")
            tempFile.writeBytes(bytes)
            currentId = messageId
            player = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    onFinished()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("VoicePlayer", "Failed to play voice clip", e)
            stop()
            onFinished()
        }
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        currentId = null
    }
}
