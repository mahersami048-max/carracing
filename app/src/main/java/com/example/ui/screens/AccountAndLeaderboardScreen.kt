package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.PlayerSaveData
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.EmeraldBoost
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CloudUiState

@Composable
fun AccountAndLeaderboardScreen(
  saveData: PlayerSaveData,
  cloudState: CloudUiState,
  onSignUp: (email: String, password: String, driverName: String) -> Unit,
  onSignIn: (email: String, password: String) -> Unit,
  onSignOut: () -> Unit,
  onSyncNow: () -> Unit,
  onRefreshLeaderboard: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var isSignUpMode by remember { mutableStateOf(true) }
  var driverName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(CarbonBlack.copy(alpha = 0.94f))
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ScreenTopBar(
      title = "DRIVER PROFILE & RANKINGS",
      coins = saveData.coins,
      onBack = onBack,
      trailingContent = {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = AsphaltSurface,
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Leaderboard,
              contentDescription = null,
              tint = NitroCyan,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${cloudState.totalPlayers} DRIVERS",
              style = MaterialTheme.typography.labelSmall,
              color = TextPrimary,
            )
          }
        }
      },
    )

    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Left Pane: Driver Cloud Account (Sign Up / Sign In or Profile Card)
      Surface(
        modifier = Modifier.weight(0.45f).fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = AsphaltSurface,
        border = BorderStroke(1.dp, PanelBorder),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val session = saveData.cloudSession
          if (session == null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              AuthTabPill(
                text = "CREATE ACCOUNT",
                selected = isSignUpMode,
                onClick = { isSignUpMode = true },
                modifier = Modifier.weight(1f),
              )
              AuthTabPill(
                text = "SIGN IN",
                selected = !isSignUpMode,
                onClick = { isSignUpMode = false },
                modifier = Modifier.weight(1f),
              )
            }

            if (!cloudState.isConfigured) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
              ) {
                Text(
                  text = "Add SUPABASE_ANON_KEY in AI Studio Secrets panel to enable cloud accounts & leaderboard.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = AmberGold,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }

            if (isSignUpMode) {
              OutlinedTextField(
                value = driverName,
                onValueChange = { driverName = it.take(24) },
                label = { Text("Driver Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("auth_driver_name_input"),
                colors = textFieldColors(),
              )
            }

            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
              colors = textFieldColors(),
            )

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password (min 6 chars)") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
              colors = textFieldColors(),
            )

            if (!cloudState.statusMessage.isNullOrBlank()) {
              Text(
                text = cloudState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (cloudState.isError) RacingCrimson else EmeraldBoost,
              )
            }

            MotorsportActionButton(
              text = if (cloudState.isLoading) "CONNECTING..." else if (isSignUpMode) "REGISTER & SYNC" else "SIGN IN",
              icon = if (isSignUpMode) Icons.Filled.PersonAdd else Icons.Filled.Person,
              enabled = !cloudState.isLoading,
              containerColor = NitroCyan,
              contentColor = CarbonBlack,
              onClick = {
                if (isSignUpMode) {
                  onSignUp(email, password, driverName)
                } else {
                  onSignIn(email, password)
                }
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "auth_submit_button",
            )
          } else {
            // Signed-in Driver Profile Card
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CloudDone,
                contentDescription = "Connected",
                tint = EmeraldBoost,
                modifier = Modifier.size(28.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = session.driverName,
                  style = MaterialTheme.typography.headlineMedium,
                  color = TextPrimary,
                )
                Text(
                  text = session.email,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ProfileMiniStat(
                label = "LEVELS PASSED",
                value = "${saveData.levelsPassedCount}/24",
                modifier = Modifier.weight(1f),
              )
              ProfileMiniStat(
                label = "STARS",
                value = "★ ${saveData.levelStars.values.sum()}",
                modifier = Modifier.weight(1f),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ProfileMiniStat(
                label = "BEST ENDLESS",
                value = "%.2f km".format(saveData.bestEndlessMeters / 1000f),
                modifier = Modifier.weight(1f),
              )
              ProfileMiniStat(
                label = "TOP SPEED",
                value = "${saveData.highestSpeedKmh.toInt()} km/h",
                modifier = Modifier.weight(1f),
              )
            }

            if (!cloudState.statusMessage.isNullOrBlank()) {
              Text(
                text = cloudState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (cloudState.isError) RacingCrimson else EmeraldBoost,
              )
            }

            MotorsportActionButton(
              text = if (cloudState.isLoading) "SYNCING..." else "SYNC SCORE NOW",
              icon = Icons.Filled.CloudUpload,
              enabled = !cloudState.isLoading,
              containerColor = NitroCyan,
              contentColor = CarbonBlack,
              onClick = onSyncNow,
              modifier = Modifier.fillMaxWidth(),
              testTag = "cloud_sync_now_button",
            )

            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cloud_sign_out_button"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, RacingCrimson.copy(alpha = 0.65f)),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = RacingCrimson,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("SIGN OUT", style = MaterialTheme.typography.labelLarge, color = RacingCrimson)
            }
          }
        }
      }

      // Right Pane: Global Supabase Leaderboard
      Surface(
        modifier = Modifier.weight(0.55f).fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        color = AsphaltSurface,
        border = BorderStroke(1.dp, PanelBorder),
      ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "GLOBAL LEADERBOARD",
                style = MaterialTheme.typography.titleMedium,
                color = NitroCyan,
              )
              Text(
                text = "Ranked by Career Levels Passed & Best Distance",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
            IconButton(
              onClick = onRefreshLeaderboard,
              modifier = Modifier.size(48.dp).testTag("refresh_leaderboard_button"),
            ) {
              if (cloudState.isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = NitroCyan,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = "Refresh Leaderboard",
                  tint = TextPrimary,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (cloudState.leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text =
                  if (cloudState.isConfigured) {
                    "No drivers synced yet. Create an account and finish a race!"
                  } else {
                    "Leaderboard will appear once SUPABASE_ANON_KEY is set."
                  },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              itemsIndexed(cloudState.leaderboard) { index, entry ->
                val isSelf = entry.id == saveData.cloudSession?.userId
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelf) NitroCyan.copy(alpha = 0.14f) else ElevatedSlate,
                  border = BorderStroke(1.dp, if (isSelf) NitroCyan else PanelBorder),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "#${index + 1}",
                      style = MaterialTheme.typography.titleMedium,
                      color = if (index < 3) AmberGold else TextSecondary,
                      modifier = Modifier.width(36.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = entry.driverName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "${entry.selectedCar} • ${entry.totalRacesPlayed} races",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = "LVL ${entry.levelsPassed}/24 • ★${entry.totalStars}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldBoost,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "%.2f km • ${entry.highestSpeedKmh.toInt()} km/h".format(entry.bestEndlessMeters / 1000f),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuthTabPill(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(40.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = if (selected) NitroCyan.copy(alpha = 0.22f) else ElevatedSlate,
    border = BorderStroke(1.dp, if (selected) NitroCyan else PanelBorder),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (selected) NitroCyan else TextSecondary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun ProfileMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = ElevatedSlate,
    border = BorderStroke(1.dp, PanelBorder),
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Text(text = value, style = MaterialTheme.typography.titleMedium, color = NitroCyan)
    }
  }
}

@Composable
private fun textFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NitroCyan,
    unfocusedBorderColor = PanelBorder,
    focusedLabelColor = NitroCyan,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NitroCyan,
  )
