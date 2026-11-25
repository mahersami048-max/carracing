package com.example.engine

import com.example.audio.SoundManager
import com.example.input.RaceInputState
import com.example.model.EnvironmentType
import com.example.model.GameMode
import com.example.model.MissionType
import com.example.model.RaceConfig
import com.example.model.RaceHudState
import com.example.model.RaceResult
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

data class TrafficVehicle(
  var active: Boolean = false,
  var kind: TrafficKind = TrafficKind.SEDAN,
  var lane: Int = 0,
  var x: Float = 0f,
  var targetX: Float = 0f,
  var relZ: Float = -100f, // negative = ahead of player, positive = behind
  var speedMps: Float = 24f,
  var colorR: Float = 0.8f,
  var colorG: Float = 0.2f,
  var colorB: Float = 0.2f,
  var wheelSpinDeg: Float = 0f,
  var overtaken: Boolean = false,
  var braking: Boolean = false,
  var laneChangeCooldown: Float = 4f,
)

data class RoadsideProp(
  var x: Float,
  var z: Float,
  var side: Int, // -1 left, +1 right
  var variant: Int,
  var scale: Float,
)

data class RainParticle(
  var x: Float,
  var y: Float,
  var z: Float,
  var speed: Float,
)

class RaceSimulation(
  val config: RaceConfig,
  private val input: RaceInputState,
  private val soundManager: SoundManager,
  private val onHudUpdate: (RaceHudState) -> Unit,
  private val onRaceFinished: (RaceResult) -> Unit,
) {
  companion object {
    val LANE_CENTERS = floatArrayOf(-4.5f, -1.5f, 1.5f, 4.5f)
    const val ROAD_LIMIT_X = 5.22f
    const val TIME_CHALLENGE_DIST = 3500f
    const val TIME_CHALLENGE_LIMIT = 65f
    const val TRAFFIC_CHALLENGE_TARGET = 30
  }

  private val rng = Random(System.currentTimeMillis())

  // Player dynamic state
  var playerX: Float = 1.5f
  var playerSpeedMps: Float = 0f
  var maxSpeedKmh: Float = 0f
  var distanceMeters: Float = 0f
  var scrollOffset: Float = 0f
  var wheelSpinDeg: Float = 0f
  var frontSteerDeg: Float = 0f
  var rollDeg: Float = 0f
  var pitchDeg: Float = 0f
  var bounceY: Float = 0f
  var lateralVel: Float = 0f
  var nitroEnergy: Float = 1f
  var isNitroActive: Boolean = false
  var isBraking: Boolean = false
  var cameraFovOffset: Float = 0f
  var cameraShake: Float = 0f

  // Race flow state
  var elapsedSeconds: Float = 0f
  var timeRemainingSeconds: Float = 0f
  var overtakes: Int = 0
  var coinsEarned: Int = 0
  private var countdownTimer: Float = 2.8f
  private var lastCountdownBeep: Int = 4
  var isPaused: Boolean = false
  var isCrashing: Boolean = false
  private var crashTimer: Float = 0f
  private var finishedReported: Boolean = false
  private var calloutText: String? = null
  private var calloutTimer: Float = 0f
  private var hudEmitAccum: Float = 0f
  private var spawnCooldown: Float = 0.25f

  // Pooled entities
  val trafficPool = Array(16) { TrafficVehicle() }
  val propPool = ArrayList<RoadsideProp>(48)
  val mountainPositions = FloatArray(10) { i -> -220f + (i / 2) * 58f }
  val lampPositions = FloatArray(14) { i -> 20f - i * 18f }
  val rainParticles = Array(90) {
    RainParticle(
      x = rng.nextFloat() * 18f - 9f,
      y = rng.nextFloat() * 9f + 0.5f,
      z = -rng.nextFloat() * 34f + 4f,
      speed = 18f + rng.nextFloat() * 8f,
    )
  }

  private val topSpeedMps: Float = config.upgradeState.effectiveTopSpeed(config.carSpec) / 3.6f
  private val accelPower: Float = config.upgradeState.effectiveAcceleration(config.carSpec)
  private val handlingPower: Float = config.upgradeState.effectiveHandling(config.carSpec)
  private val nitroPower: Float = config.upgradeState.effectiveNitroPower(config.carSpec)

  val targetDistanceMeters: Float =
    when (config.mode) {
      GameMode.CAREER -> config.careerLevel?.targetDistanceMeters ?: 2000f
      GameMode.TIME_CHALLENGE -> TIME_CHALLENGE_DIST
      else -> 0f
    }

  val targetOvertakes: Int =
    when (config.mode) {
      GameMode.CAREER -> config.careerLevel?.targetOvertakes ?: 0
      GameMode.TRAFFIC_CHALLENGE -> TRAFFIC_CHALLENGE_TARGET
      else -> 0
    }

  private val trafficColors =
    listOf(
      floatArrayOf(0.85f, 0.88f, 0.92f),
      floatArrayOf(0.20f, 0.24f, 0.30f),
      floatArrayOf(0.16f, 0.42f, 0.85f),
      floatArrayOf(0.84f, 0.20f, 0.22f),
      floatArrayOf(0.92f, 0.68f, 0.14f),
      floatArrayOf(0.18f, 0.64f, 0.40f),
      floatArrayOf(0.55f, 0.58f, 0.64f),
    )

  init {
    timeRemainingSeconds =
      when (config.mode) {
        GameMode.CAREER -> config.careerLevel?.timeLimitSeconds ?: 0f
        GameMode.TIME_CHALLENGE -> TIME_CHALLENGE_LIMIT
        else -> 0f
      }
    initProps()
    seedInitialTraffic()
  }

  private fun initProps() {
    propPool.clear()
    for (i in 0 until 44) {
      val side = if (i % 2 == 0) -1 else 1
      val z = 24f - (i / 2) * 11f
      val distFromRoad =
        if (config.environment == EnvironmentType.CITY) {
          14.5f + rng.nextFloat() * 4f
        } else {
          9.5f + rng.nextFloat() * 14f
        }
      propPool.add(
        RoadsideProp(
          x = side * distFromRoad,
          z = z,
          side = side,
          variant = rng.nextInt(4),
          scale = 0.85f + rng.nextFloat() * 0.55f,
        )
      )
    }
  }

  private fun seedInitialTraffic() {
    for (i in 0 until 7) {
      val z = -48f - i * 24f
      spawnTrafficAt(z)
    }
  }

  fun step(rawDt: Float) {
    if (isPaused || finishedReported) {
      soundManager.engineActive = false
      return
    }

    val dt = rawDt.coerceIn(0.001f, 0.05f)

    // Handle Crash sequence before ending race
    if (isCrashing) {
      soundManager.engineActive = false
      crashTimer -= dt
      playerSpeedMps = (playerSpeedMps - 28f * dt).coerceAtLeast(0f)
      scrollOffset += playerSpeedMps * dt
      rollDeg += 260f * dt
      bounceY = abs(sin(crashTimer * 18f)) * 0.35f
      cameraShake = (crashTimer * 0.7f).coerceAtLeast(0f)
      if (crashTimer <= 0f) {
        finishRace(
          isVictory = false,
          failureReason = "Vehicle Crashed!",
        )
      }
      emitHud(force = true)
      return
    }

    // Countdown 3-2-1-GO
    if (countdownTimer > 0f) {
      countdownTimer -= dt
      val count = (countdownTimer + 0.99f).toInt().coerceIn(0, 3)
      if (count < lastCountdownBeep && count >= 1) {
        lastCountdownBeep = count
        soundManager.playCountdown(isGo = false)
      }
      if (countdownTimer <= 0f) {
        soundManager.playCountdown(isGo = true)
        playerSpeedMps = 14f // Snappy launch off the line
        showCallout("GO!")
      }
      soundManager.engineActive = true
      soundManager.engineSpeedNorm = if (input.accelerate) 0.45f else 0.12f
      soundManager.engineThrottle = input.accelerate
      soundManager.engineBrake = false
      soundManager.engineNitro = false
      emitHud(force = false)
      return
    }

    elapsedSeconds += dt
    if (timeRemainingSeconds > 0f) {
      timeRemainingSeconds = (timeRemainingSeconds - dt).coerceAtLeast(0f)
    }

    if (calloutTimer > 0f) {
      calloutTimer -= dt
      if (calloutTimer <= 0f) calloutText = null
    }
    cameraShake *= 0.86f

    // 1. Player longitudinal & nitro physics
    val wantNitro = input.nitro && nitroEnergy > 0.05f && !input.brake
    isNitroActive = wantNitro
    isBraking = input.brake && playerSpeedMps > 2f

    if (isNitroActive) {
      nitroEnergy = (nitroEnergy - dt * (0.28f / nitroPower)).coerceAtLeast(0f)
    } else {
      nitroEnergy = (nitroEnergy + dt * (0.075f * nitroPower)).coerceAtMost(1f)
    }

    val effectiveMaxSpeed =
      if (isNitroActive) topSpeedMps * (1.26f + (nitroPower - 0.7f) * 0.18f) else topSpeedMps

    val wetGripPenalty = if (config.environment == EnvironmentType.RAINY) 0.90f else 1.0f

    val targetAccel =
      when {
        isBraking -> -34f * wetGripPenalty
        isNitroActive -> 28f * accelPower * nitroPower
        input.accelerate -> 17.5f * accelPower * (1f - (playerSpeedMps / (effectiveMaxSpeed * 1.15f)) * 0.55f)
        else -> -4.2f - playerSpeedMps * 0.08f // Natural drag & rolling friction
      }

    playerSpeedMps = (playerSpeedMps + targetAccel * dt).coerceIn(0f, effectiveMaxSpeed)

    // Smooth camera FOV kick during Nitro
    val targetFovBoost = if (isNitroActive) 9.5f else (playerSpeedMps / topSpeedMps) * 3.5f
    cameraFovOffset += (targetFovBoost - cameraFovOffset) * (dt * 6f).coerceAtMost(1f)

    // 2. Steering & suspension physics
    val steerInput = input.combinedSteer()
    val speedFactor = (playerSpeedMps / 12f).coerceIn(0.25f, 1.15f)
    val steerRate = 9.2f * handlingPower * wetGripPenalty * speedFactor
    val targetLatVel = steerInput * steerRate
    lateralVel += (targetLatVel - lateralVel) * (dt * 12f).coerceAtMost(1f)
    playerX += lateralVel * dt

    // Guardrail contact
    if (abs(playerX) > ROAD_LIMIT_X) {
      val sign = if (playerX > 0f) 1f else -1f
      playerX = sign * ROAD_LIMIT_X
      lateralVel = -sign * 1.8f
      playerSpeedMps *= 0.985f
      cameraShake = 0.16f
      bounceY = 0.06f
    } else {
      bounceY *= 0.84f
    }

    // Visual car roll, pitch, and wheel spin
    val targetRoll = -lateralVel * 0.95f
    rollDeg += (targetRoll - rollDeg) * (dt * 10f).coerceAtMost(1f)
    val targetPitch =
      when {
        isBraking -> -3.2f
        isNitroActive -> 2.8f
        input.accelerate -> 1.4f
        else -> 0f
      }
    pitchDeg += (targetPitch - pitchDeg) * (dt * 8f).coerceAtMost(1f)
    frontSteerDeg += (-steerInput * 22f - frontSteerDeg) * (dt * 14f).coerceAtMost(1f)
    wheelSpinDeg = (wheelSpinDeg + playerSpeedMps * dt * 115f) % 360f

    // 3. Distance & stats
    val deltaDist = playerSpeedMps * dt
    distanceMeters += deltaDist
    scrollOffset = (scrollOffset + deltaDist) % 4000f

    val speedKmh = playerSpeedMps * 3.6f
    if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh

    // Feed live sound synthesizer
    soundManager.engineActive = true
    soundManager.engineSpeedNorm = (playerSpeedMps / topSpeedMps).coerceIn(0f, 1.35f)
    soundManager.engineThrottle = input.accelerate
    soundManager.engineBrake = isBraking
    soundManager.engineNitro = isNitroActive

    // 4. Environment prop scrolling
    stepEnvironmentProps(deltaDist, dt)

    // 5. AI Traffic update, spawning, overtakes & collisions
    stepTraffic(dt)

    // 6. Check win/loss conditions
    evaluateObjectives()

    // 7. Throttled HUD update (~30Hz for Compose)
    hudEmitAccum += dt
    if (hudEmitAccum >= 0.033f) {
      hudEmitAccum = 0f
      emitHud(force = false)
    }
  }

  private fun stepEnvironmentProps(deltaDist: Float, dt: Float) {
    for (prop in propPool) {
      prop.z += deltaDist
      if (prop.z > 28f) {
        prop.z -= 242f
        prop.variant = rng.nextInt(4)
        prop.scale = 0.85f + rng.nextFloat() * 0.55f
      }
    }
    for (i in lampPositions.indices) {
      lampPositions[i] += deltaDist
      if (lampPositions[i] > 28f) {
        lampPositions[i] -= lampPositions.size * 18f
      }
    }
    for (i in mountainPositions.indices) {
      mountainPositions[i] += deltaDist * 0.35f // Distant parallax
      if (mountainPositions[i] > 50f) {
        mountainPositions[i] -= 290f
      }
    }
    if (config.environment.rainIntensity > 0f) {
      for (drop in rainParticles) {
        drop.y -= drop.speed * dt
        drop.z += playerSpeedMps * 0.45f * dt
        if (drop.y < 0f || drop.z > 6f) {
          drop.x = playerX + rng.nextFloat() * 18f - 9f
          drop.y = 7.5f + rng.nextFloat() * 3f
          drop.z = -rng.nextFloat() * 34f
        }
      }
    }
  }

  private fun stepTraffic(dt: Float) {
    val density =
      when (config.mode) {
        GameMode.CAREER -> config.careerLevel?.trafficDensity ?: 0.6f
        GameMode.TRAFFIC_CHALLENGE -> 0.88f
        GameMode.ENDLESS -> (0.50f + (distanceMeters / 6000f) * 0.45f).coerceAtMost(0.95f)
        GameMode.TIME_CHALLENGE -> 0.65f
      }

    spawnCooldown -= dt
    if (spawnCooldown <= 0f) {
      spawnTrafficAt(-175f - rng.nextFloat() * 25f)
      spawnCooldown = (1.15f - density * 0.72f).coerceIn(0.32f, 1.15f)
    }

    val playerHalfW = config.carSpec.halfWidth
    val playerHalfL = config.carSpec.halfLength

    for (tv in trafficPool) {
      if (!tv.active) continue

      // Advance relative Z
      tv.relZ += (playerSpeedMps - tv.speedMps) * dt
      tv.wheelSpinDeg = (tv.wheelSpinDeg + tv.speedMps * dt * 110f) % 360f

      // Smooth lane change AI
      tv.laneChangeCooldown -= dt
      if (tv.laneChangeCooldown <= 0f && tv.relZ < -32f && tv.relZ > -140f) {
        tv.laneChangeCooldown = 3.5f + rng.nextFloat() * 4f
        if (rng.nextFloat() < 0.35f) {
          val nextLane = (tv.lane + if (rng.nextBoolean()) 1 else -1).coerceIn(0, 3)
          if (isLaneClearAt(nextLane, tv.relZ, 20f)) {
            tv.lane = nextLane
            tv.targetX = LANE_CENTERS[nextLane]
          }
        }
      }
      tv.x += (tv.targetX - tv.x) * (dt * 4.5f).coerceAtMost(1f)

      // Overtake & Near-miss detection
      if (!tv.overtaken && tv.relZ > playerHalfL + tv.kind.halfLength * 0.4f) {
        tv.overtaken = true
        overtakes++
        val lateralGap = abs(playerX - tv.x) - (playerHalfW + tv.kind.halfWidth)
        if (lateralGap < 1.15f && playerSpeedMps > 24f) {
          coinsEarned += 25
          nitroEnergy = (nitroEnergy + 0.14f).coerceAtMost(1f)
          showCallout("NEAR MISS! +25")
          soundManager.playNearMiss()
        } else {
          coinsEarned += 10
        }
      }

      // AABB collision check
      val overlapX = abs(playerX - tv.x) < (playerHalfW + tv.kind.halfWidth - 0.18f)
      val overlapZ = abs(tv.relZ) < (playerHalfL + tv.kind.halfLength - 0.22f)
      if (overlapX && overlapZ) {
        triggerCrash()
        return
      }

      if (tv.relZ > 24f) {
        tv.active = false
      }
    }
  }

  private fun isLaneClearAt(lane: Int, z: Float, minGap: Float): Boolean {
    for (tv in trafficPool) {
      if (tv.active && tv.lane == lane && abs(tv.relZ - z) < minGap) {
        return false
      }
    }
    return true
  }

  private fun spawnTrafficAt(zPos: Float) {
    val slot = trafficPool.firstOrNull { !it.active } ?: return
    val lanesShuffled = listOf(0, 1, 2, 3).shuffled(rng)

    // Ensure at least one lane remains passable in this Z window
    val occupiedCount =
      (0..3).count { lane ->
        trafficPool.any { it.active && it.lane == lane && abs(it.relZ - zPos) < 22f }
      }
    if (occupiedCount >= 3) return

    val chosenLane = lanesShuffled.firstOrNull { isLaneClearAt(it, zPos, 24f) } ?: return

    val kind =
      when {
        chosenLane == 0 || chosenLane == 3 -> {
          // Outer lanes can have trucks, buses, vans, SUVs, sedans
          val roll = rng.nextFloat()
          when {
            roll < 0.22f -> TrafficKind.TRUCK
            roll < 0.36f -> TrafficKind.BUS
            roll < 0.54f -> TrafficKind.VAN
            roll < 0.76f -> TrafficKind.SUV
            else -> TrafficKind.SEDAN
          }
        }
        else -> {
          // Inner fast lanes: sedans, SUVs, vans
          val roll = rng.nextFloat()
          when {
            roll < 0.55f -> TrafficKind.SEDAN
            roll < 0.85f -> TrafficKind.SUV
            else -> TrafficKind.VAN
          }
        }
      }

    val speedFactor = config.careerLevel?.trafficSpeedFactor ?: 0.92f
    val minS = kind.speedRangeMps.start
    val maxS = kind.speedRangeMps.endInclusive
    val laneBias = if (chosenLane == 1 || chosenLane == 2) 1.10f else 0.92f
    val col = trafficColors[rng.nextInt(trafficColors.size)]

    slot.apply {
      active = true
      this.kind = kind
      lane = chosenLane
      x = LANE_CENTERS[chosenLane]
      targetX = x
      relZ = zPos
      speedMps = (minS + rng.nextFloat() * (maxS - minS)) * speedFactor * laneBias
      colorR = col[0]
      colorG = col[1]
      colorB = col[2]
      wheelSpinDeg = 0f
      overtaken = false
      braking = false
      laneChangeCooldown = 3f + rng.nextFloat() * 4f
    }
  }

  private fun triggerCrash() {
    isCrashing = true
    crashTimer = 1.05f
    cameraShake = 0.75f
    soundManager.playCrash()
    showCallout("CRASH!")
  }

  private fun evaluateObjectives() {
    when (config.mode) {
      GameMode.CAREER -> {
        val level = config.careerLevel ?: return
        if (level.missionType == MissionType.TIME_TRIAL && timeRemainingSeconds <= 0f) {
          finishRace(isVictory = false, failureReason = "Time Limit Expired!")
          return
        }
        if (distanceMeters >= level.targetDistanceMeters) {
          when (level.missionType) {
            MissionType.REACH_FINISH -> finishRace(isVictory = true)
            MissionType.TIME_TRIAL -> finishRace(isVictory = true)
            MissionType.OVERTAKE_COUNT -> {
              if (overtakes >= level.targetOvertakes) {
                finishRace(isVictory = true)
              } else {
                finishRace(
                  isVictory = false,
                  failureReason = "Overtakes: $overtakes / ${level.targetOvertakes}",
                )
              }
            }
            MissionType.HIGH_SPEED_RUN -> {
              if (maxSpeedKmh >= level.targetMinAvgSpeedKmh) {
                finishRace(isVictory = true)
              } else {
                finishRace(
                  isVictory = false,
                  failureReason = "Top speed ${maxSpeedKmh.toInt()} < ${level.targetMinAvgSpeedKmh.toInt()} km/h",
                )
              }
            }
          }
        }
      }
      GameMode.TIME_CHALLENGE -> {
        if (timeRemainingSeconds <= 0f) {
          finishRace(isVictory = false, failureReason = "Time Expired Before Checkpoint!")
        } else if (distanceMeters >= TIME_CHALLENGE_DIST) {
          finishRace(isVictory = true)
        }
      }
      GameMode.TRAFFIC_CHALLENGE -> {
        if (overtakes >= TRAFFIC_CHALLENGE_TARGET) {
          finishRace(isVictory = true)
        }
      }
      GameMode.ENDLESS -> Unit // Runs until crash
    }
  }

  private fun finishRace(isVictory: Boolean, failureReason: String? = null) {
    if (finishedReported) return
    finishedReported = true
    soundManager.engineActive = false

    val distanceCoins = (distanceMeters / 16f).toInt()
    val levelBonus = if (isVictory) (config.careerLevel?.rewardCoins ?: 350) else 0
    val totalCoins = (coinsEarned + distanceCoins + levelBonus).coerceAtLeast(if (isVictory) 200 else 25)

    val stars =
      if (!isVictory) {
        0
      } else {
        when (config.mode) {
          GameMode.CAREER -> {
            val lvl = config.careerLevel
            val speedRatio = maxSpeedKmh / (config.carSpec.baseTopSpeedKmh * 0.92f)
            when {
              speedRatio >= 0.98f || overtakes >= (lvl?.targetOvertakes ?: 8) + 6 -> 3
              speedRatio >= 0.82f -> 2
              else -> 1
            }
          }
          GameMode.TIME_CHALLENGE ->
            when {
              timeRemainingSeconds >= 14f -> 3
              timeRemainingSeconds >= 6f -> 2
              else -> 1
            }
          else -> 3
        }
      }

    if (isVictory) {
      soundManager.playVictory()
    }

    onRaceFinished(
      RaceResult(
        isVictory = isVictory,
        mode = config.mode,
        environment = config.environment,
        careerLevelNumber = config.careerLevel?.levelNumber,
        distanceMeters = distanceMeters,
        maxSpeedKmh = maxSpeedKmh,
        overtakes = overtakes,
        elapsedSeconds = elapsedSeconds,
        coinsEarned = totalCoins,
        starsEarned = stars,
        failureReason = failureReason,
      )
    )
  }

  private fun showCallout(text: String) {
    calloutText = text
    calloutTimer = 1.35f
  }

  private fun emitHud(force: Boolean) {
    val progress =
      when (config.mode) {
        GameMode.CAREER -> {
          val lvl = config.careerLevel
          when (lvl?.missionType) {
            MissionType.OVERTAKE_COUNT -> {
              val distP = (distanceMeters / lvl.targetDistanceMeters).coerceIn(0f, 1f)
              val ovP = (overtakes.toFloat() / lvl.targetOvertakes.coerceAtLeast(1)).coerceIn(0f, 1f)
              (distP * 0.5f + ovP * 0.5f)
            }
            else -> (distanceMeters / targetDistanceMeters.coerceAtLeast(1f)).coerceIn(0f, 1f)
          }
        }
        GameMode.TIME_CHALLENGE -> (distanceMeters / TIME_CHALLENGE_DIST).coerceIn(0f, 1f)
        GameMode.TRAFFIC_CHALLENGE -> (overtakes.toFloat() / TRAFFIC_CHALLENGE_TARGET).coerceIn(0f, 1f)
        GameMode.ENDLESS -> ((distanceMeters % 1000f) / 1000f)
      }

    val summary =
      when (config.mode) {
        GameMode.CAREER -> {
          val lvl = config.careerLevel
          when (lvl?.missionType) {
            MissionType.REACH_FINISH -> "FINISH LINE"
            MissionType.TIME_TRIAL -> "TIME TRIAL"
            MissionType.OVERTAKE_COUNT -> "OVERTAKE $overtakes/${lvl.targetOvertakes}"
            MissionType.HIGH_SPEED_RUN -> "TARGET ${lvl.targetMinAvgSpeedKmh.toInt()} KM/H"
            null -> ""
          }
        }
        GameMode.ENDLESS -> "ENDLESS RUN"
        GameMode.TIME_CHALLENGE -> "CHECKPOINT"
        GameMode.TRAFFIC_CHALLENGE -> "OVERTAKE $overtakes/$TRAFFIC_CHALLENGE_TARGET"
      }

    val countdown =
      if (countdownTimer > 0f) (countdownTimer + 0.99f).toInt().coerceIn(1, 3) else 0

    onHudUpdate(
      RaceHudState(
        speedKmh = playerSpeedMps * 3.6f,
        maxSpeedKmh = maxSpeedKmh,
        distanceMeters = distanceMeters,
        targetDistanceMeters = targetDistanceMeters,
        elapsedSeconds = elapsedSeconds,
        timeRemainingSeconds = timeRemainingSeconds,
        overtakes = overtakes,
        targetOvertakes = targetOvertakes,
        nitroEnergy = nitroEnergy,
        isNitroActive = isNitroActive,
        isBraking = isBraking,
        coinsEarned = coinsEarned + (distanceMeters / 16f).toInt(),
        calloutText = calloutText,
        countdownValue = countdown,
        isPaused = isPaused,
        isCrashing = isCrashing,
        progressFraction = progress,
        missionSummary = summary,
      )
    )
  }
}
