package com.example.engine

import com.example.model.CarType

data class CarMeshBundle(
  val bodyMesh: Mesh,
  val trimGlassMesh: Mesh,
  val headlightMesh: Mesh,
  val taillightMesh: Mesh,
  val wheelMesh: Mesh,
  val wheelOffsets: List<FloatArray>, // [x, y, z, isFront(1/0)]
  val exhaustOffsets: List<FloatArray>, // [x, y, z]
)

enum class TrafficKind(
  val halfWidth: Float,
  val halfLength: Float,
  val height: Float,
  val speedRangeMps: ClosedFloatingPointRange<Float>,
) {
  SEDAN(0.92f, 2.15f, 1.32f, 22f..32f),
  SUV(1.02f, 2.35f, 1.62f, 20f..29f),
  VAN(1.04f, 2.55f, 1.92f, 18f..25f),
  TRUCK(1.16f, 3.65f, 2.85f, 15f..21f),
  BUS(1.18f, 4.30f, 3.05f, 14f..20f),
}

object VehicleMeshFactory {

  fun buildPlayerCar(type: CarType): CarMeshBundle {
    val body = MeshBuilder()
    val trim = MeshBuilder()
    val heads = MeshBuilder()
    val tails = MeshBuilder()
    val wheel = MeshBuilder()

    // Forward is -Z, Rear is +Z (chase camera looks toward -Z)
    val wheelRadius: Float
    val wheelWidth: Float
    val wheelOffsets: List<FloatArray>
    val exhaustOffsets: List<FloatArray>

    when (type) {
      CarType.SPORTS -> {
        wheelRadius = 0.34f
        wheelWidth = 0.27f
        // Sculpted sports coupe body
        body.addBox(0f, 0.42f, 0f, 0.92f, 0.22f, 2.12f, 1f, 1f, 1f, 0.94f, 0.97f, 0.04f)
        // Hood slope (front is -Z)
        body.addBox(0f, 0.64f, -1.25f, 0.82f, 0.12f, 0.78f, 0.96f, 0.96f, 0.96f, 0.88f, 0.85f, 0.08f)
        // Rear deck
        body.addBox(0f, 0.66f, 1.35f, 0.85f, 0.12f, 0.62f, 0.96f, 0.96f, 0.96f, 0.90f, 0.90f, -0.04f)
        // Cabin roof & pillars
        body.addBox(0f, 0.96f, 0.15f, 0.72f, 0.26f, 0.98f, 0.95f, 0.95f, 0.95f, 0.78f, 0.66f, 0.05f)
        // Rear spoiler wing
        body.addBox(0f, 0.94f, 1.88f, 0.88f, 0.04f, 0.20f, 0.22f, 0.22f, 0.26f)
        trim.addBox(-0.55f, 0.82f, 1.86f, 0.05f, 0.14f, 0.12f, 0.15f, 0.16f, 0.18f)
        trim.addBox(0.55f, 0.82f, 1.86f, 0.05f, 0.14f, 0.12f, 0.15f, 0.16f, 0.18f)

        // Windows & grille
        trim.addBox(0f, 0.93f, 0.15f, 0.75f, 0.20f, 0.88f, 0.10f, 0.14f, 0.20f, 0.80f, 0.70f, 0.04f)
        trim.addBox(0f, 0.34f, -2.14f, 0.68f, 0.11f, 0.05f, 0.10f, 0.11f, 0.13f)
        trim.addBox(0f, 0.22f, 0f, 0.94f, 0.06f, 2.14f, 0.12f, 0.12f, 0.14f)

        // Headlights (-Z) & Taillights (+Z)
        heads.addBox(-0.64f, 0.48f, -2.12f, 0.20f, 0.08f, 0.06f, 1f, 0.98f, 0.86f)
        heads.addBox(0.64f, 0.48f, -2.12f, 0.20f, 0.08f, 0.06f, 1f, 0.98f, 0.86f)
        tails.addBox(-0.62f, 0.52f, 2.12f, 0.22f, 0.08f, 0.06f, 1f, 0.12f, 0.12f)
        tails.addBox(0.62f, 0.52f, 2.12f, 0.22f, 0.08f, 0.06f, 1f, 0.12f, 0.12f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.88f, wheelRadius, -1.32f, 1f),
            floatArrayOf(0.88f, wheelRadius, -1.32f, 1f),
            floatArrayOf(-0.88f, wheelRadius, 1.32f, 0f),
            floatArrayOf(0.88f, wheelRadius, 1.32f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.42f, 0.28f, 2.14f), floatArrayOf(0.42f, 0.28f, 2.14f))
      }

      CarType.SUPERCAR -> {
        wheelRadius = 0.35f
        wheelWidth = 0.31f
        // Ultra-low wide wedge
        body.addBox(0f, 0.36f, 0f, 0.98f, 0.19f, 2.20f, 1f, 1f, 1f, 0.95f, 0.96f, 0.06f)
        body.addBox(0f, 0.56f, -1.20f, 0.90f, 0.10f, 0.92f, 0.96f, 0.96f, 0.96f, 0.85f, 0.82f, 0.10f)
        body.addBox(0f, 0.62f, 1.15f, 0.92f, 0.14f, 0.95f, 0.95f, 0.95f, 0.95f, 0.86f, 0.88f, -0.05f)
        // Teardrop glass canopy
        trim.addBox(0f, 0.86f, 0.05f, 0.68f, 0.22f, 0.96f, 0.08f, 0.12f, 0.18f, 0.72f, 0.62f, 0.08f)
        body.addBox(0f, 1.04f, 0.15f, 0.52f, 0.05f, 0.60f, 0.16f, 0.17f, 0.20f)
        // Carbon splitter & big GT wing
        trim.addBox(0f, 0.18f, -2.22f, 0.98f, 0.04f, 0.18f, 0.12f, 0.13f, 0.16f)
        trim.addBox(0f, 0.98f, 2.02f, 0.96f, 0.04f, 0.24f, 0.14f, 0.15f, 0.18f)
        trim.addBox(-0.56f, 0.82f, 1.98f, 0.04f, 0.18f, 0.12f, 0.14f, 0.15f, 0.18f)
        trim.addBox(0.56f, 0.82f, 1.98f, 0.04f, 0.18f, 0.12f, 0.14f, 0.15f, 0.18f)

        heads.addBox(-0.68f, 0.44f, -2.18f, 0.22f, 0.06f, 0.06f, 0.92f, 0.98f, 1f)
        heads.addBox(0.68f, 0.44f, -2.18f, 0.22f, 0.06f, 0.06f, 0.92f, 0.98f, 1f)
        tails.addBox(0f, 0.54f, 2.20f, 0.80f, 0.06f, 0.05f, 1f, 0.10f, 0.14f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.92f, wheelRadius, -1.36f, 1f),
            floatArrayOf(0.92f, wheelRadius, -1.36f, 1f),
            floatArrayOf(-0.94f, wheelRadius, 1.34f, 0f),
            floatArrayOf(0.94f, wheelRadius, 1.34f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.22f, 0.30f, 2.22f), floatArrayOf(0.22f, 0.30f, 2.22f))
      }

      CarType.SEDAN -> {
        wheelRadius = 0.34f
        wheelWidth = 0.25f
        body.addBox(0f, 0.46f, 0f, 0.94f, 0.24f, 2.34f, 1f, 1f, 1f, 0.96f, 0.98f, 0f)
        body.addBox(0f, 0.72f, -1.35f, 0.88f, 0.10f, 0.85f, 0.96f, 0.96f, 0.96f, 0.92f, 0.90f, 0.06f)
        body.addBox(0f, 1.02f, 0.18f, 0.78f, 0.28f, 1.24f, 0.95f, 0.95f, 0.95f, 0.84f, 0.74f, 0.02f)
        trim.addBox(0f, 0.98f, 0.18f, 0.80f, 0.22f, 1.12f, 0.11f, 0.15f, 0.21f, 0.85f, 0.75f, 0.02f)
        // Chrome front grille
        trim.addBox(0f, 0.46f, -2.35f, 0.45f, 0.16f, 0.05f, 0.78f, 0.82f, 0.88f)
        heads.addBox(-0.68f, 0.52f, -2.33f, 0.20f, 0.09f, 0.05f, 1f, 0.98f, 0.90f)
        heads.addBox(0.68f, 0.52f, -2.33f, 0.20f, 0.09f, 0.05f, 1f, 0.98f, 0.90f)
        tails.addBox(-0.66f, 0.58f, 2.33f, 0.24f, 0.08f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.66f, 0.58f, 2.33f, 0.24f, 0.08f, 0.05f, 1f, 0.12f, 0.12f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.88f, wheelRadius, -1.42f, 1f),
            floatArrayOf(0.88f, wheelRadius, -1.42f, 1f),
            floatArrayOf(-0.88f, wheelRadius, 1.42f, 0f),
            floatArrayOf(0.88f, wheelRadius, 1.42f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.56f, 0.28f, 2.35f), floatArrayOf(0.56f, 0.28f, 2.35f))
      }

      CarType.MUSCLE -> {
        wheelRadius = 0.36f
        wheelWidth = 0.29f
        body.addBox(0f, 0.46f, 0f, 0.96f, 0.25f, 2.26f, 1f, 1f, 1f, 0.96f, 0.98f, 0f)
        // Big supercharger hood scoop
        body.addBox(0f, 0.74f, -1.10f, 0.88f, 0.10f, 0.98f, 0.96f, 0.96f, 0.96f, 0.94f, 0.94f, 0f)
        trim.addBox(0f, 0.86f, -1.05f, 0.30f, 0.08f, 0.48f, 0.18f, 0.19f, 0.22f)
        // Fastback cabin
        body.addBox(0f, 1.00f, 0.24f, 0.78f, 0.25f, 1.12f, 0.95f, 0.95f, 0.95f, 0.82f, 0.72f, 0.05f)
        trim.addBox(0f, 0.96f, 0.22f, 0.80f, 0.19f, 1.02f, 0.10f, 0.14f, 0.19f, 0.83f, 0.73f, 0.04f)
        // Racing stripes
        trim.addBox(-0.18f, 0.85f, -1.2f, 0.10f, 0.01f, 0.90f, 0.12f, 0.12f, 0.14f)
        trim.addBox(0.18f, 0.85f, -1.2f, 0.10f, 0.01f, 0.90f, 0.12f, 0.12f, 0.14f)
        trim.addBox(0f, 0.46f, -2.27f, 0.86f, 0.14f, 0.05f, 0.12f, 0.13f, 0.15f)

        heads.addBox(-0.68f, 0.50f, -2.28f, 0.18f, 0.09f, 0.05f, 1f, 0.96f, 0.82f)
        heads.addBox(0.68f, 0.50f, -2.28f, 0.18f, 0.09f, 0.05f, 1f, 0.96f, 0.82f)
        tails.addBox(-0.64f, 0.55f, 2.26f, 0.24f, 0.09f, 0.05f, 1f, 0.12f, 0.10f)
        tails.addBox(0.64f, 0.55f, 2.26f, 0.24f, 0.09f, 0.05f, 1f, 0.12f, 0.10f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.90f, wheelRadius, -1.36f, 1f),
            floatArrayOf(0.90f, wheelRadius, -1.36f, 1f),
            floatArrayOf(-0.92f, wheelRadius, 1.34f, 0f),
            floatArrayOf(0.92f, wheelRadius, 1.34f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.52f, 0.27f, 2.28f), floatArrayOf(0.52f, 0.27f, 2.28f))
      }

      CarType.RACING -> {
        wheelRadius = 0.35f
        wheelWidth = 0.34f
        // Le Mans prototype low monocoque + pronounced wheel fenders + shark fin + big wing
        body.addBox(0f, 0.30f, 0f, 0.98f, 0.16f, 2.30f, 1f, 1f, 1f, 0.94f, 0.96f, 0f)
        body.addBox(-0.66f, 0.46f, -1.25f, 0.32f, 0.14f, 0.86f, 0.96f, 0.96f, 0.96f, 0.86f, 0.84f, 0.04f)
        body.addBox(0.66f, 0.46f, -1.25f, 0.32f, 0.14f, 0.86f, 0.96f, 0.96f, 0.96f, 0.86f, 0.84f, 0.04f)
        body.addBox(-0.68f, 0.48f, 1.20f, 0.30f, 0.15f, 0.90f, 0.96f, 0.96f, 0.96f, 0.88f, 0.86f, -0.04f)
        body.addBox(0.68f, 0.48f, 1.20f, 0.30f, 0.15f, 0.90f, 0.96f, 0.96f, 0.96f, 0.88f, 0.86f, -0.04f)
        // Bubble cockpit
        trim.addBox(0f, 0.72f, -0.10f, 0.46f, 0.24f, 0.92f, 0.09f, 0.12f, 0.18f, 0.68f, 0.56f, 0.06f)
        // Dorsal shark fin
        body.addBox(0f, 0.78f, 1.15f, 0.04f, 0.22f, 0.78f, 0.92f, 0.92f, 0.92f)
        // Front splitter & huge rear wing
        trim.addBox(0f, 0.15f, -2.32f, 0.98f, 0.04f, 0.22f, 0.14f, 0.15f, 0.18f)
        trim.addBox(0f, 1.02f, 2.08f, 0.98f, 0.05f, 0.28f, 0.14f, 0.15f, 0.18f)
        trim.addBox(-0.62f, 0.76f, 2.04f, 0.04f, 0.26f, 0.16f, 0.14f, 0.15f, 0.18f)
        trim.addBox(0.62f, 0.76f, 2.04f, 0.04f, 0.26f, 0.16f, 0.14f, 0.15f, 0.18f)

        heads.addBox(-0.68f, 0.44f, -2.14f, 0.18f, 0.07f, 0.06f, 0.95f, 0.99f, 1f)
        heads.addBox(0.68f, 0.44f, -2.14f, 0.18f, 0.07f, 0.06f, 0.95f, 0.99f, 1f)
        tails.addBox(-0.68f, 0.48f, 2.16f, 0.22f, 0.07f, 0.06f, 1f, 0.08f, 0.12f)
        tails.addBox(0.68f, 0.48f, 2.16f, 0.22f, 0.07f, 0.06f, 1f, 0.08f, 0.12f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.94f, wheelRadius, -1.40f, 1f),
            floatArrayOf(0.94f, wheelRadius, -1.40f, 1f),
            floatArrayOf(-0.94f, wheelRadius, 1.38f, 0f),
            floatArrayOf(0.94f, wheelRadius, 1.38f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.18f, 0.28f, 2.28f), floatArrayOf(0.18f, 0.28f, 2.28f))
      }

      CarType.SUV -> {
        wheelRadius = 0.42f
        wheelWidth = 0.29f
        body.addBox(0f, 0.62f, 0f, 1.00f, 0.30f, 2.36f, 1f, 1f, 1f, 0.96f, 0.98f, 0f)
        body.addBox(0f, 1.24f, 0.34f, 0.90f, 0.34f, 1.62f, 0.96f, 0.96f, 0.96f, 0.88f, 0.86f, 0.04f)
        trim.addBox(0f, 1.20f, 0.34f, 0.92f, 0.25f, 1.50f, 0.11f, 0.15f, 0.21f, 0.89f, 0.87f, 0.04f)
        // Skid plate, cladding & roof rails
        trim.addBox(0f, 0.30f, 0f, 1.02f, 0.09f, 2.38f, 0.14f, 0.15f, 0.18f)
        trim.addBox(-0.62f, 1.62f, 0.40f, 0.04f, 0.05f, 1.20f, 0.22f, 0.24f, 0.28f)
        trim.addBox(0.62f, 1.62f, 0.40f, 0.04f, 0.05f, 1.20f, 0.22f, 0.24f, 0.28f)
        trim.addBox(0f, 0.64f, -2.37f, 0.54f, 0.18f, 0.05f, 0.20f, 0.22f, 0.26f)

        heads.addBox(-0.74f, 0.68f, -2.35f, 0.18f, 0.10f, 0.06f, 1f, 0.98f, 0.90f)
        heads.addBox(0.74f, 0.68f, -2.35f, 0.18f, 0.10f, 0.06f, 1f, 0.98f, 0.90f)
        tails.addBox(-0.74f, 0.78f, 2.35f, 0.20f, 0.12f, 0.06f, 1f, 0.12f, 0.12f)
        tails.addBox(0.74f, 0.78f, 2.35f, 0.20f, 0.12f, 0.06f, 1f, 0.12f, 0.12f)

        wheelOffsets =
          listOf(
            floatArrayOf(-0.92f, wheelRadius, -1.42f, 1f),
            floatArrayOf(0.92f, wheelRadius, -1.42f, 1f),
            floatArrayOf(-0.92f, wheelRadius, 1.42f, 0f),
            floatArrayOf(0.92f, wheelRadius, 1.42f, 0f),
          )
        exhaustOffsets = listOf(floatArrayOf(-0.55f, 0.34f, 2.38f), floatArrayOf(0.55f, 0.34f, 2.38f))
      }
    }

    wheel.addWheelCylinder(0f, 0f, 0f, wheelRadius, wheelWidth, 14)

    return CarMeshBundle(
      bodyMesh = body.build(),
      trimGlassMesh = trim.build(),
      headlightMesh = heads.build(),
      taillightMesh = tails.build(),
      wheelMesh = wheel.build(),
      wheelOffsets = wheelOffsets,
      exhaustOffsets = exhaustOffsets,
    )
  }

  fun buildTrafficVehicle(kind: TrafficKind): CarMeshBundle {
    val body = MeshBuilder()
    val trim = MeshBuilder()
    val heads = MeshBuilder()
    val tails = MeshBuilder()
    val wheel = MeshBuilder()

    val wheelRadius: Float
    val wheelOffsets: List<FloatArray>

    when (kind) {
      TrafficKind.SEDAN -> {
        wheelRadius = 0.33f
        body.addBox(0f, 0.45f, 0f, 0.90f, 0.23f, 2.12f, 1f, 1f, 1f, 0.95f, 0.97f, 0f)
        body.addBox(0f, 0.96f, 0.15f, 0.74f, 0.26f, 1.12f, 0.95f, 0.95f, 0.95f, 0.82f, 0.72f, 0.02f)
        trim.addBox(0f, 0.93f, 0.15f, 0.76f, 0.20f, 1.02f, 0.10f, 0.13f, 0.18f, 0.83f, 0.73f, 0.02f)
        heads.addBox(-0.64f, 0.48f, -2.11f, 0.18f, 0.08f, 0.05f, 1f, 0.96f, 0.82f)
        heads.addBox(0.64f, 0.48f, -2.11f, 0.18f, 0.08f, 0.05f, 1f, 0.96f, 0.82f)
        tails.addBox(-0.64f, 0.52f, 2.11f, 0.20f, 0.08f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.64f, 0.52f, 2.11f, 0.20f, 0.08f, 0.05f, 1f, 0.12f, 0.12f)
        wheelOffsets =
          listOf(
            floatArrayOf(-0.84f, wheelRadius, -1.30f, 0f),
            floatArrayOf(0.84f, wheelRadius, -1.30f, 0f),
            floatArrayOf(-0.84f, wheelRadius, 1.30f, 0f),
            floatArrayOf(0.84f, wheelRadius, 1.30f, 0f),
          )
      }
      TrafficKind.SUV -> {
        wheelRadius = 0.40f
        body.addBox(0f, 0.58f, 0f, 0.98f, 0.28f, 2.30f, 1f, 1f, 1f)
        body.addBox(0f, 1.20f, 0.30f, 0.88f, 0.32f, 1.56f, 0.96f, 0.96f, 0.96f, 0.90f, 0.88f, 0f)
        trim.addBox(0f, 1.16f, 0.30f, 0.90f, 0.24f, 1.44f, 0.10f, 0.14f, 0.19f, 0.91f, 0.89f, 0f)
        heads.addBox(-0.70f, 0.62f, -2.29f, 0.18f, 0.09f, 0.05f, 1f, 0.96f, 0.84f)
        heads.addBox(0.70f, 0.62f, -2.29f, 0.18f, 0.09f, 0.05f, 1f, 0.96f, 0.84f)
        tails.addBox(-0.70f, 0.70f, 2.29f, 0.18f, 0.12f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.70f, 0.70f, 2.29f, 0.18f, 0.12f, 0.05f, 1f, 0.12f, 0.12f)
        wheelOffsets =
          listOf(
            floatArrayOf(-0.90f, wheelRadius, -1.36f, 0f),
            floatArrayOf(0.90f, wheelRadius, -1.36f, 0f),
            floatArrayOf(-0.90f, wheelRadius, 1.36f, 0f),
            floatArrayOf(0.90f, wheelRadius, 1.36f, 0f),
          )
      }
      TrafficKind.VAN -> {
        wheelRadius = 0.38f
        body.addBox(0f, 1.02f, 0.15f, 1.00f, 0.74f, 2.26f, 1f, 1f, 1f, 0.94f, 0.96f, 0.04f)
        body.addBox(0f, 0.66f, -2.18f, 0.96f, 0.34f, 0.32f, 0.96f, 0.96f, 0.96f, 0.90f, 0.85f, 0.04f)
        trim.addBox(0f, 1.30f, -1.96f, 0.88f, 0.32f, 0.24f, 0.10f, 0.14f, 0.20f)
        heads.addBox(-0.72f, 0.56f, -2.50f, 0.18f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        heads.addBox(0.72f, 0.56f, -2.50f, 0.18f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        tails.addBox(-0.78f, 0.78f, 2.42f, 0.14f, 0.22f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.78f, 0.78f, 2.42f, 0.14f, 0.22f, 0.05f, 1f, 0.12f, 0.12f)
        wheelOffsets =
          listOf(
            floatArrayOf(-0.92f, wheelRadius, -1.55f, 0f),
            floatArrayOf(0.92f, wheelRadius, -1.55f, 0f),
            floatArrayOf(-0.92f, wheelRadius, 1.55f, 0f),
            floatArrayOf(0.92f, wheelRadius, 1.55f, 0f),
          )
      }
      TrafficKind.TRUCK -> {
        wheelRadius = 0.46f
        // Cab (-Z)
        body.addBox(0f, 1.24f, -2.55f, 1.06f, 0.88f, 0.96f, 1f, 1f, 1f, 0.94f, 0.92f, 0.04f)
        trim.addBox(0f, 1.52f, -2.98f, 0.94f, 0.34f, 0.16f, 0.10f, 0.14f, 0.20f)
        trim.addBox(0f, 0.78f, -3.52f, 0.68f, 0.26f, 0.05f, 0.22f, 0.24f, 0.28f)
        // Cargo Trailer (+Z)
        trim.addBox(0f, 1.68f, 1.05f, 1.14f, 1.18f, 2.45f, 0.88f, 0.90f, 0.92f)
        trim.addBox(0f, 0.42f, 0.10f, 0.92f, 0.14f, 3.30f, 0.16f, 0.17f, 0.19f)
        heads.addBox(-0.78f, 0.72f, -3.52f, 0.20f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        heads.addBox(0.78f, 0.72f, -3.52f, 0.20f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        tails.addBox(-0.88f, 0.76f, 3.52f, 0.18f, 0.10f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.88f, 0.76f, 3.52f, 0.18f, 0.10f, 0.05f, 1f, 0.12f, 0.12f)
        wheelOffsets =
          listOf(
            floatArrayOf(-1.02f, wheelRadius, -2.55f, 0f),
            floatArrayOf(1.02f, wheelRadius, -2.55f, 0f),
            floatArrayOf(-1.02f, wheelRadius, 1.85f, 0f),
            floatArrayOf(1.02f, wheelRadius, 1.85f, 0f),
            floatArrayOf(-1.02f, wheelRadius, 2.85f, 0f),
            floatArrayOf(1.02f, wheelRadius, 2.85f, 0f),
          )
      }
      TrafficKind.BUS -> {
        wheelRadius = 0.46f
        body.addBox(0f, 1.56f, 0f, 1.14f, 1.16f, 4.20f, 1f, 1f, 1f, 0.96f, 0.98f, 0f)
        trim.addBox(0f, 1.82f, 0f, 1.16f, 0.44f, 3.90f, 0.12f, 0.16f, 0.22f)
        trim.addBox(0f, 0.38f, 0f, 1.15f, 0.14f, 4.22f, 0.15f, 0.16f, 0.19f)
        heads.addBox(-0.82f, 0.66f, -4.21f, 0.22f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        heads.addBox(0.82f, 0.66f, -4.21f, 0.22f, 0.10f, 0.05f, 1f, 0.96f, 0.84f)
        tails.addBox(-0.84f, 0.82f, 4.21f, 0.18f, 0.18f, 0.05f, 1f, 0.12f, 0.12f)
        tails.addBox(0.84f, 0.82f, 4.21f, 0.18f, 0.18f, 0.05f, 1f, 0.12f, 0.12f)
        wheelOffsets =
          listOf(
            floatArrayOf(-1.02f, wheelRadius, -2.70f, 0f),
            floatArrayOf(1.02f, wheelRadius, -2.70f, 0f),
            floatArrayOf(-1.02f, wheelRadius, 2.40f, 0f),
            floatArrayOf(1.02f, wheelRadius, 2.40f, 0f),
          )
      }
    }

    wheel.addWheelCylinder(0f, 0f, 0f, wheelRadius, 0.28f, 12)

    return CarMeshBundle(
      bodyMesh = body.build(),
      trimGlassMesh = trim.build(),
      headlightMesh = heads.build(),
      taillightMesh = tails.build(),
      wheelMesh = wheel.build(),
      wheelOffsets = wheelOffsets,
      exhaustOffsets = emptyList(),
    )
  }

  fun buildNitroFlameMesh(): Mesh {
    val mb = MeshBuilder()
    // Cone pointing backward (+Z) from exhaust
    for (i in 0 until 8) {
      val a0 = (2.0 * Math.PI * i / 8).toFloat()
      val a1 = (2.0 * Math.PI * (i + 1) / 8).toFloat()
      val r = 0.13f
      val x0 = kotlin.math.cos(a0) * r
      val y0 = kotlin.math.sin(a0) * r
      val x1 = kotlin.math.cos(a1) * r
      val y1 = kotlin.math.sin(a1) * r
      mb.addTri(x0, y0, 0f, x1, y1, 0f, 0f, 0f, 1.0f, 0.1f, 0.88f, 1.0f)
    }
    return mb.build()
  }
}
