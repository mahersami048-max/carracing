package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.input.RaceInputState
import com.example.model.GameMode
import com.example.model.RaceHudState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.EmeraldBoost
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RaceHudOverlay(
  hud: RaceHudState,
  mode: GameMode,
  levelNumber: Int?,
  inputState: RaceInputState,
  onTogglePause: () -> Unit,
) {
  BackHandler(onBack = onTogglePause)

  DisposableEffect(Unit) {
    onDispose { inputState.reset() }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    // TOP HUD BAR
    Row(
      modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top-Left: Level / Mode & Mission Progress
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = AsphaltSurface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, PanelBorder),
      ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp).width(190.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = if (levelNumber != null) "LEVEL $levelNumber" else mode.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = NitroCyan,
            )
            Text(
              text = hud.missionSummary,
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
            )
          }
          Spacer(modifier = Modifier.height(5.dp))
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CarbonBlack)
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth(hud.progressFraction.coerceIn(0.02f, 1f))
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp))
                  .background(EmeraldBoost)
            )
          }
        }
      }

      // Top-Center: Distance Remaining / Time Remaining
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = AsphaltSurface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, PanelBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = if (hud.targetDistanceMeters > 0f) "REMAINING" else "DISTANCE",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary,
            )
            val distDisplay =
              if (hud.targetDistanceMeters > 0f) {
                "%.2f km".format(((hud.targetDistanceMeters - hud.distanceMeters).coerceAtLeast(0f)) / 1000f)
              } else {
                "%.2f km".format(hud.distanceMeters / 1000f)
              }
            Text(
              text = distDisplay,
              style = MaterialTheme.typography.titleMedium,
              fontFamily = JetBrainsMonoFontFamily,
              color = TextPrimary,
            )
          }

          if (hud.timeRemainingSeconds > 0f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "TIME", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Text(
                text = "%.1fs".format(hud.timeRemainingSeconds),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = if (hud.timeRemainingSeconds < 10f) RacingCrimson else AmberGold,
              )
            }
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "OVERTAKES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Text(
                text = "${hud.overtakes}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                color = NitroCyan,
              )
            }
          }
        }
      }

      // Top-Right: Coins + Pause Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CoinBadge(coins = hud.coinsEarned)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AsphaltSurface.copy(alpha = 0.84f),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          IconButton(
            onClick = onTogglePause,
            modifier = Modifier.size(48.dp).testTag("hud_pause_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Pause,
              contentDescription = stringResource(R.string.cd_pause),
              tint = TextPrimary,
            )
          }
        }
      }
    }

    // CENTER CALLOUT / COUNTDOWN
    if (hud.countdownValue > 0) {
      Text(
        text = "${hud.countdownValue}",
        fontFamily = RussoOneFontFamily,
        fontSize = 84.sp,
        color = AmberGold,
        modifier = Modifier.align(Alignment.Center),
      )
    } else if (hud.calloutText != null) {
      Surface(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 66.dp),
        shape = RoundedCornerShape(8.dp),
        color = CarbonBlack.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, NitroCyan),
      ) {
        Text(
          text = hud.calloutText,
          style = MaterialTheme.typography.headlineMedium,
          color = NitroCyan,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        )
      }
    }

    // BOTTOM-CENTER: Speedometer + Nitro Meter
    Column(
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SpeedometerGauge(
        speedKmh = hud.speedKmh,
        isNitroActive = hud.isNitroActive,
      )
      Spacer(modifier = Modifier.height(4.dp))
      // Nitro Bar
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = AsphaltSurface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, if (hud.isNitroActive) NitroCyan else PanelBorder),
      ) {
        Row(
          modifier = Modifier.width(190.dp).padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "N2O",
            style = MaterialTheme.typography.labelSmall,
            color = NitroCyan,
            fontWeight = FontWeight.Bold,
          )
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CarbonBlack)
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth(hud.nitroEnergy.coerceIn(0f, 1f))
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(
                    Brush.horizontalGradient(
                      listOf(NitroCyan, if (hud.isNitroActive) AmberGold else EmeraldBoost)
                    )
                  )
            )
          }
        }
      }
    }

    // BOTTOM-LEFT: Steering Touch Controls
    Row(
      modifier = Modifier.align(Alignment.BottomStart),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      HoldControlButton(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        label = null,
        contentDesc = stringResource(R.string.cd_steer_left),
        activeColor = NitroCyan,
        onHoldChanged = { inputState.steerLeft = it },
        modifier = Modifier.size(72.dp).testTag("steer_left_button"),
      )
      HoldControlButton(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        label = null,
        contentDesc = stringResource(R.string.cd_steer_right),
        activeColor = NitroCyan,
        onHoldChanged = { inputState.steerRight = it },
        modifier = Modifier.size(72.dp).testTag("steer_right_button"),
      )
    }

    // BOTTOM-RIGHT: Nitro, Brake, Accelerator Controls
    Column(
      modifier = Modifier.align(Alignment.BottomEnd),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      HoldControlButton(
        icon = Icons.Filled.Bolt,
        label = "NITRO",
        contentDesc = stringResource(R.string.cd_nitro),
        activeColor = NitroCyan,
        shapeCircle = true,
        onHoldChanged = { inputState.nitro = it },
        modifier = Modifier.size(64.dp).testTag("nitro_button"),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HoldControlButton(
          icon = null,
          label = "BRAKE",
          contentDesc = stringResource(R.string.cd_brake),
          activeColor = RacingCrimson,
          onHoldChanged = { inputState.brake = it },
          modifier = Modifier.width(76.dp).height(68.dp).testTag("brake_button"),
        )
        HoldControlButton(
          icon = Icons.Filled.Speed,
          label = "GAS",
          contentDesc = stringResource(R.string.cd_accelerate),
          activeColor = EmeraldBoost,
          onHoldChanged = { inputState.accelerate = it },
          modifier = Modifier.width(86.dp).height(82.dp).testTag("accelerate_button"),
        )
      }
    }
  }
}

@Composable
private fun SpeedometerGauge(speedKmh: Float, isNitroActive: Boolean) {
  Box(
    modifier = Modifier.size(width = 148.dp, height = 84.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeW = 8.dp.toPx()
      val arcSize = Size(size.width - strokeW, (size.width - strokeW))
      val topLeft = Offset(strokeW / 2f, 4.dp.toPx())
      drawArc(
        color = AsphaltSurface.copy(alpha = 0.85f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
      )
      val sweep = (speedKmh / 360f).coerceIn(0f, 1f) * 180f
      drawArc(
        color = if (isNitroActive) NitroCyan else if (speedKmh > 240f) RacingCrimson else AmberGold,
        startAngle = 180f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
      Text(
        text = "${speedKmh.toInt()}",
        fontFamily = RussoOneFontFamily,
        fontSize = 32.sp,
        color = if (isNitroActive) NitroCyan else TextPrimary,
      )
      Text(
        text = "KM/H",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun HoldControlButton(
  icon: ImageVector?,
  label: String?,
  contentDesc: String,
  activeColor: Color,
  onHoldChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  shapeCircle: Boolean = false,
) {
  var pressed by remember { mutableStateOf(false) }

  Surface(
    modifier =
      modifier.pointerInput(Unit) {
        awaitEachGesture {
          awaitFirstDown(requireUnconsumed = false)
          pressed = true
          onHoldChanged(true)
          waitForUpOrCancellation()
          pressed = false
          onHoldChanged(false)
        }
      },
    shape = if (shapeCircle) CircleShape else RoundedCornerShape(14.dp),
    color = if (pressed) activeColor.copy(alpha = 0.36f) else AsphaltSurface.copy(alpha = 0.78f),
    border = BorderStroke(2.dp, if (pressed) activeColor else activeColor.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = contentDesc,
          tint = if (pressed) Color.White else activeColor,
          modifier = Modifier.size(if (label == null) 34.dp else 22.dp),
        )
      }
      if (label != null) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = if (pressed) Color.White else TextPrimary,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
