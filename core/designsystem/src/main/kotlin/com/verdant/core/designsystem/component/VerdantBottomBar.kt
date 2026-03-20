package com.verdant.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating pill-shaped bottom navigation bar.
 *
 * All destinations sit inside a single dark capsule. The selected item gets a
 * lighter pill highlight with icon + label; unselected items show only their icon.
 */
@Composable
fun VerdantBottomBar(
    items: List<BottomBarItem>,
    modifier: Modifier = Modifier,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Palette adapts to theme
    val pillBackground = if (isDark) Color(0xFF1E1C1A) else Color(0xFF1A1917)
    val selectedBg = if (isDark) Color(0xFF2A2826) else Color(0xFF2E2D2B)
    val selectedContent = if (isDark) Color(0xFFE8E2DB) else Color(0xFFF5F0EB)
    val iconTint = if (isDark) Color(0xFFE8E2DB).copy(alpha = 0.6f) else Color(0xFFE8E2DB).copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navBarPadding.calculateBottomPadding()),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 12.dp)
                .height(64.dp)
                .fillMaxWidth()
                .background(
                    color = pillBackground,
                    shape = RoundedCornerShape(36.dp),
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                NavPillItem(
                    icon = item.icon,
                    label = item.label,
                    selected = item.selected,
                    onClick = item.onClick,
                    selectedBackground = selectedBg,
                    selectedContentColor = selectedContent,
                    unselectedIconTint = iconTint,
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedBackground: Color,
    selectedContentColor: Color,
    unselectedIconTint: Color,
) {
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 16.dp else 14.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hPad",
    )

    Row(
        modifier = Modifier
            .height(44.dp)
            .then(
                if (selected) {
                    Modifier.background(
                        color = selectedBackground,
                        shape = RoundedCornerShape(24.dp),
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (!selected) label else null,
            tint = if (selected) selectedContentColor else unselectedIconTint,
            modifier = Modifier.size(22.dp),
        )

        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(
                expandFrom = Alignment.Start,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) + fadeIn(),
            exit = shrinkHorizontally(
                shrinkTowards = Alignment.Start,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) + fadeOut(),
        ) {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = selectedContentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Helper to compute luminance for theme detection. */
private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

/** Represents a single bottom-bar destination. */
data class BottomBarItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)
