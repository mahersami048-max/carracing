package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CarCatalog
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
import com.example.viewmodel.UpgradeStat

@Composable
fun GarageScreen(
  saveData: PlayerSaveData,
  previewCarId: String,
  onSelectPreviewCar: (String) -> Unit,
  onUnlockOrSelectCar: (String) -> Unit,
  onUpgradeStat: (String, UpgradeStat) -> Unit,
  onSelectPaint: (String, Int) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val carSpec = CarCatalog.findById(previewCarId)
  val upgrade = saveData.upgradeFor(previewCarId)
  val isUnlocked = saveData.unlockedCarIds.contains(previewCarId)
  val isEquipped = saveData.selectedCarId == previewCarId

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.horizontalGradient(
            listOf(
              Color.Transparent,
              CarbonBlack.copy(alpha = 0.25f),
              CarbonBlack.copy(alpha = 0.88f),
            )
          )
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      ScreenTopBar(
        title = "GARAGE",
        coins = saveData.coins,
        onBack = onBack,
        trailingContent = {
          Text(
            text = "SWIPE CAR TO ROTATE",
            style = MaterialTheme.typography.labelSmall,
            color = NitroCyan,
          )
        },
      )

      Row(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Left side stays transparent over 3D turntable so user sees and rotates the 3D car!
        Box(modifier = Modifier.weight(0.48f).fillMaxHeight()) {
          Column(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)) {
            Text(
              text = carSpec.category.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = NitroCyan,
            )
            Text(
              text = carSpec.name,
              style = MaterialTheme.typography.displayMedium,
              color = TextPrimary,
            )
            Text(
              text = carSpec.description,
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // Right side: Specs, Paint, Upgrades & Buy/Equip
        Surface(
          modifier = Modifier.weight(0.52f).fillMaxHeight(),
          shape = RoundedCornerShape(14.dp),
          color = AsphaltSurface.copy(alpha = 0.92f),
          border = BorderStroke(1.dp, PanelBorder),
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val topSpeed = upgrade.effectiveTopSpeed(carSpec)
            val accel = upgrade.effectiveAcceleration(carSpec)
            val handling = upgrade.effectiveHandling(carSpec)
            val nitro = upgrade.effectiveNitroPower(carSpec)

            StatBar(
              label = "TOP SPEED",
              valueText = "${topSpeed.toInt()} KM/H",
              fraction = (topSpeed / 400f),
              accentColor = RacingCrimson,
            )
            StatBar(
              label = "ACCELERATION",
              valueText = "${(accel * 100).toInt()}",
              fraction = (accel / 1.25f),
              accentColor = AmberGold,
            )
            StatBar(
              label = "HANDLING",
              valueText = "${(handling * 100).toInt()}",
              fraction = (handling / 1.25f),
              accentColor = EmeraldBoost,
            )
            StatBar(
              label = "NITRO BOOST",
              valueText = "${(nitro * 100).toInt()}",
              fraction = (nitro / 1.30f),
              accentColor = NitroCyan,
            )

            if (isUnlocked) {
              // Paint color swatches
              Text(
                text = "PAINT FINISH",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                CarCatalog.paintPalette.forEach { (_, hex) ->
                  val selected = upgrade.colorHex == hex
                  Box(
                    modifier =
                      Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                          width = if (selected) 2.5.dp else 1.dp,
                          color = if (selected) Color.White else PanelBorder,
                          shape = CircleShape,
                        )
                        .clickable { onSelectPaint(previewCarId, hex) }
                  )
                }
              }

              // Upgrades row
              Text(
                text = "PERFORMANCE UPGRADES",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                UpgradeTile(
                  label = "ENGINE",
                  level = upgrade.engineLevel,
                  cost = upgrade.upgradeCost(upgrade.engineLevel),
                  canAfford = saveData.coins >= upgrade.upgradeCost(upgrade.engineLevel),
                  onClick = { onUpgradeStat(previewCarId, UpgradeStat.ENGINE) },
                  modifier = Modifier.weight(1f),
                  testTag = "upgrade_engine_button",
                )
                UpgradeTile(
                  label = "HANDLING",
                  level = upgrade.handlingLevel,
                  cost = upgrade.upgradeCost(upgrade.handlingLevel),
                  canAfford = saveData.coins >= upgrade.upgradeCost(upgrade.handlingLevel),
                  onClick = { onUpgradeStat(previewCarId, UpgradeStat.HANDLING) },
                  modifier = Modifier.weight(1f),
                  testTag = "upgrade_handling_button",
                )
                UpgradeTile(
                  label = "NITRO",
                  level = upgrade.nitroLevel,
                  cost = upgrade.upgradeCost(upgrade.nitroLevel),
                  canAfford = saveData.coins >= upgrade.upgradeCost(upgrade.nitroLevel),
                  onClick = { onUpgradeStat(previewCarId, UpgradeStat.NITRO) },
                  modifier = Modifier.weight(1f),
                  testTag = "upgrade_nitro_button",
                )
              }
            }

            Spacer(modifier = Modifier.height(2.dp))
            MotorsportActionButton(
              text =
                when {
                  isEquipped -> "SELECTED FOR RACE"
                  isUnlocked -> "SELECT VEHICLE"
                  else -> "UNLOCK FOR %,d COINS".format(carSpec.price)
                },
              icon =
                when {
                  isEquipped -> Icons.Filled.CheckCircle
                  isUnlocked -> Icons.Filled.CheckCircle
                  else -> Icons.Filled.ShoppingCart
                },
              containerColor =
                when {
                  isEquipped -> EmeraldBoost
                  isUnlocked -> NitroCyan
                  else -> AmberGold
                },
              contentColor = CarbonBlack,
              enabled = isUnlocked || saveData.coins >= carSpec.price,
              onClick = { onUnlockOrSelectCar(previewCarId) },
              modifier = Modifier.fillMaxWidth(),
              testTag = "garage_select_unlock_button",
            )
          }
        }
      }

      // Bottom 6-Car Carousel Strip
      LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(CarCatalog.cars) { car ->
          val unlocked = saveData.unlockedCarIds.contains(car.id)
          val selected = car.id == previewCarId
          val equipped = car.id == saveData.selectedCarId
          Surface(
            modifier =
              Modifier
                .width(148.dp)
                .height(58.dp)
                .clickable { onSelectPreviewCar(car.id) }
                .testTag("garage_car_${car.id}"),
            shape = RoundedCornerShape(10.dp),
            color = if (selected) ElevatedSlate else AsphaltSurface.copy(alpha = 0.90f),
            border =
              BorderStroke(
                1.5.dp,
                when {
                  selected -> NitroCyan
                  equipped -> EmeraldBoost
                  else -> PanelBorder
                },
              ),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = car.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (unlocked) (if (equipped) "EQUIPPED" else "OWNED") else "%,d".format(car.price),
                  style = MaterialTheme.typography.labelSmall,
                  color = if (equipped) EmeraldBoost else if (unlocked) NitroCyan else AmberGold,
                )
              }
              if (!unlocked) {
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
    }
  }
}

@Composable
private fun UpgradeTile(
  label: String,
  level: Int,
  cost: Int,
  canAfford: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String,
) {
  val isMax = level >= 5
  Surface(
    modifier =
      modifier
        .clickable(enabled = !isMax && canAfford, onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(8.dp),
    color = ElevatedSlate,
    border =
      BorderStroke(
        1.dp,
        when {
          isMax -> EmeraldBoost
          canAfford -> NitroCyan.copy(alpha = 0.65f)
          else -> PanelBorder
        },
      ),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
          imageVector = Icons.Filled.Build,
          contentDescription = null,
          tint = if (isMax) EmeraldBoost else NitroCyan,
          modifier = Modifier.size(12.dp),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
      }
      Text(
        text = "LVL $level/5",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
      Text(
        text = if (isMax) "MAX" else "$cost",
        style = MaterialTheme.typography.labelSmall,
        color = if (isMax) EmeraldBoost else if (canAfford) AmberGold else TextMuted,
      )
    }
  }
}
