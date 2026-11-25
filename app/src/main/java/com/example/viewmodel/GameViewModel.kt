package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.CloudAuthOutcome
import com.example.data.LeaderboardOutcome
import com.example.data.SaveRepository
import com.example.data.SupabaseClient
import com.example.engine.RaceGlRenderer
import com.example.engine.RaceSimulation
import com.example.input.RaceInputState
import com.example.input.TiltSteeringManager
import com.example.model.CarCatalog
import com.example.model.CareerCatalog
import com.example.model.CareerLevel
import com.example.model.EnvironmentType
import com.example.model.GameMode
import com.example.model.GraphicsQuality
import com.example.model.LeaderboardEntry
import com.example.model.PlayerSaveData
import com.example.model.RaceConfig
import com.example.model.RaceHudState
import com.example.model.RaceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppScreen {
  data object MainMenu : AppScreen
  data object ModeSelect : AppScreen
  data object CareerMap : AppScreen
  data object Garage : AppScreen
  data object Settings : AppScreen
  data object Leaderboard : AppScreen
  data object Racing : AppScreen
  data class Results(val result: RaceResult) : AppScreen
}

enum class UpgradeStat {
  ENGINE,
  HANDLING,
  NITRO,
}

data class CloudUiState(
  val isLoading: Boolean = false,
  val statusMessage: String? = null,
  val isError: Boolean = false,
  val leaderboard: List<LeaderboardEntry> = emptyList(),
  val totalPlayers: Int = 0,
  val isConfigured: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
  private val saveRepo = SaveRepository(application)
  private val supabaseClient = SupabaseClient()
  val soundManager = SoundManager()
  val inputState = RaceInputState()
  val tiltManager = TiltSteeringManager(application, inputState)
  val renderer = RaceGlRenderer()

  private val _saveData = MutableStateFlow(PlayerSaveData())
  val saveData: StateFlow<PlayerSaveData> = _saveData.asStateFlow()

  private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.MainMenu)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _hudState = MutableStateFlow(RaceHudState())
  val hudState: StateFlow<RaceHudState> = _hudState.asStateFlow()

  private val _garagePreviewCarId = MutableStateFlow("apex_gt")
  val garagePreviewCarId: StateFlow<String> = _garagePreviewCarId.asStateFlow()

  private val _cloudUiState =
    MutableStateFlow(CloudUiState(isConfigured = supabaseClient.isConfigured))
  val cloudUiState: StateFlow<CloudUiState> = _cloudUiState.asStateFlow()

  private var lastRaceConfig: RaceConfig? = null

  val isTiltSupported: Boolean
    get() = tiltManager.isSupported

  init {
    soundManager.start()
    viewModelScope.launch {
      saveRepo.saveDataFlow.collect { data ->
        _saveData.value = data
        soundManager.soundEnabled = data.soundEnabled
        soundManager.musicEnabled = data.musicEnabled
        renderer.graphicsQuality = data.graphicsQuality
        if (_currentScreen.value != AppScreen.Garage) {
          _garagePreviewCarId.value = data.selectedCarId
        }
        syncShowroomCar(data)
      }
    }
  }

  private fun syncShowroomCar(data: PlayerSaveData) {
    val displayId =
      if (_currentScreen.value == AppScreen.Garage) _garagePreviewCarId.value else data.selectedCarId
    val spec = CarCatalog.findById(displayId)
    val upgrade = data.upgradeFor(displayId)
    renderer.showroomCarType = spec.carType
    renderer.showroomColorHex = upgrade.colorHex
  }

  fun navigateTo(screen: AppScreen) {
    soundManager.playClick()
    if (screen != AppScreen.Racing) {
      soundManager.engineActive = false
      tiltManager.setEnabled(false)
      inputState.reset()
      renderer.renderMode = RaceGlRenderer.RenderMode.SHOWROOM
      renderer.activeSimulation = null
    }
    if (screen == AppScreen.Garage) {
      _garagePreviewCarId.value = _saveData.value.selectedCarId
      renderer.showroomAutoRotate = false
    } else if (screen == AppScreen.MainMenu) {
      renderer.showroomAutoRotate = true
    } else if (screen == AppScreen.Leaderboard) {
      refreshLeaderboard()
    }
    _currentScreen.value = screen
    syncShowroomCar(_saveData.value)
  }

  fun startCareerLevel(level: CareerLevel) {
    val save = _saveData.value
    val carSpec = CarCatalog.findById(save.selectedCarId)
    val upgrade = save.upgradeFor(carSpec.id)
    launchRace(
      RaceConfig(
        mode = GameMode.CAREER,
        environment = level.environment,
        careerLevel = level,
        carSpec = carSpec,
        upgradeState = upgrade,
        graphicsQuality = save.graphicsQuality,
      )
    )
  }

  fun startFreeMode(mode: GameMode, environment: EnvironmentType) {
    val save = _saveData.value
    val carSpec = CarCatalog.findById(save.selectedCarId)
    val upgrade = save.upgradeFor(carSpec.id)
    launchRace(
      RaceConfig(
        mode = mode,
        environment = environment,
        careerLevel = null,
        carSpec = carSpec,
        upgradeState = upgrade,
        graphicsQuality = save.graphicsQuality,
      )
    )
  }

  fun retryLastRace() {
    val cfg = lastRaceConfig ?: return
    val save = _saveData.value
    val carSpec = CarCatalog.findById(save.selectedCarId)
    launchRace(
      cfg.copy(
        carSpec = carSpec,
        upgradeState = save.upgradeFor(carSpec.id),
        graphicsQuality = save.graphicsQuality,
      )
    )
  }

  fun startNextCareerLevel(currentLevelNumber: Int) {
    val nextNum = (currentLevelNumber + 1).coerceAtMost(CareerCatalog.levels.size)
    startCareerLevel(CareerCatalog.findLevel(nextNum))
  }

  private fun launchRace(config: RaceConfig) {
    soundManager.playClick()
    lastRaceConfig = config
    inputState.reset()
    _hudState.value = RaceHudState()

    val sim =
      RaceSimulation(
        config = config,
        input = inputState,
        soundManager = soundManager,
        onHudUpdate = { hud -> _hudState.value = hud },
        onRaceFinished = { result -> onRaceComplete(result) },
      )
    renderer.activeSimulation = sim
    renderer.graphicsQuality = config.graphicsQuality
    renderer.renderMode = RaceGlRenderer.RenderMode.RACING
    tiltManager.setEnabled(_saveData.value.tiltSteeringEnabled)
    _currentScreen.value = AppScreen.Racing
  }

  fun togglePause() {
    val sim = renderer.activeSimulation ?: return
    soundManager.playClick()
    val nextPause = !sim.isPaused
    sim.isPaused = nextPause
    if (nextPause) {
      inputState.reset()
      soundManager.engineActive = false
    }
    _hudState.value = _hudState.value.copy(isPaused = nextPause)
  }

  fun setPaused(paused: Boolean) {
    val sim = renderer.activeSimulation ?: return
    if (sim.isPaused == paused) return
    sim.isPaused = paused
    if (paused) {
      inputState.reset()
      soundManager.engineActive = false
    }
    _hudState.value = _hudState.value.copy(isPaused = paused)
  }

  private fun onRaceComplete(result: RaceResult) {
    tiltManager.setEnabled(false)
    inputState.reset()
    soundManager.engineActive = false
    renderer.renderMode = RaceGlRenderer.RenderMode.SHOWROOM
    renderer.activeSimulation = null

    viewModelScope.launch {
      saveRepo.updateSave { cur ->
        var nextUnlocked = cur.unlockedCareerLevel
        val starsMap = cur.levelStars.toMutableMap()
        if (result.isVictory && result.careerLevelNumber != null) {
          val prevStars = starsMap[result.careerLevelNumber] ?: 0
          if (result.starsEarned > prevStars) {
            starsMap[result.careerLevelNumber] = result.starsEarned
          }
          if (result.careerLevelNumber >= nextUnlocked && nextUnlocked < CareerCatalog.levels.size) {
            nextUnlocked = result.careerLevelNumber + 1
          }
        }

        val bestEndless =
          if (result.mode == GameMode.ENDLESS && result.distanceMeters > cur.bestEndlessMeters) {
            result.distanceMeters
          } else {
            cur.bestEndlessMeters
          }
        val bestTime =
          if (
            result.mode == GameMode.TIME_CHALLENGE &&
              result.isVictory &&
              (cur.bestTimeChallengeSeconds <= 0f || result.elapsedSeconds < cur.bestTimeChallengeSeconds)
          ) {
            result.elapsedSeconds
          } else {
            cur.bestTimeChallengeSeconds
          }
        val bestTraffic =
          if (
            result.mode == GameMode.TRAFFIC_CHALLENGE &&
              result.overtakes > cur.bestTrafficOvertakes
          ) {
            result.overtakes
          } else {
            cur.bestTrafficOvertakes
          }

        val topSpeedEver = maxOf(cur.highestSpeedKmh, result.maxSpeedKmh)
        cur.copy(
          coins = cur.coins + result.coinsEarned,
          unlockedCareerLevel = nextUnlocked,
          levelStars = starsMap,
          bestEndlessMeters = bestEndless,
          bestTimeChallengeSeconds = bestTime,
          bestTrafficOvertakes = bestTraffic,
          highestSpeedKmh = topSpeedEver,
          totalRacesPlayed = cur.totalRacesPlayed + 1,
        )
      }
      _currentScreen.value = AppScreen.Results(result)

      // Automatically sync completed race & updated career/scores to Supabase if signed in
      val updatedSave = _saveData.value
      val session = updatedSave.cloudSession
      if (session != null && supabaseClient.isConfigured) {
        val carName = CarCatalog.findById(updatedSave.selectedCarId).name
        val scoreRes = supabaseClient.insertRaceScore(session, carName, result)
        val activeSess = scoreRes.getOrNull() ?: session
        val profRes = supabaseClient.upsertPlayerProfile(activeSess, updatedSave)
        val finalSess = profRes.getOrNull()
        if (finalSess != null && finalSess != session) {
          saveRepo.updateSave { it.copy(cloudSession = finalSess) }
        }
      }
    }
  }

  // Garage Actions
  fun previewCarInGarage(carId: String) {
    soundManager.playClick()
    _garagePreviewCarId.value = carId
    val spec = CarCatalog.findById(carId)
    val upgrade = _saveData.value.upgradeFor(carId)
    renderer.showroomCarType = spec.carType
    renderer.showroomColorHex = upgrade.colorHex
    if (_saveData.value.unlockedCarIds.contains(carId)) {
      viewModelScope.launch {
        saveRepo.updateSave { it.copy(selectedCarId = carId) }
      }
    }
  }

  fun unlockOrSelectCar(carId: String) {
    val spec = CarCatalog.findById(carId)
    val save = _saveData.value
    if (save.unlockedCarIds.contains(carId)) {
      soundManager.playClick()
      viewModelScope.launch { saveRepo.updateSave { it.copy(selectedCarId = carId) } }
      return
    }
    if (save.coins >= spec.price) {
      soundManager.playCoin()
      viewModelScope.launch {
        saveRepo.updateSave { cur ->
          if (cur.coins >= spec.price && !cur.unlockedCarIds.contains(carId)) {
            cur.copy(
              coins = cur.coins - spec.price,
              unlockedCarIds = cur.unlockedCarIds + carId,
              selectedCarId = carId,
            )
          } else {
            cur
          }
        }
      }
    }
  }

  fun upgradeCarStat(carId: String, stat: UpgradeStat) {
    val save = _saveData.value
    if (!save.unlockedCarIds.contains(carId)) return
    val currentUpgrade = save.upgradeFor(carId)
    val curLvl =
      when (stat) {
        UpgradeStat.ENGINE -> currentUpgrade.engineLevel
        UpgradeStat.HANDLING -> currentUpgrade.handlingLevel
        UpgradeStat.NITRO -> currentUpgrade.nitroLevel
      }
    if (curLvl >= 5) return
    val cost = currentUpgrade.upgradeCost(curLvl)
    if (save.coins < cost) return

    soundManager.playCoin()
    viewModelScope.launch {
      saveRepo.updateSave { cur ->
        val upg = cur.upgradeFor(carId)
        val lvl =
          when (stat) {
            UpgradeStat.ENGINE -> upg.engineLevel
            UpgradeStat.HANDLING -> upg.handlingLevel
            UpgradeStat.NITRO -> upg.nitroLevel
          }
        val actualCost = upg.upgradeCost(lvl)
        if (lvl >= 5 || cur.coins < actualCost) return@updateSave cur
        val updatedUpg =
          when (stat) {
            UpgradeStat.ENGINE -> upg.copy(engineLevel = lvl + 1)
            UpgradeStat.HANDLING -> upg.copy(handlingLevel = lvl + 1)
            UpgradeStat.NITRO -> upg.copy(nitroLevel = lvl + 1)
          }
        cur.copy(
          coins = cur.coins - actualCost,
          carUpgrades = cur.carUpgrades + (carId to updatedUpg),
        )
      }
    }
  }

  fun setCarPaintColor(carId: String, colorHex: Int) {
    if (!_saveData.value.unlockedCarIds.contains(carId)) return
    soundManager.playClick()
    renderer.showroomColorHex = colorHex
    viewModelScope.launch {
      saveRepo.updateSave { cur ->
        val upg = cur.upgradeFor(carId).copy(colorHex = colorHex)
        cur.copy(carUpgrades = cur.carUpgrades + (carId to upg))
      }
    }
  }

  // Settings Actions
  fun setSoundEnabled(enabled: Boolean) {
    soundManager.soundEnabled = enabled
    if (enabled) soundManager.playClick()
    viewModelScope.launch { saveRepo.updateSave { it.copy(soundEnabled = enabled) } }
  }

  fun setMusicEnabled(enabled: Boolean) {
    soundManager.playClick()
    soundManager.musicEnabled = enabled
    viewModelScope.launch { saveRepo.updateSave { it.copy(musicEnabled = enabled) } }
  }

  fun setTiltSteeringEnabled(enabled: Boolean) {
    soundManager.playClick()
    viewModelScope.launch { saveRepo.updateSave { it.copy(tiltSteeringEnabled = enabled) } }
  }

  fun setGraphicsQuality(quality: GraphicsQuality) {
    soundManager.playClick()
    renderer.graphicsQuality = quality
    viewModelScope.launch { saveRepo.updateSave { it.copy(graphicsQuality = quality) } }
  }

  fun resetAllProgress() {
    soundManager.playClick()
    viewModelScope.launch { saveRepo.resetProgress() }
  }

  // Cloud Account & Leaderboard Actions
  fun clearCloudMessage() {
    _cloudUiState.value = _cloudUiState.value.copy(statusMessage = null, isError = false)
  }

  fun signUpCloudAccount(email: String, password: String, driverName: String) {
    soundManager.playClick()
    viewModelScope.launch {
      _cloudUiState.value =
        _cloudUiState.value.copy(isLoading = true, statusMessage = null, isError = false)
      when (val outcome = supabaseClient.signUp(email, password, driverName)) {
        is CloudAuthOutcome.Authenticated -> {
          saveRepo.updateSave { it.copy(cloudSession = outcome.session) }
          val synced = supabaseClient.upsertPlayerProfile(outcome.session, _saveData.value)
          synced.getOrNull()?.let { sess ->
            if (sess != outcome.session) saveRepo.updateSave { it.copy(cloudSession = sess) }
          }
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = "Account created & synced to Supabase!",
              isError = false,
            )
          refreshLeaderboard()
        }
        is CloudAuthOutcome.ConfirmationEmailSent -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = outcome.message,
              isError = false,
            )
        }
        is CloudAuthOutcome.Error -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = outcome.message,
              isError = true,
            )
        }
      }
    }
  }

  fun signInCloudAccount(email: String, password: String) {
    soundManager.playClick()
    viewModelScope.launch {
      _cloudUiState.value =
        _cloudUiState.value.copy(isLoading = true, statusMessage = null, isError = false)
      when (val outcome = supabaseClient.signIn(email, password)) {
        is CloudAuthOutcome.Authenticated -> {
          saveRepo.updateSave { it.copy(cloudSession = outcome.session) }
          val synced = supabaseClient.upsertPlayerProfile(outcome.session, _saveData.value)
          synced.getOrNull()?.let { sess ->
            if (sess != outcome.session) saveRepo.updateSave { it.copy(cloudSession = sess) }
          }
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = "Signed in & stats synced!",
              isError = false,
            )
          refreshLeaderboard()
        }
        is CloudAuthOutcome.ConfirmationEmailSent -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = outcome.message,
              isError = false,
            )
        }
        is CloudAuthOutcome.Error -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = outcome.message,
              isError = true,
            )
        }
      }
    }
  }

  fun signOutCloudAccount() {
    soundManager.playClick()
    viewModelScope.launch {
      saveRepo.updateSave { it.copy(cloudSession = null) }
      _cloudUiState.value =
        _cloudUiState.value.copy(statusMessage = "Signed out.", isError = false)
    }
  }

  fun syncCloudNow() {
    soundManager.playClick()
    val session = _saveData.value.cloudSession ?: return
    viewModelScope.launch {
      _cloudUiState.value =
        _cloudUiState.value.copy(isLoading = true, statusMessage = null, isError = false)
      val res = supabaseClient.upsertPlayerProfile(session, _saveData.value)
      res.onSuccess { updatedSess ->
        if (updatedSess != session) {
          saveRepo.updateSave { it.copy(cloudSession = updatedSess) }
        }
        _cloudUiState.value =
          _cloudUiState.value.copy(
            isLoading = false,
            statusMessage = "Progress synced to Supabase!",
            isError = false,
          )
        refreshLeaderboard()
      }
      res.onFailure { err ->
        _cloudUiState.value =
          _cloudUiState.value.copy(
            isLoading = false,
            statusMessage = err.message ?: "Sync failed.",
            isError = true,
          )
      }
    }
  }

  fun refreshLeaderboard() {
    viewModelScope.launch {
      _cloudUiState.value = _cloudUiState.value.copy(isLoading = true)
      when (
        val outcome = supabaseClient.fetchLeaderboard(_saveData.value.cloudSession?.accessToken)
      ) {
        is LeaderboardOutcome.Success -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              leaderboard = outcome.entries,
              totalPlayers = outcome.totalPlayers,
            )
        }
        is LeaderboardOutcome.Error -> {
          _cloudUiState.value =
            _cloudUiState.value.copy(
              isLoading = false,
              statusMessage = outcome.message,
              isError = true,
            )
        }
      }
    }
  }

  fun onAppPause() {
    soundManager.setAppPaused(true)
    if (_currentScreen.value == AppScreen.Racing) {
      setPaused(true)
    }
    tiltManager.setEnabled(false)
  }

  fun onAppResume() {
    soundManager.setAppPaused(false)
    if (_currentScreen.value == AppScreen.Racing && _saveData.value.tiltSteeringEnabled) {
      tiltManager.setEnabled(true)
    }
  }

  override fun onCleared() {
    tiltManager.setEnabled(false)
    soundManager.release()
    super.onCleared()
  }
}
