package com.example.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object EnvironmentMeshFactory {

  fun buildSkyDome(radius: Float = 260f): Mesh {
    val mb = MeshBuilder()
    val latSteps = 10
    val lonSteps = 20
    for (lat in 0 until latSteps) {
      val v0 = lat.toFloat() / latSteps
      val v1 = (lat + 1).toFloat() / latSteps
      val phi0 = v0 * (PI.toFloat() * 0.52f) - 0.04f
      val phi1 = v1 * (PI.toFloat() * 0.52f) - 0.04f
      val y0 = sin(phi0) * radius
      val r0 = cos(phi0) * radius
      val y1 = sin(phi1) * radius
      val r1 = cos(phi1) * radius

      for (lon in 0 until lonSteps) {
        val th0 = (2.0 * PI * lon / lonSteps).toFloat()
        val th1 = (2.0 * PI * (lon + 1) / lonSteps).toFloat()
        val ax = cos(th0) * r0
        val az = sin(th0) * r0
        val bx = cos(th1) * r0
        val bz = sin(th1) * r0
        val cx = cos(th1) * r1
        val cz = sin(th1) * r1
        val dx = cos(th0) * r1
        val dz = sin(th0) * r1

        mb.addTri(ax, y0, az, bx, y0, bz, cx, y1, cz, 1f, 1f, 1f, 0f, v0, 1f, v0, 1f, v1)
        mb.addTri(ax, y0, az, cx, y1, cz, dx, y1, dz, 1f, 1f, 1f, 0f, v0, 1f, v1, 0f, v1)
      }
    }
    return mb.build()
  }

  /** Road strip: 4 lanes (-6.2 to +6.2 X) extending from z = +30 to z = -220. */
  fun buildRoadSurface(): Mesh {
    val mb = MeshBuilder()
    val halfW = 6.25f
    mb.addQuad(-halfW, 0f, 30f, halfW, 0f, 30f, halfW, 0f, -220f, -halfW, 0f, -220f, 1f, 1f, 1f)
    return mb.build()
  }

  /** Ground plains flanking the highway. */
  fun buildGroundTerrain(): Mesh {
    val mb = MeshBuilder()
    // Left terrain
    mb.addQuad(-220f, -0.06f, 30f, -6.25f, -0.06f, 30f, -6.25f, -0.06f, -220f, -220f, -0.06f, -220f, 1f, 1f, 1f)
    // Right terrain
    mb.addQuad(6.25f, -0.06f, 30f, 220f, -0.06f, 30f, 220f, -0.06f, -220f, 6.25f, -0.06f, -220f, 1f, 1f, 1f)
    return mb.build()
  }

  /** Guardrails segment of length 16m along Z. */
  fun buildGuardrailSegment(): Mesh {
    val mb = MeshBuilder()
    // Left & right metal rails + concrete posts
    mb.addBox(-6.50f, 0.54f, 0f, 0.12f, 0.16f, 8.0f, 0.72f, 0.75f, 0.80f)
    mb.addBox(6.50f, 0.54f, 0f, 0.12f, 0.16f, 8.0f, 0.72f, 0.75f, 0.80f)
    for (z in listOf(-6f, -2f, 2f, 6f)) {
      mb.addBox(-6.58f, 0.34f, z, 0.10f, 0.34f, 0.10f, 0.45f, 0.48f, 0.52f)
      mb.addBox(6.58f, 0.34f, z, 0.10f, 0.34f, 0.10f, 0.45f, 0.48f, 0.52f)
    }
    return mb.build()
  }

  fun buildPineTree(): Mesh {
    val mb = MeshBuilder()
    mb.addVerticalConeOrCylinder(0f, 0f, 0f, 0.34f, 0.24f, 2.2f, 7, 0.42f, 0.26f, 0.14f)
    mb.addVerticalConeOrCylinder(0f, 1.8f, 0f, 2.1f, 0f, 3.6f, 8, 0.14f, 0.46f, 0.16f)
    mb.addVerticalConeOrCylinder(0f, 4.0f, 0f, 1.6f, 0f, 3.2f, 8, 0.16f, 0.54f, 0.19f)
    mb.addVerticalConeOrCylinder(0f, 6.0f, 0f, 1.05f, 0f, 2.4f, 8, 0.20f, 0.60f, 0.22f)
    return mb.build()
  }

  fun buildBroadleafTree(): Mesh {
    val mb = MeshBuilder()
    mb.addVerticalConeOrCylinder(0f, 0f, 0f, 0.36f, 0.28f, 3.0f, 7, 0.44f, 0.28f, 0.16f)
    mb.addBox(0f, 4.4f, 0f, 2.1f, 1.8f, 2.1f, 0.18f, 0.56f, 0.18f, 0.78f, 0.78f, 0f)
    mb.addBox(0.8f, 5.2f, 0.4f, 1.3f, 1.2f, 1.3f, 0.24f, 0.64f, 0.22f, 0.72f, 0.72f, 0f)
    return mb.build()
  }

  fun buildDesertCactus(): Mesh {
    val mb = MeshBuilder()
    mb.addBox(0f, 2.0f, 0f, 0.34f, 2.0f, 0.34f, 0.22f, 0.56f, 0.24f)
    mb.addBox(-0.72f, 2.3f, 0f, 0.45f, 0.24f, 0.24f, 0.22f, 0.56f, 0.24f)
    mb.addBox(-1.02f, 3.1f, 0f, 0.24f, 0.90f, 0.24f, 0.24f, 0.60f, 0.26f)
    mb.addBox(0.72f, 2.0f, 0f, 0.42f, 0.22f, 0.22f, 0.22f, 0.56f, 0.24f)
    mb.addBox(1.00f, 2.75f, 0f, 0.22f, 0.80f, 0.22f, 0.24f, 0.60f, 0.26f)
    return mb.build()
  }

  fun buildCityBuilding(height: Float, isNight: Boolean): Mesh {
    val mb = MeshBuilder()
    val w = 5.5f
    val d = 5.5f
    val baseR = if (isNight) 0.12f else 0.38f
    val baseG = if (isNight) 0.14f else 0.42f
    val baseB = if (isNight) 0.20f else 0.50f
    mb.addBox(0f, height * 0.5f, 0f, w, height * 0.5f, d, baseR, baseG, baseB)

    // Window strips on front/side faces
    val floors = (height / 3.4f).toInt().coerceAtLeast(3)
    val winR = if (isNight) 0.98f else 0.62f
    val winG = if (isNight) 0.88f else 0.78f
    val winB = if (isNight) 0.52f else 0.92f
    for (f in 1 until floors) {
      val wy = f * 3.2f
      mb.addBox(0f, wy, 0f, w + 0.04f, 0.55f, d - 0.8f, winR, winG, winB)
      mb.addBox(0f, wy, 0f, w - 0.8f, 0.55f, d + 0.04f, winR, winG, winB)
    }
    return mb.build()
  }

  fun buildMountainRidge(snowCap: Boolean): Mesh {
    val mb = MeshBuilder()
    // Broad sculpted pyramid ridge
    val h = 38f
    val w = 34f
    val d = 28f
    mb.addTri(-w, 0f, d, w, 0f, d, 0f, h, 0f, 1f, 1f, 1f)
    mb.addTri(w, 0f, d, w, 0f, -d, 0f, h, 0f, 0.90f, 0.90f, 0.90f)
    mb.addTri(w, 0f, -d, -w, 0f, -d, 0f, h, 0f, 0.82f, 0.82f, 0.82f)
    mb.addTri(-w, 0f, -d, -w, 0f, d, 0f, h, 0f, 0.92f, 0.92f, 0.92f)
    if (snowCap) {
      val sh = h * 0.66f
      val sw = w * 0.34f
      val sd = d * 0.34f
      mb.addTri(-sw, sh, sd, sw, sh, sd, 0f, h + 0.25f, 0f, 0.96f, 0.98f, 1.0f)
      mb.addTri(sw, sh, sd, sw, sh, -sd, 0f, h + 0.25f, 0f, 0.92f, 0.95f, 0.98f)
      mb.addTri(sw, sh, -sd, -sw, sh, -sd, 0f, h + 0.25f, 0f, 0.88f, 0.92f, 0.96f)
      mb.addTri(-sw, sh, -sd, -sw, sh, sd, 0f, h + 0.25f, 0f, 0.94f, 0.96f, 0.99f)
    }
    return mb.build()
  }

  /** Returns pair of (poleMesh, glowingLampMesh). */
  fun buildStreetLamp(): Pair<Mesh, Mesh> {
    val pole = MeshBuilder()
    val lamp = MeshBuilder()
    // Pole at x = 0, arm extending inward (+X or -X depending on rotation)
    pole.addBox(0f, 3.6f, 0f, 0.11f, 3.6f, 0.11f, 0.32f, 0.35f, 0.40f)
    pole.addBox(1.1f, 7.1f, 0f, 1.15f, 0.09f, 0.09f, 0.32f, 0.35f, 0.40f)
    lamp.addBox(2.05f, 6.96f, 0f, 0.32f, 0.08f, 0.18f, 1.0f, 0.92f, 0.68f)
    return pole.build() to lamp.build()
  }

  /** Finish line overhead gantry across the 4-lane highway. */
  fun buildFinishGantry(): Pair<Mesh, Mesh> {
    val structure = MeshBuilder()
    val banner = MeshBuilder()
    structure.addBox(-6.9f, 3.5f, 0f, 0.28f, 3.5f, 0.28f, 0.86f, 0.88f, 0.92f)
    structure.addBox(6.9f, 3.5f, 0f, 0.28f, 3.5f, 0.28f, 0.86f, 0.88f, 0.92f)
    structure.addBox(0f, 6.8f, 0f, 7.2f, 0.22f, 0.36f, 0.20f, 0.22f, 0.28f)

    // Checkered finish banner blocks
    val cols = 12
    val totalW = 13.0f
    val cellW = totalW / cols
    for (i in 0 until cols) {
      val cx = -totalW * 0.5f + cellW * (i + 0.5f)
      val isWhiteTop = i % 2 == 0
      val rT = if (isWhiteTop) 0.98f else 0.08f
      val rB = if (!isWhiteTop) 0.98f else 0.08f
      banner.addBox(cx, 6.15f, 0f, cellW * 0.49f, 0.34f, 0.16f, rT, rT, rT)
      banner.addBox(cx, 5.45f, 0f, cellW * 0.49f, 0.34f, 0.16f, rB, rB, rB)
    }
    return structure.build() to banner.build()
  }

  fun buildStreakMesh(): Mesh {
    val mb = MeshBuilder()
    mb.addBox(0f, 0f, 0f, 0.018f, 0.018f, 0.90f, 0.78f, 0.92f, 1.0f)
    return mb.build()
  }

  fun buildRainDropMesh(): Mesh {
    val mb = MeshBuilder()
    mb.addBox(0f, 0f, 0f, 0.015f, 0.42f, 0.015f, 0.74f, 0.86f, 0.98f)
    return mb.build()
  }

  fun buildShowroomPedestal(): Mesh {
    val mb = MeshBuilder()
    mb.addVerticalConeOrCylinder(0f, -0.18f, 0f, 3.6f, 3.4f, 0.18f, 32, 0.15f, 0.18f, 0.25f)
    mb.addVerticalConeOrCylinder(0f, -0.02f, 0f, 3.44f, 3.44f, 0.03f, 32, 0.0f, 0.85f, 1.0f)
    return mb.build()
  }
}
