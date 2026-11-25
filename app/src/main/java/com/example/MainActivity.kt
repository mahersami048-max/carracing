package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.GameSurfaceView
import com.example.model.GameMode
import com.example.ui.screens.AccountAndLeaderboardScreen
import com.example.ui.screens.CareerMapScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.ModeSelectScreen
import com.example.ui.screens.PauseOverlay
import com.example.ui.screens.RaceHudOverlay
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HighwayRushTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
  private val gameViewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    hideSystemBars()

    setContent {
      HighwayRushTheme {
        HighwayRushApp(
          viewModel = gameViewModel,
          onExitApp = { finish() },
        )
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemBars()
  }

  override fun onPause() {
    super.onPause()
    gameViewModel.onAppPause()
  }

  override fun onResume() {
    super.onResume()
    gameViewModel.onAppResume()
  }

  private fun hideSystemBars() {
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
  }
}

@Composable
fun HighwayRushApp(
  viewModel: GameViewModel,
  onExitApp: () -> Unit,
) {
  val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val saveData by viewModel.saveData.collectAsStateWithLifecycle()
  val hudState by viewModel.hudState.collectAsStateWithLifecycle()
  val garageCarId by viewModel.garagePreviewCarId.collectAsStateWithLifecycle()
  val cloudState by viewModel.cloudUiState.collectAsStateWithLifecycle()
  var modeSelectInitial by remember { mutableStateOf(GameMode.ENDLESS) }

  Box(modifier = Modifier.fillMaxSize()) {
    // Persistent 3D OpenGL ES Scene (Showroom turntable or active 3D highway racing)
    GameSurfaceView(
      renderer = viewModel.renderer,
      allowTurntableDrag = screen == AppScreen.Garage,
      modifier = Modifier.fillMaxSize(),
    )

    when (val current = screen) {
      AppScreen.MainMenu -> {
        MainMenuScreen(
          saveData = saveData,
          onPlayGame = {
            modeSelectInitial = GameMode.TIME_CHALLENGE
            viewModel.navigateTo(AppScreen.ModeSelect)
          },
          onCareerMode = { viewModel.navigateTo(AppScreen.CareerMap) },
          onEndlessMode = {
            modeSelectInitial = GameMode.ENDLESS
            viewModel.navigateTo(AppScreen.ModeSelect)
          },
          onGarage = { viewModel.navigateTo(AppScreen.Garage) },
          onLeaderboard = { viewModel.navigateTo(AppScreen.Leaderboard) },
          onSettings = { viewModel.navigateTo(AppScreen.Settings) },
          onExit = onExitApp,
        )
      }

      AppScreen.ModeSelect -> {
        ModeSelectScreen(
          saveData = saveData,
          initialMode = modeSelectInitial,
          onStartMode = { mode, env -> viewModel.startFreeMode(mode, env) },
          onOpenCareer = { viewModel.navigateTo(AppScreen.CareerMap) },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }

      AppScreen.CareerMap -> {
        CareerMapScreen(
          saveData = saveData,
          onLaunchLevel = { level -> viewModel.startCareerLevel(level) },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }

      AppScreen.Garage -> {
        GarageScreen(
          saveData = saveData,
          previewCarId = garageCarId,
          onSelectPreviewCar = { viewModel.previewCarInGarage(it) },
          onUnlockOrSelectCar = { viewModel.unlockOrSelectCar(it) },
          onUpgradeStat = { carId, stat -> viewModel.upgradeCarStat(carId, stat) },
          onSelectPaint = { carId, hex -> viewModel.setCarPaintColor(carId, hex) },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }

      AppScreen.Settings -> {
        SettingsScreen(
          saveData = saveData,
          isTiltSupported = viewModel.isTiltSupported,
          onSoundChanged = { viewModel.setSoundEnabled(it) },
          onMusicChanged = { viewModel.setMusicEnabled(it) },
          onTiltChanged = { viewModel.setTiltSteeringEnabled(it) },
          onGraphicsQualityChanged = { viewModel.setGraphicsQuality(it) },
          onResetProgress = { viewModel.resetAllProgress() },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }

      AppScreen.Leaderboard -> {
        AccountAndLeaderboardScreen(
          saveData = saveData,
          cloudState = cloudState,
          onSignUp = { email, pass, name -> viewModel.signUpCloudAccount(email, pass, name) },
          onSignIn = { email, pass -> viewModel.signInCloudAccount(email, pass) },
          onSignOut = { viewModel.signOutCloudAccount() },
          onSyncNow = { viewModel.syncCloudNow() },
          onRefreshLeaderboard = { viewModel.refreshLeaderboard() },
          onBack = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }

      AppScreen.Racing -> {
        val simConfig = viewModel.renderer.activeSimulation?.config
        RaceHudOverlay(
          hud = hudState,
          mode = simConfig?.mode ?: GameMode.ENDLESS,
          levelNumber = simConfig?.careerLevel?.levelNumber,
          inputState = viewModel.inputState,
          onTogglePause = { viewModel.togglePause() },
        )
        if (hudState.isPaused) {
          PauseOverlay(
            saveData = saveData,
            onResume = { viewModel.setPaused(false) },
            onRestart = { viewModel.retryLastRace() },
            onToggleSound = { viewModel.setSoundEnabled(!saveData.soundEnabled) },
            onMainMenu = { viewModel.navigateTo(AppScreen.MainMenu) },
          )
        }
      }

      is AppScreen.Results -> {
        ResultsScreen(
          result = current.result,
          onNextLevel = { lvlNum -> viewModel.startNextCareerLevel(lvlNum) },
          onRetry = { viewModel.retryLastRace() },
          onMainMenu = { viewModel.navigateTo(AppScreen.MainMenu) },
        )
      }
    }
  }
}
