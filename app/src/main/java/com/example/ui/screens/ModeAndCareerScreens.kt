package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CareerCatalog
import com.example.model.CareerLevel
import com.example.model.EnvironmentType
import com.example.model.GameMode
import com.example.model.MissionType
import com.example.model.PlayerSaveData
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
fun ModeSelectScreen(
  saveData: PlayerSaveData,
  initialMode: GameMode = GameMode.ENDLESS,
  onStartMode: (GameMode, EnvironmentType) -> Unit,
  onOpenCareer: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedMode by remember { mutableStateOf(initialMode) }
  var selectedEnv by remember { mutableStateOf(EnvironmentType.COUNTRYSIDE) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.92f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(title = "SELECT RACE MODE", coins = saveData.coins, onBack = onBack)

    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Left: Game Modes List
      LazyColumn(
        modifier = Modifier.weight(0.46f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(GameMode.entries) { mode ->
          val isSelected = selectedMode == mode
          val icon =
            when (mode) {
              GameMode.CAREER -> Icons.Filled.EmojiEvents
              GameMode.ENDLESS -> Icons.Filled.AllInclusive
              GameMode.TIME_CHALLENGE -> Icons.Filled.Timer
              GameMode.TRAFFIC_CHALLENGE -> Icons.Filled.Traffic
            }
          val recordText =
            when (mode) {
              GameMode.CAREER -> "Level ${saveData.unlockedCareerLevel}/24 Unlocked"
              GameMode.ENDLESS ->
                if (saveData.bestEndlessMeters > 0f) {
                  "Best: %.2f km".format(saveData.bestEndlessMeters / 1000f)
                } else {
                  "No record yet"
                }
              GameMode.TIME_CHALLENGE ->
                if (saveData.bestTimeChallengeSeconds > 0f) {
                  "Best: %.1fs".format(saveData.bestTimeChallengeSeconds)
                } else {
                  "Target: 3.5 km in 65s"
                }
              GameMode.TRAFFIC_CHALLENGE ->
                if (saveData.bestTrafficOvertakes > 0) {
                  "Best: ${saveData.bestTrafficOvertakes} Overtakes"
                } else {
                  "Target: 30 Overtakes"
                }
            }

          Surface(
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  if (mode == GameMode.CAREER) {
                    onOpenCareer()
                  } else {
                    selectedMode = mode
                  }
                }
                .testTag("mode_card_${mode.name.lowercase()}"),
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) ElevatedSlate else AsphaltSurface,
            border = BorderStroke(1.5.dp, if (isSelected) NitroCyan else PanelBorder),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NitroCyan else AmberGold,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = mode.displayName,
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = mode.subtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = recordText,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldBoost,
              )
            }
          }
        }
      }

      // Right: Environment Selector + Launch Button
      Column(
        modifier = Modifier.weight(0.54f).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "HIGHWAY ENVIRONMENT",
          style = MaterialTheme.typography.titleMedium,
          color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(EnvironmentType.entries) { env ->
            val selected = env == selectedEnv
            Surface(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(62.dp)
                  .clickable { selectedEnv = env }
                  .testTag("env_card_${env.name.lowercase()}"),
              shape = RoundedCornerShape(10.dp),
              color = if (selected) ElevatedSlate else AsphaltSurface,
              border = BorderStroke(1.5.dp, if (selected) Color(env.accentHex) else PanelBorder),
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(12.dp)
                      .background(Color(env.accentHex), RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = env.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (env.isNight) "Headlights Active" else if (env.rainIntensity > 0f) "Wet Asphalt" else "Daylight",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        MotorsportActionButton(
          text = "START ${selectedMode.displayName.uppercase()}",
          icon = Icons.Filled.PlayArrow,
          onClick = { onStartMode(selectedMode, selectedEnv) },
          containerColor = RacingCrimson,
          modifier = Modifier.fillMaxWidth(),
          testTag = "start_mode_race_button",
        )
      }
    }
  }
}

@Composable
fun CareerMapScreen(
  saveData: PlayerSaveData,
  onLaunchLevel: (CareerLevel) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedLevelNum by remember { mutableStateOf(saveData.unlockedCareerLevel) }
  val selectedLevel = CareerCatalog.findLevel(selectedLevelNum)
  var envFilter by remember { mutableStateOf<EnvironmentType?>(null) }
  val visibleLevels =
    remember(envFilter) {
      if (envFilter == null) CareerCatalog.levels
      else CareerCatalog.levels.filter { it.environment == envFilter }
    }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.93f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(title = "CAREER MISSIONS", coins = saveData.coins, onBack = onBack)

    // Environment filter strip
    LazyRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        FilterChipPill(
          text = "ALL (24)",
          selected = envFilter == null,
          accent = NitroCyan,
          onClick = { envFilter = null },
        )
      }
      items(EnvironmentType.entries) { env ->
        FilterChipPill(
          text = env.shortTag,
          selected = envFilter == env,
          accent = Color(env.accentHex),
          onClick = { envFilter = env },
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Left: Level Grid (24 progressive levels)
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.weight(0.62f).fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(visibleLevels) { lvl ->
          val unlocked = lvl.levelNumber <= saveData.unlockedCareerLevel
          val stars = saveData.levelStars[lvl.levelNumber] ?: 0
          val isSelected = lvl.levelNumber == selectedLevelNum
          Surface(
            modifier =
              Modifier
                .height(74.dp)
                .clickable(enabled = unlocked) { selectedLevelNum = lvl.levelNumber }
                .testTag("career_level_${lvl.levelNumber}"),
            shape = RoundedCornerShape(10.dp),
            color = if (isSelected) ElevatedSlate else AsphaltSurface,
            border =
              BorderStroke(
                1.5.dp,
                when {
                  isSelected -> RacingCrimson
                  unlocked -> Color(lvl.environment.accentHex).copy(alpha = 0.55f)
                  else -> PanelBorder.copy(alpha = 0.5f)
                },
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().padding(6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "LVL ${lvl.levelNumber}",
                style = MaterialTheme.typography.labelLarge,
                color = if (unlocked) TextPrimary else TextMuted,
              )
              Text(
                text = lvl.environment.shortTag,
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) Color(lvl.environment.accentHex) else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (unlocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                  for (s in 1..3) {
                    Icon(
                      imageVector = if (s <= stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                      contentDescription = null,
                      tint = if (s <= stars) AmberGold else TextMuted,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              } else {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = "Locked",
                  tint = TextMuted,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }

      // Right: Mission Briefing Panel
      Surface(
        modifier = Modifier.weight(0.38f).fillMaxHeight().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        color = AsphaltSurface,
        border = BorderStroke(1.dp, PanelBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "LEVEL ${selectedLevel.levelNumber} • ${selectedLevel.environment.displayName.uppercase()}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(selectedLevel.environment.accentHex),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = selectedLevel.title,
              style = MaterialTheme.typography.headlineLarge,
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = selectedLevel.description,
              style = MaterialTheme.typography.bodyLarge,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val objectiveLabel =
              when (selectedLevel.missionType) {
                MissionType.REACH_FINISH -> "Distance: %.1f km".format(selectedLevel.targetDistanceMeters / 1000f)
                MissionType.TIME_TRIAL -> "Time Limit: ${selectedLevel.timeLimitSeconds.toInt()}s"
                MissionType.OVERTAKE_COUNT -> "Overtakes: ${selectedLevel.targetOvertakes} cars"
                MissionType.HIGH_SPEED_RUN -> "Reach ${selectedLevel.targetMinAvgSpeedKmh.toInt()} km/h"
              }
            Text(
              text = "OBJECTIVE: $objectiveLabel",
              style = MaterialTheme.typography.titleMedium,
              color = NitroCyan,
            )
            Text(
              text = "REWARD: +${selectedLevel.rewardCoins} COINS",
              style = MaterialTheme.typography.titleMedium,
              color = AmberGold,
            )
          }

          MotorsportActionButton(
            text = "RACE LEVEL ${selectedLevel.levelNumber}",
            icon = Icons.Filled.PlayArrow,
            onClick = { onLaunchLevel(selectedLevel) },
            containerColor = RacingCrimson,
            modifier = Modifier.fillMaxWidth(),
            testTag = "launch_career_level_button",
          )
        }
      }
    }
  }
}

@Composable
private fun FilterChipPill(
  text: String,
  selected: Boolean,
  accent: Color,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = if (selected) accent.copy(alpha = 0.22f) else AsphaltSurface,
    border = BorderStroke(1.dp, if (selected) accent else PanelBorder),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) accent else TextSecondary,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}
