package com.example.model

enum class CarType {
  SPORTS,
  SUPERCAR,
  SEDAN,
  MUSCLE,
  RACING,
  SUV,
}

data class CarSpec(
  val id: String,
  val name: String,
  val category: String,
  val carType: CarType,
  val description: String,
  val price: Int,
  val baseTopSpeedKmh: Float,
  val baseAcceleration: Float,
  val baseHandling: Float,
  val baseNitroPower: Float,
  val defaultColorHex: Int,
  val halfWidth: Float,
  val halfLength: Float,
  val height: Float,
)

object CarCatalog {
  val paintPalette: List<Pair<String, Int>> =
    listOf(
      "Crimson Red" to 0xFFE62038.toInt(),
      "Electric Cyan" to 0xFF00D4FF.toInt(),
      "Solar Yellow" to 0xFFFFC400.toInt(),
      "Emerald Neon" to 0xFF00D66C.toInt(),
      "Midnight Blue" to 0xFF1E5BFF.toInt(),
      "Pearl White" to 0xFFEDF2F7.toInt(),
      "Obsidian" to 0xFF232833.toInt(),
      "Blaze Orange" to 0xFFFF5A1F.toInt(),
    )

  val cars: List<CarSpec> =
    listOf(
      CarSpec(
        id = "apex_gt",
        name = "Apex GT",
        category = "Sports Car",
        carType = CarType.SPORTS,
        description = "Balanced twin-turbo coupe with crisp lane agility.",
        price = 0,
        baseTopSpeedKmh = 235f,
        baseAcceleration = 0.68f,
        baseHandling = 0.78f,
        baseNitroPower = 0.70f,
        defaultColorHex = 0xFFE62038.toInt(),
        halfWidth = 0.92f,
        halfLength = 2.15f,
        height = 1.24f,
      ),
      CarSpec(
        id = "titan_v8",
        name = "Titan V8",
        category = "Muscle Car",
        carType = CarType.MUSCLE,
        description = "Supercharged V8 torque monster built for raw straight-line launches.",
        price = 1200,
        baseTopSpeedKmh = 252f,
        baseAcceleration = 0.76f,
        baseHandling = 0.66f,
        baseNitroPower = 0.78f,
        defaultColorHex = 0xFFFF5A1F.toInt(),
        halfWidth = 0.96f,
        halfLength = 2.28f,
        height = 1.30f,
      ),
      CarSpec(
        id = "sovereign_s8",
        name = "Sovereign S8",
        category = "Luxury Sedan",
        carType = CarType.SEDAN,
        description = "Long-wheelbase grand tourer with silky high-speed stability.",
        price = 2500,
        baseTopSpeedKmh = 264f,
        baseAcceleration = 0.74f,
        baseHandling = 0.80f,
        baseNitroPower = 0.76f,
        defaultColorHex = 0xFF1E5BFF.toInt(),
        halfWidth = 0.95f,
        halfLength = 2.35f,
        height = 1.34f,
      ),
      CarSpec(
        id = "atlas_x7",
        name = "Atlas X7",
        category = "Performance SUV",
        carType = CarType.SUV,
        description = "Heavy armored stance with boosted nitro reserve and strong stability.",
        price = 3800,
        baseTopSpeedKmh = 258f,
        baseAcceleration = 0.78f,
        baseHandling = 0.72f,
        baseNitroPower = 0.88f,
        defaultColorHex = 0xFFEDF2F7.toInt(),
        halfWidth = 1.02f,
        halfLength = 2.38f,
        height = 1.60f,
      ),
      CarSpec(
        id = "valkyrie_v12",
        name = "Valkyrie V12",
        category = "Supercar",
        carType = CarType.SUPERCAR,
        description = "Mid-engine carbon wedge engineered for razor-sharp overtakes.",
        price = 6500,
        baseTopSpeedKmh = 305f,
        baseAcceleration = 0.88f,
        baseHandling = 0.90f,
        baseNitroPower = 0.86f,
        defaultColorHex = 0xFFFFC400.toInt(),
        halfWidth = 0.98f,
        halfLength = 2.22f,
        height = 1.14f,
      ),
      CarSpec(
        id = "furia_lmp",
        name = "Furia LMP",
        category = "Racing Car",
        carType = CarType.RACING,
        description = "Le Mans endurance prototype with unmatched aero grip and top velocity.",
        price = 10000,
        baseTopSpeedKmh = 340f,
        baseAcceleration = 0.95f,
        baseHandling = 0.96f,
        baseNitroPower = 0.95f,
        defaultColorHex = 0xFF00D4FF.toInt(),
        halfWidth = 0.98f,
        halfLength = 2.32f,
        height = 1.08f,
      ),
    )

  fun findById(id: String): CarSpec = cars.firstOrNull { it.id == id } ?: cars.first()
}

enum class EnvironmentType(
  val displayName: String,
  val shortTag: String,
  val skyTopRgb: FloatArray,
  val skyHorizonRgb: FloatArray,
  val fogRgb: FloatArray,
  val fogDensity: Float,
  val sunDir: FloatArray,
  val sunColorRgb: FloatArray,
  val ambientRgb: FloatArray,
  val terrainRgb: FloatArray,
  val roadTintRgb: FloatArray,
  val mountainRgb: FloatArray,
  val wetness: Float,
  val rainIntensity: Float,
  val isNight: Boolean,
  val accentHex: Int,
) {
  COUNTRYSIDE(
    displayName = "Green Countryside",
    shortTag = "COUNTRYSIDE",
    skyTopRgb = floatArrayOf(0.16f, 0.48f, 0.88f),
    skyHorizonRgb = floatArrayOf(0.68f, 0.86f, 0.98f),
    fogRgb = floatArrayOf(0.66f, 0.83f, 0.94f),
    fogDensity = 0.0055f,
    sunDir = floatArrayOf(0.45f, 0.78f, -0.42f),
    sunColorRgb = floatArrayOf(1.0f, 0.96f, 0.86f),
    ambientRgb = floatArrayOf(0.36f, 0.42f, 0.40f),
    terrainRgb = floatArrayOf(0.18f, 0.48f, 0.16f),
    roadTintRgb = floatArrayOf(0.18f, 0.19f, 0.21f),
    mountainRgb = floatArrayOf(0.24f, 0.42f, 0.34f),
    wetness = 0.0f,
    rainIntensity = 0.0f,
    isNight = false,
    accentHex = 0xFF2ECC71.toInt(),
  ),
  CITY(
    displayName = "Modern City",
    shortTag = "CITY",
    skyTopRgb = floatArrayOf(0.20f, 0.50f, 0.84f),
    skyHorizonRgb = floatArrayOf(0.75f, 0.83f, 0.92f),
    fogRgb = floatArrayOf(0.70f, 0.78f, 0.88f),
    fogDensity = 0.0062f,
    sunDir = floatArrayOf(-0.40f, 0.82f, -0.40f),
    sunColorRgb = floatArrayOf(0.98f, 0.97f, 0.95f),
    ambientRgb = floatArrayOf(0.38f, 0.40f, 0.46f),
    terrainRgb = floatArrayOf(0.26f, 0.28f, 0.32f),
    roadTintRgb = floatArrayOf(0.16f, 0.17f, 0.20f),
    mountainRgb = floatArrayOf(0.35f, 0.42f, 0.52f),
    wetness = 0.12f,
    rainIntensity = 0.0f,
    isNight = false,
    accentHex = 0xFF38BDF8.toInt(),
  ),
  MOUNTAIN(
    displayName = "Mountain Pass",
    shortTag = "MOUNTAIN",
    skyTopRgb = floatArrayOf(0.11f, 0.38f, 0.78f),
    skyHorizonRgb = floatArrayOf(0.66f, 0.84f, 0.96f),
    fogRgb = floatArrayOf(0.64f, 0.79f, 0.90f),
    fogDensity = 0.0058f,
    sunDir = floatArrayOf(0.55f, 0.72f, -0.35f),
    sunColorRgb = floatArrayOf(1.0f, 0.95f, 0.88f),
    ambientRgb = floatArrayOf(0.32f, 0.38f, 0.44f),
    terrainRgb = floatArrayOf(0.22f, 0.38f, 0.22f),
    roadTintRgb = floatArrayOf(0.19f, 0.20f, 0.22f),
    mountainRgb = floatArrayOf(0.38f, 0.43f, 0.50f),
    wetness = 0.05f,
    rainIntensity = 0.0f,
    isNight = false,
    accentHex = 0xFFA78BFA.toInt(),
  ),
  DESERT(
    displayName = "Desert Highway",
    shortTag = "DESERT",
    skyTopRgb = floatArrayOf(0.22f, 0.52f, 0.82f),
    skyHorizonRgb = floatArrayOf(0.95f, 0.78f, 0.54f),
    fogRgb = floatArrayOf(0.90f, 0.74f, 0.54f),
    fogDensity = 0.0056f,
    sunDir = floatArrayOf(0.60f, 0.65f, -0.45f),
    sunColorRgb = floatArrayOf(1.0f, 0.88f, 0.68f),
    ambientRgb = floatArrayOf(0.46f, 0.38f, 0.28f),
    terrainRgb = floatArrayOf(0.78f, 0.58f, 0.34f),
    roadTintRgb = floatArrayOf(0.22f, 0.20f, 0.19f),
    mountainRgb = floatArrayOf(0.64f, 0.40f, 0.26f),
    wetness = 0.0f,
    rainIntensity = 0.0f,
    isNight = false,
    accentHex = 0xFFF59E0B.toInt(),
  ),
  NIGHT(
    displayName = "Night Highway",
    shortTag = "NIGHT",
    skyTopRgb = floatArrayOf(0.02f, 0.04f, 0.12f),
    skyHorizonRgb = floatArrayOf(0.08f, 0.12f, 0.26f),
    fogRgb = floatArrayOf(0.06f, 0.09f, 0.19f),
    fogDensity = 0.0085f,
    sunDir = floatArrayOf(-0.30f, 0.80f, -0.50f),
    sunColorRgb = floatArrayOf(0.34f, 0.44f, 0.70f),
    ambientRgb = floatArrayOf(0.14f, 0.17f, 0.26f),
    terrainRgb = floatArrayOf(0.05f, 0.08f, 0.12f),
    roadTintRgb = floatArrayOf(0.08f, 0.09f, 0.12f),
    mountainRgb = floatArrayOf(0.07f, 0.10f, 0.18f),
    wetness = 0.22f,
    rainIntensity = 0.0f,
    isNight = true,
    accentHex = 0xFF6366F1.toInt(),
  ),
  RAINY(
    displayName = "Rainy Highway",
    shortTag = "RAINY",
    skyTopRgb = floatArrayOf(0.14f, 0.19f, 0.26f),
    skyHorizonRgb = floatArrayOf(0.36f, 0.42f, 0.49f),
    fogRgb = floatArrayOf(0.32f, 0.38f, 0.45f),
    fogDensity = 0.0095f,
    sunDir = floatArrayOf(0.20f, 0.85f, -0.45f),
    sunColorRgb = floatArrayOf(0.62f, 0.68f, 0.78f),
    ambientRgb = floatArrayOf(0.24f, 0.28f, 0.34f),
    terrainRgb = floatArrayOf(0.12f, 0.26f, 0.15f),
    roadTintRgb = floatArrayOf(0.10f, 0.12f, 0.15f),
    mountainRgb = floatArrayOf(0.20f, 0.25f, 0.31f),
    wetness = 0.90f,
    rainIntensity = 1.0f,
    isNight = false,
    accentHex = 0xFF00E5FF.toInt(),
  ),
}

enum class GameMode(val displayName: String, val subtitle: String) {
  CAREER("Career Mode", "Complete 24 progressive missions & unlock tracks"),
  ENDLESS("Endless Highway", "Drive as far as possible without crashing"),
  TIME_CHALLENGE("Time Challenge", "Reach the checkpoint before the timer expires"),
  TRAFFIC_CHALLENGE("Traffic Challenge", "Overtake target vehicles without crashing"),
}

enum class MissionType {
  REACH_FINISH,
  TIME_TRIAL,
  OVERTAKE_COUNT,
  HIGH_SPEED_RUN,
}

data class CareerLevel(
  val levelNumber: Int,
  val title: String,
  val environment: EnvironmentType,
  val missionType: MissionType,
  val description: String,
  val targetDistanceMeters: Float,
  val timeLimitSeconds: Float,
  val targetOvertakes: Int,
  val targetMinAvgSpeedKmh: Float,
  val trafficDensity: Float, // 0.35f .. 1.0f
  val trafficSpeedFactor: Float, // 0.7f .. 1.15f
  val rewardCoins: Int,
)

object CareerCatalog {
  val levels: List<CareerLevel> = buildLevels()

  private fun buildLevels(): List<CareerLevel> {
    val envs = EnvironmentType.entries
    val specs =
      listOf(
        // Countryside (1-4)
        Triple("Sunlit Sprint", MissionType.REACH_FINISH, "Reach the 1.8 km highway finish line."),
        Triple("Morning Rush", MissionType.OVERTAKE_COUNT, "Overtake 12 cars before reaching 2.0 km."),
        Triple("Golden Clock", MissionType.TIME_TRIAL, "Beat the 42s timer across 2.2 km."),
        Triple("Countryside Blitz", MissionType.HIGH_SPEED_RUN, "Finish 2.5 km while hitting 200+ km/h."),
        // City (5-8)
        Triple("Metro Express", MissionType.REACH_FINISH, "Navigate downtown lanes for 2.4 km."),
        Triple("Gridlock Weave", MissionType.OVERTAKE_COUNT, "Overtake 18 vehicles in city traffic."),
        Triple("Downtown Dash", MissionType.TIME_TRIAL, "Beat the 46s timer across 2.8 km."),
        Triple("Skyline Velocity", MissionType.HIGH_SPEED_RUN, "Clear 3.0 km & reach 220+ km/h."),
        // Mountain (9-12)
        Triple("Alpine Climb", MissionType.REACH_FINISH, "Conquer the 2.8 km mountain pass."),
        Triple("Ridge Overtake", MissionType.OVERTAKE_COUNT, "Pass 22 vehicles along alpine lanes."),
        Triple("Summit Countdown", MissionType.TIME_TRIAL, "Cross 3.2 km in under 50 seconds."),
        Triple("Apex Elevation", MissionType.HIGH_SPEED_RUN, "Finish 3.4 km & hit 235+ km/h."),
        // Desert (13-16)
        Triple("Canyon Heat", MissionType.REACH_FINISH, "Cross 3.2 km of sun-baked desert asphalt."),
        Triple("Mirage Slalom", MissionType.OVERTAKE_COUNT, "Overtake 26 vehicles without a crash."),
        Triple("Dust Storm Run", MissionType.TIME_TRIAL, "Clear 3.6 km within 52 seconds."),
        Triple("Redline Horizon", MissionType.HIGH_SPEED_RUN, "Complete 3.8 km & reach 250+ km/h."),
        // Night (17-20)
        Triple("Neon Midnight", MissionType.REACH_FINISH, "Drive 3.5 km under city streetlamps."),
        Triple("Headlight Hunt", MissionType.OVERTAKE_COUNT, "Overtake 30 cars in night traffic."),
        Triple("Midnight Express", MissionType.TIME_TRIAL, "Reach 4.0 km before 54s expires."),
        Triple("Shadow Velocity", MissionType.HIGH_SPEED_RUN, "Finish 4.2 km & hit 265+ km/h."),
        // Rainy (21-24)
        Triple("Wet Asphalt", MissionType.REACH_FINISH, "Master slick rain lanes for 3.8 km."),
        Triple("Storm Surge", MissionType.OVERTAKE_COUNT, "Overtake 34 vehicles in heavy rain."),
        Triple("Thunder Clock", MissionType.TIME_TRIAL, "Beat 56 seconds across 4.4 km."),
        Triple("Extreme Finale", MissionType.HIGH_SPEED_RUN, "Conquer 5.0 km & reach 285+ km/h."),
      )

    return specs.mapIndexed { index, (title, missionType, desc) ->
      val levelNum = index + 1
      val env = envs[(index / 4) % envs.size]
      val progress = index / 23f
      val dist = 1800f + index * 140f
      val timeLimit =
        when (missionType) {
          MissionType.TIME_TRIAL -> (dist / (46f + progress * 24f)) + 6f
          else -> 0f
        }
      val overtakes =
        when (missionType) {
          MissionType.OVERTAKE_COUNT -> 12 + (index * 1.1f).toInt()
          else -> 0
        }
      val speedTarget =
        when (missionType) {
          MissionType.HIGH_SPEED_RUN -> 200f + (index / 4) * 16f
          else -> 0f
        }
      CareerLevel(
        levelNumber = levelNum,
        title = title,
        environment = env,
        missionType = missionType,
        description = desc,
        targetDistanceMeters = dist,
        timeLimitSeconds = timeLimit,
        targetOvertakes = overtakes,
        targetMinAvgSpeedKmh = speedTarget,
        trafficDensity = 0.40f + progress * 0.55f,
        trafficSpeedFactor = 0.78f + progress * 0.30f,
        rewardCoins = 250 + levelNum * 65,
      )
    }
  }

  fun findLevel(number: Int): CareerLevel =
    levels.firstOrNull { it.levelNumber == number } ?: levels.first()
}

enum class GraphicsQuality(val displayName: String, val fogScale: Float, val propStep: Int) {
  LOW("Low", 1.30f, 2),
  MEDIUM("Medium", 1.0f, 1),
  HIGH("High", 0.80f, 1),
}

data class CarUpgradeState(
  val engineLevel: Int = 1, // 1..5
  val handlingLevel: Int = 1, // 1..5
  val nitroLevel: Int = 1, // 1..5
  val colorHex: Int = 0xFFE62038.toInt(),
) {
  fun effectiveTopSpeed(spec: CarSpec): Float =
    spec.baseTopSpeedKmh * (1f + (engineLevel - 1) * 0.055f)

  fun effectiveAcceleration(spec: CarSpec): Float =
    (spec.baseAcceleration + (engineLevel - 1) * 0.055f).coerceAtMost(1.25f)

  fun effectiveHandling(spec: CarSpec): Float =
    (spec.baseHandling + (handlingLevel - 1) * 0.055f).coerceAtMost(1.25f)

  fun effectiveNitroPower(spec: CarSpec): Float =
    (spec.baseNitroPower + (nitroLevel - 1) * 0.06f).coerceAtMost(1.30f)

  fun upgradeCost(currentLevel: Int): Int = 300 * currentLevel * currentLevel
}

data class CloudSession(
  val userId: String,
  val email: String,
  val driverName: String,
  val accessToken: String,
  val refreshToken: String,
)

data class LeaderboardEntry(
  val id: String,
  val driverName: String,
  val levelsPassed: Int,
  val unlockedCareerLevel: Int,
  val totalStars: Int,
  val bestEndlessMeters: Float,
  val highestSpeedKmh: Float,
  val totalCoins: Int,
  val selectedCar: String,
  val totalRacesPlayed: Int,
)

data class PlayerSaveData(
  val coins: Int = 500,
  val selectedCarId: String = "apex_gt",
  val unlockedCarIds: Set<String> = setOf("apex_gt"),
  val carUpgrades: Map<String, CarUpgradeState> =
    CarCatalog.cars.associate { it.id to CarUpgradeState(colorHex = it.defaultColorHex) },
  val unlockedCareerLevel: Int = 1,
  val levelStars: Map<Int, Int> = emptyMap(),
  val bestEndlessMeters: Float = 0f,
  val bestTimeChallengeSeconds: Float = 0f,
  val bestTrafficOvertakes: Int = 0,
  val highestSpeedKmh: Float = 0f,
  val totalRacesPlayed: Int = 0,
  val soundEnabled: Boolean = true,
  val musicEnabled: Boolean = true,
  val tiltSteeringEnabled: Boolean = false,
  val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
  val cloudSession: CloudSession? = null,
) {
  val levelsPassedCount: Int
    get() = levelStars.count { it.value > 0 }

  fun upgradeFor(carId: String): CarUpgradeState =
    carUpgrades[carId] ?: CarUpgradeState(colorHex = CarCatalog.findById(carId).defaultColorHex)
}

data class RaceConfig(
  val mode: GameMode,
  val environment: EnvironmentType,
  val careerLevel: CareerLevel? = null,
  val carSpec: CarSpec,
  val upgradeState: CarUpgradeState,
  val graphicsQuality: GraphicsQuality,
)

data class RaceHudState(
  val speedKmh: Float = 0f,
  val maxSpeedKmh: Float = 0f,
  val distanceMeters: Float = 0f,
  val targetDistanceMeters: Float = 0f,
  val elapsedSeconds: Float = 0f,
  val timeRemainingSeconds: Float = 0f,
  val overtakes: Int = 0,
  val targetOvertakes: Int = 0,
  val nitroEnergy: Float = 1f,
  val isNitroActive: Boolean = false,
  val isBraking: Boolean = false,
  val coinsEarned: Int = 0,
  val calloutText: String? = null,
  val countdownValue: Int = 3,
  val isPaused: Boolean = false,
  val isCrashing: Boolean = false,
  val progressFraction: Float = 0f,
  val missionSummary: String = "",
)

data class RaceResult(
  val isVictory: Boolean,
  val mode: GameMode,
  val environment: EnvironmentType,
  val careerLevelNumber: Int?,
  val distanceMeters: Float,
  val maxSpeedKmh: Float,
  val overtakes: Int,
  val elapsedSeconds: Float,
  val coinsEarned: Int,
  val starsEarned: Int,
  val failureReason: String? = null,
)
