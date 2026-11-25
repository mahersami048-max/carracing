package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AsphaltSurface
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.RacingCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CoinBadge(coins: Int, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = AsphaltSurface.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.MonetizationOn,
        contentDescription = stringResource(R.string.cd_coins),
        tint = AmberGold,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "%,d".format(coins),
        style = MaterialTheme.typography.titleMedium,
        color = AmberGold,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun ScreenTopBar(
  title: String,
  coins: Int,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = AsphaltSurface.copy(alpha = 0.85f),
      border = BorderStroke(1.dp, PanelBorder),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.size(48.dp).testTag("back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = TextPrimary,
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.headlineLarge,
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.weight(1f))
    if (trailingContent != null) {
      trailingContent()
      Spacer(modifier = Modifier.width(10.dp))
    }
    CoinBadge(coins = coins)
  }
}

@Composable
fun MotorsportActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  containerColor: Color = RacingCrimson,
  contentColor: Color = TextPrimary,
  enabled: Boolean = true,
  testTag: String = "",
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.height(48.dp).then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
    shape = RoundedCornerShape(10.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = ElevatedSlate,
        disabledContentColor = TextSecondary,
      ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun StatBar(
  label: String,
  valueText: String,
  fraction: Float,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelSmall,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(CarbonBlack)
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
              Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = 0.7f), accentColor)
              )
            )
      )
    }
  }
}
