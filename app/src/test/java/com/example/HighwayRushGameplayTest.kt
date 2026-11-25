package com.example

import com.example.audio.SoundManager
import com.example.engine.RaceSimulation
import com.example.input.RaceInputState
import com.example.model.CarCatalog
import com.example.model.CarUpgradeState
import com.example.model.CareerCatalog
import com.example.model.EnvironmentType
import com.example.model.GameMode
import com.example.model.GraphicsQuality
import com.example.model.RaceConfig
import com.example.model.RaceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighwayRushGameplayTest {

  @Test
  fun `catalog has 6 cars with free starter and 24 career levels across 6 environments`() {
    assertEquals(6, CarCatalog.cars.size)
    assertEquals(0, CarCatalog.findById("apex_gt").price)
    assertEquals(24, CareerCatalog.levels.size)
    assertEquals(6, EnvironmentType.entries.size)

    val usedEnvs = CareerCatalog.levels.map { it.environment }.toSet()
    assertEquals(EnvironmentType.entries.toSet(), usedEnvs)
  }

  @Test
  fun `car upgrades increase top speed acceleration handling and nitro`() {
    val car = CarCatalog.findById("apex_gt")
    val base = CarUpgradeState(engineLevel = 1, handlingLevel = 1, nitroLevel = 1)
    val maxed = CarUpgradeState(engineLevel = 5, handlingLevel = 5, nitroLevel = 5)

    assertTrue(maxed.effectiveTopSpeed(car) > base.effectiveTopSpeed(car))
    assertTrue(maxed.effectiveAcceleration(car) > base.effectiveAcceleration(car))
    assertTrue(maxed.effectiveHandling(car) > base.effectiveHandling(car))
    assertTrue(maxed.effectiveNitroPower(car) > base.effectiveNitroPower(car))
  }

  @Test
  fun `simulation accelerates steers brakes uses nitro and completes career level`() {
    val input = RaceInputState()
    val sound = SoundManager().apply {
      soundEnabled = false
      musicEnabled = false
    }
    var result: RaceResult? = null
    val level1 = CareerCatalog.findLevel(1)
    val car = CarCatalog.findById("apex_gt")

    val sim =
      RaceSimulation(
        config =
          RaceConfig(
            mode = GameMode.CAREER,
            environment = level1.environment,
            careerLevel = level1,
            carSpec = car,
            upgradeState = CarUpgradeState(),
            graphicsQuality = GraphicsQuality.MEDIUM,
          ),
        input = input,
        soundManager = sound,
        onHudUpdate = {},
        onRaceFinished = { result = it },
      )

    // Clear 2.8s countdown
    repeat(70) { sim.step(0.05f) }

    // Accelerate & steer left
    input.accelerate = true
    input.steerLeft = true
    val startX = sim.playerX
    repeat(30) { sim.step(0.04f) }
    assertTrue("Car should accelerate forward", sim.playerSpeedMps > 20f)
    assertTrue("Car should steer left", sim.playerX < startX)
    assertTrue("Distance should accumulate", sim.distanceMeters > 10f)

    // Activate nitro
    input.steerLeft = false
    input.nitro = true
    val nitroBefore = sim.nitroEnergy
    repeat(20) { sim.step(0.04f) }
    assertTrue("Nitro energy should drain when boosting", sim.nitroEnergy < nitroBefore)

    // Brake decelerates
    input.nitro = false
    input.accelerate = false
    input.brake = true
    val speedBeforeBrake = sim.playerSpeedMps
    repeat(15) { sim.step(0.04f) }
    assertTrue("Braking should slow car down", sim.playerSpeedMps < speedBeforeBrake)
    input.brake = false

    // Advance cleanly past finish line (avoid traffic by staying clear or clearing pool)
    sim.trafficPool.forEach { it.active = false }
    sim.distanceMeters = level1.targetDistanceMeters - 5f
    input.accelerate = true
    repeat(25) { sim.step(0.04f) }

    val finalResult = result ?: error("Finishing career level should emit RaceResult")
    assertTrue("Level 1 should complete as victory", finalResult.isVictory)
    assertTrue("Victory should award stars", finalResult.starsEarned >= 1)
    assertTrue("Victory should award coins", finalResult.coinsEarned >= level1.rewardCoins)
  }

  @Test
  fun `direct traffic collision triggers crash sequence and ends run`() {
    val input = RaceInputState()
    val sound = SoundManager().apply {
      soundEnabled = false
      musicEnabled = false
    }
    var result: RaceResult? = null
    val car = CarCatalog.findById("apex_gt")

    val sim =
      RaceSimulation(
        config =
          RaceConfig(
            mode = GameMode.ENDLESS,
            environment = EnvironmentType.NIGHT,
            careerLevel = null,
            carSpec = car,
            upgradeState = CarUpgradeState(),
            graphicsQuality = GraphicsQuality.HIGH,
          ),
        input = input,
        soundManager = sound,
        onHudUpdate = {},
        onRaceFinished = { result = it },
      )

    repeat(70) { sim.step(0.05f) }
    input.accelerate = true
    repeat(15) { sim.step(0.04f) }

    // Force a traffic vehicle directly in front of player
    val tv = sim.trafficPool.first()
    tv.active = true
    tv.x = sim.playerX
    tv.targetX = sim.playerX
    tv.relZ = -0.5f

    sim.step(0.02f)
    assertTrue("Collision should trigger crash state", sim.isCrashing)

    // Step through 1.1s crash sequence
    repeat(30) { sim.step(0.05f) }
    assertNotNull("Crash should finish Endless run", result)
    assertFalse("Crash result should not be marked victory", result!!.isVictory)
  }
}
