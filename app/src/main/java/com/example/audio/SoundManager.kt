package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundManager {
  companion object {
    private const val SAMPLE_RATE = 22050
    private const val TWO_PI = (2.0 * PI).toFloat()
  }

  private sealed class OneShotSfx(val durationSamples: Int) {
    var cursor: Int = 0

    class Click : OneShotSfx((SAMPLE_RATE * 0.055f).toInt())
    class Coin : OneShotSfx((SAMPLE_RATE * 0.24f).toInt())
    class NearMiss : OneShotSfx((SAMPLE_RATE * 0.28f).toInt())
    class Countdown(val isGo: Boolean) : OneShotSfx((SAMPLE_RATE * if (isGo) 0.35f else 0.16f).toInt())
    class Crash : OneShotSfx((SAMPLE_RATE * 0.55f).toInt())
    class Victory : OneShotSfx((SAMPLE_RATE * 0.90f).toInt())
  }

  @Volatile var soundEnabled: Boolean = true
  @Volatile var musicEnabled: Boolean = true

  // Real-time vehicle telemetry fed by RaceSimulation
  @Volatile var engineActive: Boolean = false
  @Volatile var engineSpeedNorm: Float = 0f // 0..1.3
  @Volatile var engineThrottle: Boolean = false
  @Volatile var engineBrake: Boolean = false
  @Volatile var engineNitro: Boolean = false

  @Volatile private var isRunning = false
  @Volatile private var isPaused = false
  private var audioThread: Thread? = null
  private val activeSfx = ConcurrentLinkedQueue<OneShotSfx>()

  fun start() {
    if (isRunning) return
    isRunning = true
    audioThread =
      Thread(::audioLoop, "HighwayRushAudioThread").apply {
        priority = Thread.MAX_PRIORITY
        start()
      }
  }

  fun setAppPaused(paused: Boolean) {
    isPaused = paused
  }

  fun release() {
    isRunning = false
    audioThread?.join(300)
    audioThread = null
    activeSfx.clear()
  }

  fun playClick() {
    if (soundEnabled) enqueueSfx(OneShotSfx.Click())
  }

  fun playCoin() {
    if (soundEnabled) enqueueSfx(OneShotSfx.Coin())
  }

  fun playNearMiss() {
    if (soundEnabled) enqueueSfx(OneShotSfx.NearMiss())
  }

  fun playCountdown(isGo: Boolean) {
    if (soundEnabled) enqueueSfx(OneShotSfx.Countdown(isGo))
  }

  fun playCrash() {
    if (soundEnabled) enqueueSfx(OneShotSfx.Crash())
  }

  fun playVictory() {
    if (soundEnabled) enqueueSfx(OneShotSfx.Victory())
  }

  private fun enqueueSfx(sfx: OneShotSfx) {
    if (activeSfx.size < 8) {
      activeSfx.add(sfx)
    }
  }

  private fun audioLoop() {
    val minBuf =
      AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      )
    val chunkSamples = (minBuf / 2).coerceIn(512, 1024)
    val track =
      try {
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(SAMPLE_RATE)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(chunkSamples * 4)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
      } catch (_: Exception) {
        return
      }

    val buffer = ShortArray(chunkSamples)
    var engPhase1 = 0f
    var engPhase2 = 0f
    var engSubPhase = 0f
    var nitroPhase = 0f
    var musicPhaseBass = 0f
    var musicPhaseLead = 0f
    var musicSampleCounter = 0L
    var smoothedSpeed = 0f
    var brakeNoiseState = 0f

    // 132 BPM synthwave loop
    val samplesPer16th = (SAMPLE_RATE * 60f / 132f / 4f).toInt()
    val bassNotes = floatArrayOf(55f, 55f, 65.41f, 55f, 73.42f, 73.42f, 82.41f, 65.41f)
    val leadNotes =
      floatArrayOf(220f, 261.63f, 329.63f, 392f, 329.63f, 261.63f, 293.66f, 392f, 440f, 392f, 329.63f, 261.63f, 293.66f, 329.63f, 392f, 493.88f)

    try {
      track.play()
    } catch (_: Exception) {
      track.release()
      return
    }

    val rng = Random(42)
    while (isRunning) {
      if (isPaused) {
        buffer.fill(0)
        try {
          track.write(buffer, 0, buffer.size)
        } catch (_: Exception) {
          break
        }
        continue
      }

      val targetSpeed = engineSpeedNorm
      val sfxOn = soundEnabled
      val musOn = musicEnabled
      val engOn = engineActive && sfxOn

      for (i in 0 until chunkSamples) {
        var mix = 0f

        // 1. Background Racing Music (Energetic Synthwave Bass + Arp + Kick/Hat)
        if (musOn) {
          val step = (musicSampleCounter / samplesPer16th).toInt()
          val stepPos = (musicSampleCounter % samplesPer16th).toFloat() / samplesPer16th
          val bassFreq = bassNotes[(step / 2) % bassNotes.size]
          val leadFreq = leadNotes[step % leadNotes.size]

          musicPhaseBass = (musicPhaseBass + TWO_PI * bassFreq / SAMPLE_RATE) % TWO_PI
          musicPhaseLead = (musicPhaseLead + TWO_PI * leadFreq / SAMPLE_RATE) % TWO_PI

          val sawBass = (musicPhaseBass / PI.toFloat()) - 1f
          val bassEnv = (1f - stepPos * 0.55f)
          val leadTri = kotlin.math.abs((musicPhaseLead / PI.toFloat()) - 1f) * 2f - 1f
          val leadEnv = (1f - stepPos).coerceIn(0f, 1f) * 0.5f

          // Kick every quarter note (every 4 16ths)
          val kick =
            if (step % 4 == 0 && stepPos < 0.65f) {
              val kFreq = 130f * (1f - stepPos * 1.1f).coerceAtLeast(0.28f)
              sin(TWO_PI * kFreq * stepPos * (samplesPer16th.toFloat() / SAMPLE_RATE)) *
                (1f - stepPos * 1.4f).coerceAtLeast(0f) *
                0.35f
            } else {
              0f
            }
          // Crisp offbeat hi-hat
          val hat =
            if (step % 2 == 1 && stepPos < 0.25f) {
              (rng.nextFloat() * 2f - 1f) * (1f - stepPos * 4f) * 0.08f
            } else {
              0f
            }

          val musicGain = if (engineActive) 0.14f else 0.20f
          mix += (sawBass * bassEnv * 0.24f + leadTri * leadEnv * 0.18f + kick + hat) * musicGain
          musicSampleCounter++
        }

        // 2. Real-time Car Engine, Brake Screech & Nitro Boost
        if (engOn) {
          smoothedSpeed += (targetSpeed - smoothedSpeed) * 0.0025f
          val gearMod = ((smoothedSpeed * 3.8f) % 1f)
          val baseRpmHz =
            46f +
              smoothedSpeed * 115f +
              gearMod * 24f +
              (if (engineThrottle) 14f else 0f) +
              (if (engineNitro) 34f else 0f)

          engPhase1 = (engPhase1 + TWO_PI * baseRpmHz / SAMPLE_RATE) % TWO_PI
          engPhase2 = (engPhase2 + TWO_PI * baseRpmHz * 2.0f / SAMPLE_RATE) % TWO_PI
          engSubPhase = (engSubPhase + TWO_PI * baseRpmHz * 0.5f / SAMPLE_RATE) % TWO_PI

          val saw1 = (engPhase1 / PI.toFloat()) - 1f
          val pulse2 = if (engPhase2 < PI) 0.6f else -0.6f
          val sub = sin(engSubPhase)
          val throttleBoost = if (engineThrottle || engineNitro) 1.25f else 0.72f
          mix += (saw1 * 0.11f + pulse2 * 0.06f + sub * 0.12f) * throttleBoost

          // Nitro jet turbine whine + filtered noise
          if (engineNitro) {
            nitroPhase = (nitroPhase + TWO_PI * (520f + smoothedSpeed * 280f) / SAMPLE_RATE) % TWO_PI
            val jetTone = sin(nitroPhase) * 0.09f
            val jetRush = (rng.nextFloat() * 2f - 1f) * 0.10f
            mix += jetTone + jetRush
          }

          // Braking tire screech at speed
          if (engineBrake && smoothedSpeed > 0.25f) {
            val screechFreq = 980f + sin(musicSampleCounter * 0.01f) * 70f
            val rawNoise = (rng.nextFloat() * 2f - 1f)
            brakeNoiseState = brakeNoiseState * 0.6f + rawNoise * 0.4f
            val screech =
              sin(TWO_PI * screechFreq * (musicSampleCounter % SAMPLE_RATE) / SAMPLE_RATE) * 0.09f +
                brakeNoiseState * 0.08f
            mix += screech * smoothedSpeed.coerceAtMost(1f)
          }
        }

        // 3. One-shot SFX Mixer
        if (sfxOn && activeSfx.isNotEmpty()) {
          val iter = activeSfx.iterator()
          while (iter.hasNext()) {
            val sfx = iter.next()
            val t = sfx.cursor.toFloat() / sfx.durationSamples
            mix += sampleSfx(sfx, sfx.cursor, t, rng)
            sfx.cursor++
            if (sfx.cursor >= sfx.durationSamples) {
              iter.remove()
            }
          }
        }

        val clamped = mix.coerceIn(-0.92f, 0.92f)
        buffer[i] = (clamped * 32767f).toInt().toShort()
      }

      try {
        track.write(buffer, 0, buffer.size)
      } catch (_: Exception) {
        break
      }
    }

    try {
      track.stop()
    } catch (_: Exception) {}
    track.release()
  }

  private fun sampleSfx(sfx: OneShotSfx, sampleIdx: Int, t: Float, rng: Random): Float {
    val timeSec = sampleIdx.toFloat() / SAMPLE_RATE
    return when (sfx) {
      is OneShotSfx.Click -> {
        val env = (1f - t) * (1f - t)
        sin(TWO_PI * (900f - t * 400f) * timeSec) * env * 0.28f
      }
      is OneShotSfx.Coin -> {
        val freq = if (t < 0.35f) 987.77f else 1318.51f
        val env = (1f - t).coerceIn(0f, 1f)
        sin(TWO_PI * freq * timeSec) * env * 0.32f
      }
      is OneShotSfx.NearMiss -> {
        val env = sin(t * PI.toFloat())
        val sweepFreq = 680f - t * 460f
        (sin(TWO_PI * sweepFreq * timeSec) * 0.16f + (rng.nextFloat() * 2f - 1f) * 0.12f) * env
      }
      is OneShotSfx.Countdown -> {
        val env = (1f - t).coerceIn(0f, 1f)
        val freq = if (sfx.isGo) 880f else 440f
        sin(TWO_PI * freq * timeSec) * env * 0.34f
      }
      is OneShotSfx.Crash -> {
        val env = (1f - t) * (1f - t)
        val thud = sin(TWO_PI * (120f * (1f - t * 0.6f)) * timeSec) * 0.55f
        val crunch = (rng.nextFloat() * 2f - 1f) * 0.45f
        (thud + crunch) * env
      }
      is OneShotSfx.Victory -> {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        val idx = (t * 4f).toInt().coerceIn(0, 3)
        val localT = (t * 4f) - idx
        val env = (1f - localT * 0.5f) * (1f - t * 0.3f)
        sin(TWO_PI * notes[idx] * timeSec) * env * 0.35f
      }
    }
  }
}
