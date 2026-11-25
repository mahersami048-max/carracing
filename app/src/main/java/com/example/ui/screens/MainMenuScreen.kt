package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CarCatalog
import com.example.model.PlayerSaveData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainMenuScreen(
  saveData: PlayerSaveData,
  onPlayGame: () -> Unit,
  onCareerMode: () -> Unit,
  onEndlessMode: () -> Unit,
  onGarage: () -> Unit,
  onLeaderboard: () -> Unit,
  onSettings: () -> Unit,
  onExit: () -> Unit,
) {
  val activeCar = CarCatalog.findById(saveData.selectedCarId)
  val totalStars = saveData.levelStars.values.sum()

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.horizontalGradient(
            listOf(
              CarbonBlack.copy(alpha = 0.86f),
              CarbonBlack.copy(alpha = 0.30f),
              CarbonBlack.copy(alpha = 0.15f),
            )
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 20.dp, vertical = 14.dp)
  ) {
    // Top Header Bar
    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "HIGHWAY RUSH",
          style = MaterialTheme.typography.displayMedium,
          color = TextPrimary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .width(24.dp)
                .height(3.dp)
                .background(RacingCrimson)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.game_subtitle),
            style = MaterialTheme.typography.labelLarge,
            color = NitroCyan,
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AsphaltSurface.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.EmojiEvents,
              contentDescription = "Career Stars",
              tint = NitroCyan,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "LVL ${saveData.unlockedCareerLevel}/24  •  ★ $totalStars",
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
            )
          }
        }
        CoinBadge(coins = saveData.coins)
      }
    }

    // Left Navigation Column
    Column(
      modifier = Modifier.width(280.dp).fillMaxHeight().padding(top = 64.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      MotorsportActionButton(
        text = stringResource(R.string.menu_play_game),
        icon = Icons.Filled.PlayArrow,
        onClick = onPlayGame,
        containerColor = RacingCrimson,
        modifier = Modifier.fillMaxWidth(),
        testTag = "play_game_button",
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MotorsportActionButton(
          text = stringResource(R.string.menu_career),
          icon = Icons.Filled.EmojiEvents,
          onClick = onCareerMode,
          containerColor = ElevatedSlate,
          modifier = Modifier.weight(1f),
          testTag = "career_mode_button",
        )
        MotorsportActionButton(
          text = stringResource(R.string.menu_endless),
          icon = Icons.Filled.AllInclusive,
          onClick = onEndlessMode,
          containerColor = ElevatedSlate,
          modifier = Modifier.weight(1f),
          testTag = "endless_mode_button",
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        MotorsportActionButton(
          text = stringResource(R.string.menu_garage),
          icon = Icons.Filled.DirectionsCar,
          onClick = onGarage,
          containerColor = NitroCyan,
          contentColor = CarbonBlack,
          modifier = Modifier.weight(1f),
          testTag = "garage_button",
        )
        MotorsportActionButton(
          text = saveData.cloudSession?.driverName ?: "RANKINGS",
          onClick = onLeaderboard,
          containerColor = ElevatedSlate,
          contentColor = NitroCyan,
          modifier = Modifier.weight(1f),
          testTag = "leaderboard_button",
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onSettings,
          modifier = Modifier.weight(1f).height(48.dp).testTag("settings_button"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.menu_settings),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
          )
        }
        OutlinedButton(
          onClick = onExit,
          modifier = Modifier.height(48.dp).testTag("exit_button"),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = stringResource(R.string.menu_exit),
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // Bottom-Right Selected Vehicle Pill over 3D Showroom
    Surface(
      modifier = Modifier.align(Alignment.BottomEnd),
      shape = RoundedCornerShape(12.dp),
      color = AsphaltSurface.copy(alpha = 0.85f),
      border = BorderStroke(1.dp, PanelBorder),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column {
          Text(
            text = activeCar.category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NitroCyan,
          )
          Text(
            text = activeCar.name,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
          )
        }
        Box(
          modifier =
            Modifier
              .width(1.dp)
              .height(28.dp)
              .background(PanelBorder)
        )
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "TOP SPEED",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
          Text(
            text = "${saveData.upgradeFor(activeCar.id).effectiveTopSpeed(activeCar).toInt()} KM/H",
            style = MaterialTheme.typography.titleMedium,
            color = AmberGold,
          )
        }
      }
    }
  }
}
