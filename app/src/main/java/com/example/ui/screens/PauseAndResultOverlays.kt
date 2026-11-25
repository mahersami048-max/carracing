package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CareerCatalog
import com.example.model.GameMode
import com.example.model.PlayerSaveData
import com.example.model.RaceResult
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldBoost
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PauseOverlay(
  saveData: PlayerSaveData,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onToggleSound: () -> Unit,
  onMainMenu: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.78f))
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.width(340.dp),
      shape = RoundedCornerShape(16.dp),
      color = AsphaltSurface,
      border = BorderStroke(1.5.dp, PanelBorder),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "RACE PAUSED",
          style = MaterialTheme.typography.headlineLarge,
          color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        MotorsportActionButton(
          text = "RESUME RACE",
          icon = Icons.Filled.PlayArrow,
          containerColor = EmeraldBoost,
          contentColor = CarbonBlack,
          onClick = onResume,
          modifier = Modifier.fillMaxWidth(),
          testTag = "pause_resume_button",
        )
        MotorsportActionButton(
          text = "RESTART",
          icon = Icons.Filled.Refresh,
          containerColor = ElevatedSlate,
          onClick = onRestart,
          modifier = Modifier.fillMaxWidth(),
          testTag = "pause_restart_button",
        )
        OutlinedButton(
          onClick = onToggleSound,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_sound_button"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Icon(
            imageVector = if (saveData.soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            contentDescription = null,
            tint = NitroCyan,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (saveData.soundEnabled) "SOUND: ON" else "SOUND: OFF",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
          )
        }
        OutlinedButton(
          onClick = onMainMenu,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_menu_button"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, RacingCrimson.copy(alpha = 0.65f)),
        ) {
          Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            tint = RacingCrimson,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "MAIN MENU",
            style = MaterialTheme.typography.labelLarge,
            color = RacingCrimson,
          )
        }
      }
    }
  }
}

@Composable
fun ResultsScreen(
  result: RaceResult,
  onNextLevel: (Int) -> Unit,
  onRetry: () -> Unit,
  onMainMenu: () -> Unit,
) {
  BackHandler(onBack = onMainMenu)

  val hasNextLevel =
    result.isVictory &&
      result.mode == GameMode.CAREER &&
      result.careerLevelNumber != null &&
      result.careerLevelNumber < CareerCatalog.levels.size

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.88f))
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.width(460.dp),
      shape = RoundedCornerShape(18.dp),
      color = AsphaltSurface,
      border =
        BorderStroke(
          1.5.dp,
          if (result.isVictory) EmeraldBoost else RacingCrimson,
        ),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text =
            when {
              result.isVictory -> "MISSION COMPLETED!"
              result.mode == GameMode.ENDLESS -> "RUN FINISHED"
              else -> "RACE FAILED"
            },
          style = MaterialTheme.typography.displayMedium,
          color = if (result.isVictory) EmeraldBoost else if (result.mode == GameMode.ENDLESS) NitroCyan else RacingCrimson,
        )

        if (!result.failureReason.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = result.failureReason,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
          )
        }

        // Stars Row
        if (result.mode == GameMode.CAREER || result.mode == GameMode.TIME_CHALLENGE) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in 1..3) {
              Icon(
                imageVector = if (s <= result.starsEarned) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Star $s",
                tint = if (s <= result.starsEarned) AmberGold else TextMuted,
                modifier = Modifier.size(34.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ResultStatTile(
            label = "DISTANCE",
            value = "%.2f km".format(result.distanceMeters / 1000f),
            modifier = Modifier.weight(1f),
          )
          ResultStatTile(
            label = "MAX SPEED",
            value = "${result.maxSpeedKmh.toInt()} km/h",
            modifier = Modifier.weight(1f),
          )
          ResultStatTile(
            label = "OVERTAKES",
            value = "${result.overtakes}",
            modifier = Modifier.weight(1f),
          )
          ResultStatTile(
            label = "COINS",
            value = "+${result.coinsEarned}",
            valueColor = AmberGold,
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier.weight(1f).height(48.dp).testTag("results_menu_button"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, PanelBorder),
          ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MENU", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
          }

          MotorsportActionButton(
            text = "RETRY",
            icon = Icons.Filled.Refresh,
            containerColor = ElevatedSlate,
            onClick = onRetry,
            modifier = Modifier.weight(1f),
            testTag = "results_retry_button",
          )

          if (hasNextLevel) {
            MotorsportActionButton(
              text = "NEXT LEVEL",
              icon = Icons.Filled.SkipNext,
              containerColor = EmeraldBoost,
              contentColor = CarbonBlack,
              onClick = { onNextLevel(result.careerLevelNumber!!) },
              modifier = Modifier.weight(1.25f),
              testTag = "results_next_level_button",
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ResultStatTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = ElevatedSlate,
    border = BorderStroke(1.dp, PanelBorder),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Spacer(modifier = Modifier.height(3.dp))
      Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
  }
}
