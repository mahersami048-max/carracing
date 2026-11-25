package com.example.engine

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.model.CarType
import com.example.model.EnvironmentType
import com.example.model.GraphicsQuality
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class RaceGlRenderer : GLSurfaceView.Renderer {
  enum class RenderMode {
    SHOWROOM,
    RACING,
  }

  @Volatile var renderMode: RenderMode = RenderMode.SHOWROOM
  @Volatile var showroomCarType: CarType = CarType.SPORTS
  @Volatile var showroomColorHex: Int = 0xFFE62038.toInt()
  @Volatile var showroomEnvironment: EnvironmentType = EnvironmentType.NIGHT
  @Volatile var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
  @Volatile var activeSimulation: RaceSimulation? = null

  // Interactive rotation in Garage / Showroom
  @Volatile var showroomYawDeg: Float = 28f
  @Volatile var showroomAutoRotate: Boolean = true

  private lateinit var shader: ShaderProgram
  private lateinit var skyDome: Mesh
  private lateinit var roadMesh: Mesh
  private lateinit var groundMesh: Mesh
  private lateinit var guardrailMesh: Mesh
  private lateinit var pineMesh: Mesh
  private lateinit var broadleafMesh: Mesh
  private lateinit var cactusMesh: Mesh
  private lateinit var cityDayBldgMesh: Mesh
  private lateinit var cityNightBldgMesh: Mesh
  private lateinit var mountainSnowMesh: Mesh
  private lateinit var mountainPlainMesh: Mesh
  private lateinit var streetLampPole: Mesh
  private lateinit var streetLampBulb: Mesh
  private lateinit var gantryStructure: Mesh
  private lateinit var gantryBanner: Mesh
  private lateinit var nitroFlameMesh: Mesh
  private lateinit var speedStreakMesh: Mesh
  private lateinit var rainDropMesh: Mesh
  private lateinit var pedestalMesh: Mesh

  private val playerMeshes = mutableMapOf<CarType, CarMeshBundle>()
  private val trafficMeshes = mutableMapOf<TrafficKind, CarMeshBundle>()

  // Pre-allocated matrices to avoid GC churn on render thread
  private val projMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)
  private val childModel = FloatArray(16)

  private var aspectRatio = 16f / 9f
  private var lastTimeNs = 0L
  private var totalTimeSec = 0f

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)

    shader = ShaderProgram()
    skyDome = EnvironmentMeshFactory.buildSkyDome()
    roadMesh = EnvironmentMeshFactory.buildRoadSurface()
    groundMesh = EnvironmentMeshFactory.buildGroundTerrain()
    guardrailMesh = EnvironmentMeshFactory.buildGuardrailSegment()
    pineMesh = EnvironmentMeshFactory.buildPineTree()
    broadleafMesh = EnvironmentMeshFactory.buildBroadleafTree()
    cactusMesh = EnvironmentMeshFactory.buildDesertCactus()
    cityDayBldgMesh = EnvironmentMeshFactory.buildCityBuilding(24f, isNight = false)
    cityNightBldgMesh = EnvironmentMeshFactory.buildCityBuilding(24f, isNight = true)
    mountainSnowMesh = EnvironmentMeshFactory.buildMountainRidge(snowCap = true)
    mountainPlainMesh = EnvironmentMeshFactory.buildMountainRidge(snowCap = false)
    val (pole, bulb) = EnvironmentMeshFactory.buildStreetLamp()
    streetLampPole = pole
    streetLampBulb = bulb
    val (gStruct, gBanner) = EnvironmentMeshFactory.buildFinishGantry()
    gantryStructure = gStruct
    gantryBanner = gBanner
    nitroFlameMesh = VehicleMeshFactory.buildNitroFlameMesh()
    speedStreakMesh = EnvironmentMeshFactory.buildStreakMesh()
    rainDropMesh = EnvironmentMeshFactory.buildRainDropMesh()
    pedestalMesh = EnvironmentMeshFactory.buildShowroomPedestal()

    CarType.entries.forEach { playerMeshes[it] = VehicleMeshFactory.buildPlayerCar(it) }
    TrafficKind.entries.forEach { trafficMeshes[it] = VehicleMeshFactory.buildTrafficVehicle(it) }

    lastTimeNs = System.nanoTime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f
  }

  override fun onDrawFrame(gl: GL10?) {
    val now = System.nanoTime()
    val dt = ((now - lastTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
    lastTimeNs = now
    totalTimeSec += dt

    val sim = activeSimulation
    if (renderMode == RenderMode.RACING && sim != null) {
      sim.step(dt)
      drawRaceScene(sim)
    } else {
      if (showroomAutoRotate) {
        showroomYawDeg = (showroomYawDeg + dt * 22f) % 360f
      }
      drawShowroomScene()
    }
  }

  private fun applyEnvironmentUniforms(
    env: EnvironmentType,
    camX: Float,
    camY: Float,
    camZ: Float,
    scrollOffset: Float,
    playerX: Float,
  ) {
    val sun = GlMath.normalize(env.sunDir)
    GLES20.glClearColor(env.fogRgb[0], env.fogRgb[1], env.fogRgb[2], 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    shader.use()
    GLES20.glUniform3f(shader.uCameraPos, camX, camY, camZ)
    GLES20.glUniform3f(shader.uLightDir, sun[0], sun[1], sun[2])
    GLES20.glUniform3f(shader.uLightColor, env.sunColorRgb[0], env.sunColorRgb[1], env.sunColorRgb[2])
    GLES20.glUniform3f(shader.uAmbientColor, env.ambientRgb[0], env.ambientRgb[1], env.ambientRgb[2])
    GLES20.glUniform3f(shader.uFogColor, env.fogRgb[0], env.fogRgb[1], env.fogRgb[2])
    GLES20.glUniform1f(shader.uFogDensity, env.fogDensity * graphicsQuality.fogScale)
    GLES20.glUniform3f(shader.uSkyTop, env.skyTopRgb[0], env.skyTopRgb[1], env.skyTopRgb[2])
    GLES20.glUniform3f(shader.uSkyHorizon, env.skyHorizonRgb[0], env.skyHorizonRgb[1], env.skyHorizonRgb[2])
    GLES20.glUniform1f(shader.uIsNight, if (env.isNight) 1f else 0f)
    GLES20.glUniform1f(shader.uWetness, if (graphicsQuality == GraphicsQuality.LOW) 0f else env.wetness)
    GLES20.glUniform1f(shader.uScrollOffset, scrollOffset)
    GLES20.glUniform1f(shader.uTime, totalTimeSec)
    GLES20.glUniform3f(shader.uPlayerPos, playerX, 0f, 0f)
  }

  private fun drawShowroomScene() {
    val env = EnvironmentType.NIGHT
    val camX = 0f
    val camY = 1.95f
    val camZ = 5.6f
    applyEnvironmentUniforms(env, camX, camY, camZ, totalTimeSec * 4f, 0f)

    GlMath.perspective(projMatrix, 46f, aspectRatio, 0.5f, 340f)
    GlMath.lookAt(viewMatrix, camX, camY, camZ, -0.35f, 0.72f, 0f, 0f, 1f, 0f)
    GlMath.multiply(vpMatrix, projMatrix, viewMatrix)

    // Sky dome
    GLES20.glDisable(GLES20.GL_CULL_FACE)
    GLES20.glUniform1i(shader.uShaderMode, 2)
    GlMath.setIdentity(modelMatrix)
    GlMath.translate(modelMatrix, camX, -4f, camZ)
    drawMesh(skyDome, modelMatrix, useTint = false, spec = 0f, shin = 8f)
    GLES20.glEnable(GLES20.GL_CULL_FACE)

    // Pedestal & highway floor
    GLES20.glUniform1i(shader.uShaderMode, 1)
    GlMath.setIdentity(modelMatrix)
    GlMath.translate(modelMatrix, 0f, -0.22f, -15f)
    drawMesh(
      roadMesh,
      modelMatrix,
      useTint = true,
      tintR = env.roadTintRgb[0],
      tintG = env.roadTintRgb[1],
      tintB = env.roadTintRgb[2],
      spec = 0.25f,
      shin = 28f,
    )

    GLES20.glUniform1i(shader.uShaderMode, 0)
    GlMath.setIdentity(modelMatrix)
    drawMesh(pedestalMesh, modelMatrix, useTint = false, spec = 0.55f, shin = 48f)

    // Selected player car rotating on turntable
    val bundle = playerMeshes[showroomCarType] ?: return
    GlMath.setIdentity(modelMatrix)
    GlMath.rotateY(modelMatrix, showroomYawDeg)
    drawCarBundle(
      bundle = bundle,
      baseModel = modelMatrix,
      colorHex = showroomColorHex,
      wheelSpinDeg = 0f,
      frontSteerDeg = 12f,
      headlightsOn = true,
      braking = false,
      nitroActive = false,
    )
  }

  private fun drawRaceScene(sim: RaceSimulation) {
    val env = sim.config.environment
    val shakeX = if (sim.cameraShake > 0.01f) sin(totalTimeSec * 48f) * sim.cameraShake else 0f
    val shakeY = if (sim.cameraShake > 0.01f) cos(totalTimeSec * 56f) * sim.cameraShake * 0.6f else 0f

    val camX = sim.playerX * 0.84f + shakeX
    val camY = 2.68f + sim.bounceY * 0.4f + shakeY
    val camZ = 6.25f
    applyEnvironmentUniforms(env, camX, camY, camZ, sim.scrollOffset, sim.playerX)

    val fov = 56f + sim.cameraFovOffset
    GlMath.perspective(projMatrix, fov, aspectRatio, 0.4f, 340f)
    GlMath.lookAt(
      viewMatrix,
      camX,
      camY,
      camZ,
      sim.playerX * 0.92f,
      0.95f,
      -20f,
      0f,
      1f,
      0f,
    )
    GlMath.multiply(vpMatrix, projMatrix, viewMatrix)

    // 1. Sky Dome
    GLES20.glDisable(GLES20.GL_CULL_FACE)
    GLES20.glUniform1i(shader.uShaderMode, 2)
    GlMath.setIdentity(modelMatrix)
    GlMath.translate(modelMatrix, camX, -6f, camZ)
    drawMesh(skyDome, modelMatrix, useTint = false, spec = 0f, shin = 4f)
    GLES20.glEnable(GLES20.GL_CULL_FACE)

    // 2. Ground terrain & procedural 4-lane highway
    GLES20.glUniform1i(shader.uShaderMode, 0)
    GlMath.setIdentity(modelMatrix)
    drawMesh(
      groundMesh,
      modelMatrix,
      useTint = true,
      tintR = env.terrainRgb[0],
      tintG = env.terrainRgb[1],
      tintB = env.terrainRgb[2],
      spec = 0.04f,
      shin = 4f,
    )

    GLES20.glUniform1i(shader.uShaderMode, 1)
    GlMath.setIdentity(modelMatrix)
    drawMesh(
      roadMesh,
      modelMatrix,
      useTint = true,
      tintR = env.roadTintRgb[0],
      tintG = env.roadTintRgb[1],
      tintB = env.roadTintRgb[2],
      spec = 0.14f,
      shin = 24f,
    )

    // 3. Guardrails scrolling seamlessly in 16m blocks
    GLES20.glUniform1i(shader.uShaderMode, 0)
    val railOffset = sim.scrollOffset % 16f
    for (i in 0..13) {
      val rz = 16f - i * 16f + railOffset
      GlMath.setIdentity(modelMatrix)
      GlMath.translate(modelMatrix, 0f, 0f, rz)
      drawMesh(guardrailMesh, modelMatrix, useTint = false, spec = 0.35f, shin = 24f)
    }

    // 4. Distant Mountains
    val useSnow = env == EnvironmentType.MOUNTAIN || env == EnvironmentType.COUNTRYSIDE
    val mountainMesh = if (useSnow) mountainSnowMesh else mountainPlainMesh
    for (i in sim.mountainPositions.indices) {
      val side = if (i % 2 == 0) -1f else 1f
      GlMath.setIdentity(modelMatrix)
      GlMath.translate(modelMatrix, side * 95f, -0.5f, sim.mountainPositions[i])
      GlMath.scale(modelMatrix, 1.35f, 1.0f + (i % 3) * 0.25f, 1.25f)
      drawMesh(
        mountainMesh,
        modelMatrix,
        useTint = true,
        tintR = env.mountainRgb[0],
        tintG = env.mountainRgb[1],
        tintB = env.mountainRgb[2],
        spec = 0.05f,
        shin = 6f,
      )
    }

    // 5. Roadside Scenery Props (Trees, Cacti, City Skyscrapers)
    val propStep = graphicsQuality.propStep
    for (idx in 0 until sim.propPool.size step propStep) {
      val prop = sim.propPool[idx]
      GlMath.setIdentity(modelMatrix)
      GlMath.translate(modelMatrix, prop.x, 0f, prop.z)
      when (env) {
        EnvironmentType.CITY -> {
          GlMath.scale(modelMatrix, 1f, 0.75f + (prop.variant % 3) * 0.35f, 1f)
          val bldg = if (env.isNight) cityNightBldgMesh else cityDayBldgMesh
          drawMesh(bldg, modelMatrix, useTint = false, spec = 0.35f, shin = 32f)
        }
        EnvironmentType.NIGHT -> {
          GlMath.scale(modelMatrix, 1f, 0.70f + (prop.variant % 3) * 0.40f, 1f)
          drawMesh(
            cityNightBldgMesh,
            modelMatrix,
            useTint = false,
            spec = 0.30f,
            shin = 28f,
            emR = 0.08f,
            emG = 0.07f,
            emB = 0.03f,
          )
        }
        EnvironmentType.DESERT -> {
          GlMath.scale(modelMatrix, prop.scale, prop.scale, prop.scale)
          drawMesh(cactusMesh, modelMatrix, useTint = false, spec = 0.08f, shin = 8f)
        }
        EnvironmentType.MOUNTAIN -> {
          GlMath.scale(modelMatrix, prop.scale * 1.15f, prop.scale * 1.25f, prop.scale * 1.15f)
          drawMesh(pineMesh, modelMatrix, useTint = false, spec = 0.05f, shin = 6f)
        }
        else -> {
          GlMath.scale(modelMatrix, prop.scale, prop.scale, prop.scale)
          val tree = if (prop.variant % 2 == 0) pineMesh else broadleafMesh
          drawMesh(tree, modelMatrix, useTint = false, spec = 0.05f, shin = 6f)
        }
      }
    }

    // 6. Highway Streetlamps (especially vivid at Night & Rainy & City)
    if (env == EnvironmentType.NIGHT || env == EnvironmentType.CITY || env == EnvironmentType.RAINY) {
      val bulbGlow = if (env.isNight) 1.0f else if (env == EnvironmentType.RAINY) 0.7f else 0.15f
      for (i in sim.lampPositions.indices) {
        val lz = sim.lampPositions[i]
        val isLeft = i % 2 == 0
        GlMath.setIdentity(modelMatrix)
        GlMath.translate(modelMatrix, if (isLeft) -7.0f else 7.0f, 0f, lz)
        if (!isLeft) GlMath.rotateY(modelMatrix, 180f)
        drawMesh(streetLampPole, modelMatrix, useTint = false, spec = 0.4f, shin = 20f)
        drawMesh(
          streetLampBulb,
          modelMatrix,
          useTint = false,
          spec = 0f,
          shin = 4f,
          emR = bulbGlow,
          emG = bulbGlow * 0.90f,
          emB = bulbGlow * 0.62f,
        )
      }
    }

    // 7. Finish Line Gantry when within draw distance
    if (sim.targetDistanceMeters > 0f) {
      val distToFinish = sim.targetDistanceMeters - sim.distanceMeters
      if (distToFinish in -15f..190f) {
        GlMath.setIdentity(modelMatrix)
        GlMath.translate(modelMatrix, 0f, 0f, -distToFinish)
        drawMesh(gantryStructure, modelMatrix, useTint = false, spec = 0.4f, shin = 24f)
        drawMesh(
          gantryBanner,
          modelMatrix,
          useTint = false,
          spec = 0.15f,
          shin = 10f,
          emR = 0.16f,
          emG = 0.16f,
          emB = 0.16f,
        )
      }
    }

    // 8. AI Traffic Vehicles
    val headlightsAlways = env.isNight || env == EnvironmentType.RAINY
    for (tv in sim.trafficPool) {
      if (!tv.active) continue
      val bundle = trafficMeshes[tv.kind] ?: continue
      GlMath.setIdentity(modelMatrix)
      GlMath.translate(modelMatrix, tv.x, 0f, tv.relZ)
      drawCarBundle(
        bundle = bundle,
        baseModel = modelMatrix,
        colorR = tv.colorR,
        colorG = tv.colorG,
        colorB = tv.colorB,
        wheelSpinDeg = tv.wheelSpinDeg,
        frontSteerDeg = 0f,
        headlightsOn = headlightsAlways,
        braking = tv.braking,
        nitroActive = false,
      )
    }

    // 9. Player Car
    val playerBundle = playerMeshes[sim.config.carSpec.carType] ?: return
    GlMath.setIdentity(modelMatrix)
    GlMath.translate(modelMatrix, sim.playerX, sim.bounceY, 0f)
    GlMath.rotateY(modelMatrix, sim.frontSteerDeg * 0.25f)
    GlMath.rotateZ(modelMatrix, sim.rollDeg)
    GlMath.rotateX(modelMatrix, sim.pitchDeg)
    drawCarBundle(
      bundle = playerBundle,
      baseModel = modelMatrix,
      colorHex = sim.config.upgradeState.colorHex,
      wheelSpinDeg = sim.wheelSpinDeg,
      frontSteerDeg = sim.frontSteerDeg,
      headlightsOn = true,
      braking = sim.isBraking,
      nitroActive = sim.isNitroActive,
    )

    // 10. Rain Particles (Rainy environment)
    if (env.rainIntensity > 0f && graphicsQuality != GraphicsQuality.LOW) {
      val step = if (graphicsQuality == GraphicsQuality.HIGH) 1 else 2
      for (i in 0 until sim.rainParticles.size step step) {
        val drop = sim.rainParticles[i]
        GlMath.setIdentity(modelMatrix)
        GlMath.translate(modelMatrix, drop.x, drop.y, drop.z)
        GlMath.rotateX(modelMatrix, 18f)
        drawMesh(
          rainDropMesh,
          modelMatrix,
          useTint = false,
          spec = 0f,
          shin = 4f,
          emR = 0.25f,
          emG = 0.30f,
          emB = 0.38f,
        )
      }
    }

    // 11. Nitro Wind Speed Streaks
    if (sim.isNitroActive) {
      val count = if (graphicsQuality == GraphicsQuality.LOW) 6 else 12
      for (i in 0 until count) {
        val angle = (i * 0.72f)
        val sx = sim.playerX + cos(angle) * (2.2f + (i % 3) * 1.1f)
        val sy = 0.8f + sin(angle).let { kotlin.math.abs(it) } * 2.1f
        val sz = -((totalTimeSec * 45f + i * 2.6f) % 24f) + 2f
        GlMath.setIdentity(modelMatrix)
        GlMath.translate(modelMatrix, sx, sy, sz)
        drawMesh(
          speedStreakMesh,
          modelMatrix,
          useTint = false,
          spec = 0f,
          shin = 4f,
          emR = 0.15f,
          emG = 0.85f,
          emB = 1.0f,
        )
      }
    }
  }

  private fun drawCarBundle(
    bundle: CarMeshBundle,
    baseModel: FloatArray,
    colorHex: Int,
    wheelSpinDeg: Float,
    frontSteerDeg: Float,
    headlightsOn: Boolean,
    braking: Boolean,
    nitroActive: Boolean,
  ) {
    val r = Color.red(colorHex) / 255f
    val g = Color.green(colorHex) / 255f
    val b = Color.blue(colorHex) / 255f
    drawCarBundle(bundle, baseModel, r, g, b, wheelSpinDeg, frontSteerDeg, headlightsOn, braking, nitroActive)
  }

  private fun drawCarBundle(
    bundle: CarMeshBundle,
    baseModel: FloatArray,
    colorR: Float,
    colorG: Float,
    colorB: Float,
    wheelSpinDeg: Float,
    frontSteerDeg: Float,
    headlightsOn: Boolean,
    braking: Boolean,
    nitroActive: Boolean,
  ) {
    val specBoost = if (graphicsQuality == GraphicsQuality.LOW) 0.25f else 0.72f

    // Painted car body with metallic specular reflection
    drawMesh(
      bundle.bodyMesh,
      baseModel,
      useTint = true,
      tintR = colorR,
      tintG = colorG,
      tintB = colorB,
      spec = specBoost,
      shin = 48f,
    )

    // Dark glass & trim
    drawMesh(bundle.trimGlassMesh, baseModel, useTint = false, spec = 0.85f, shin = 64f)

    // Headlights
    val headGlow = if (headlightsOn) 1.0f else 0.20f
    drawMesh(
      bundle.headlightMesh,
      baseModel,
      useTint = false,
      spec = 0.3f,
      shin = 16f,
      emR = headGlow,
      emG = headGlow * 0.96f,
      emB = headGlow * 0.82f,
    )

    // Taillights (glow intensely when braking)
    val tailGlow = if (braking) 1.35f else 0.45f
    drawMesh(
      bundle.taillightMesh,
      baseModel,
      useTint = false,
      spec = 0.2f,
      shin = 16f,
      emR = tailGlow,
      emG = 0.04f,
      emB = 0.04f,
    )

    // 4 Rotating + Steering Wheels
    for (w in bundle.wheelOffsets) {
      System.arraycopy(baseModel, 0, childModel, 0, 16)
      GlMath.translate(childModel, w[0], w[1], w[2])
      if (w[3] > 0.5f && frontSteerDeg != 0f) {
        GlMath.rotateY(childModel, frontSteerDeg)
      }
      GlMath.rotateX(childModel, -wheelSpinDeg)
      drawMesh(bundle.wheelMesh, childModel, useTint = false, spec = 0.45f, shin = 28f)
    }

    // Nitro exhaust flames
    if (nitroActive) {
      val pulse = 0.85f + sin(totalTimeSec * 42f) * 0.25f
      for (ex in bundle.exhaustOffsets) {
        System.arraycopy(baseModel, 0, childModel, 0, 16)
        GlMath.translate(childModel, ex[0], ex[1], ex[2])
        GlMath.scale(childModel, 1f, 1f, pulse)
        drawMesh(
          nitroFlameMesh,
          childModel,
          useTint = false,
          spec = 0f,
          shin = 4f,
          emR = 0.1f,
          emG = 0.85f,
          emB = 1.0f,
        )
      }
    }
  }

  private fun drawMesh(
    mesh: Mesh,
    model: FloatArray,
    useTint: Boolean,
    tintR: Float = 1f,
    tintG: Float = 1f,
    tintB: Float = 1f,
    spec: Float = 0.35f,
    shin: Float = 24f,
    emR: Float = 0f,
    emG: Float = 0f,
    emB: Float = 0f,
  ) {
    GlMath.multiply(mvpMatrix, vpMatrix, model)
    GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, model, 0)
    GLES20.glUniform1f(shader.uUseTint, if (useTint) 1f else 0f)
    GLES20.glUniform3f(shader.uColorTint, tintR, tintG, tintB)
    GLES20.glUniform1f(shader.uSpecularStrength, spec)
    GLES20.glUniform1f(shader.uShininess, shin)
    GLES20.glUniform3f(shader.uEmissive, emR, emG, emB)
    mesh.draw(shader)
  }
}
