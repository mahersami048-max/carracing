package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GraphicsQuality
import com.example.model.PlayerSaveData
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
  saveData: PlayerSaveData,
  isTiltSupported: Boolean,
  onSoundChanged: (Boolean) -> Unit,
  onMusicChanged: (Boolean) -> Unit,
  onTiltChanged: (Boolean) -> Unit,
  onGraphicsQualityChanged: (GraphicsQuality) -> Unit,
  onResetProgress: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  var showResetDialog by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.93f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(title = "SETTINGS", coins = saveData.coins, onBack = onBack)

    Row(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .verticalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SettingToggleRow(
          icon = Icons.Filled.VolumeUp,
          title = "Sound Effects",
          subtitle = "Engine RPM, tire screech, nitro & impacts",
          checked = saveData.soundEnabled,
          onCheckedChange = onSoundChanged,
          testTag = "setting_sound_switch",
        )
        SettingToggleRow(
          icon = Icons.Filled.MusicNote,
          title = "Racing Music",
          subtitle = "Synthesized synthwave racing soundtrack",
          checked = saveData.musicEnabled,
          onCheckedChange = onMusicChanged,
          testTag = "setting_music_switch",
        )
        SettingToggleRow(
          icon = Icons.Filled.PhoneAndroid,
          title = "Tilt Steering",
          subtitle = if (isTiltSupported) "Steer by tilting device (touch buttons stay active)" else "Accelerometer sensor unavailable on this device",
          checked = saveData.tiltSteeringEnabled && isTiltSupported,
          enabled = isTiltSupported,
          onCheckedChange = onTiltChanged,
          testTag = "setting_tilt_switch",
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AsphaltSurface,
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = null,
                tint = NitroCyan,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Graphics Quality",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = "Adjust 3D draw distance, reflections & rain particles",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              GraphicsQuality.entries.forEach { q ->
                val selected = saveData.graphicsQuality == q
                Surface(
                  modifier =
                    Modifier
                      .weight(1f)
                      .height(44.dp)
                      .clickable { onGraphicsQualityChanged(q) }
                      .testTag("quality_${q.name.lowercase()}"),
                  shape = RoundedCornerShape(8.dp),
                  color = if (selected) NitroCyan.copy(alpha = 0.22f) else ElevatedSlate,
                  border = BorderStroke(1.5.dp, if (selected) NitroCyan else PanelBorder),
                ) {
                  Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = q.displayName.uppercase(),
                      style = MaterialTheme.typography.labelLarge,
                      color = if (selected) NitroCyan else TextSecondary,
                    )
                  }
                }
              }
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AsphaltSurface,
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Reset Save Data",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = "Clears unlocked cars, career levels, stars & coins",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
            OutlinedButton(
              onClick = { showResetDialog = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = RacingCrimson),
              border = BorderStroke(1.dp, RacingCrimson),
              modifier = Modifier.height(48.dp).testTag("reset_progress_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("RESET", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }
    }
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Reset Game Progress?") },
      text = { Text("This will restore starter coins, reset unlocked cars and career progress.") },
      confirmButton = {
        TextButton(
          onClick = {
            showResetDialog = false
            onResetProgress()
          }
        ) {
          Text("RESET", color = RacingCrimson)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) {
          Text("CANCEL")
        }
      },
    )
  }
}

@Composable
private fun SettingToggleRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  enabled: Boolean = true,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = AsphaltSurface,
    border = BorderStroke(1.dp, PanelBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (checked) NitroCyan else TextSecondary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
        colors = SwitchDefaults.colors(checkedThumbColor = CarbonBlack, checkedTrackColor = NitroCyan),
      )
    }
  }
}
