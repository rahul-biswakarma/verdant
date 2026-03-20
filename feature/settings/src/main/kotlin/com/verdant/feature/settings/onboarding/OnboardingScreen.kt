package com.verdant.feature.settings.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6

private val presetColors = listOf(
    0xFF30A14EL, 0xFF2196F3L, 0xFF9C27B0L, 0xFFFF5722L,
    0xFF009688L, 0xFFE91E63L, 0xFFFF9800L, 0xFF607D8BL,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    // Request POST_NOTIFICATIONS permission on Android 13+ during onboarding
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result doesn't block onboarding flow */ }

    val goNext: () -> Unit = {
        scope.launch {
            // When leaving the notifications page (page 4), request permission
            if (pagerState.currentPage == 4 &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (uiState.morningMotivation || uiState.habitReminders)
            ) {
                notificationPermissionLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                )
            }
            if (pagerState.currentPage < PAGE_COUNT - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Progress dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PAGE_COUNT) { index ->
                val isActive = pagerState.currentPage == index
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(300),
                    label = "dot_color",
                )
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                if (index < PAGE_COUNT - 1) Spacer(Modifier.width(8.dp))
            }
        }

        // Page content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> WelcomePage(onGetStarted = goNext)
                1 -> PickHabitsPage(
                    uiState = uiState,
                    onToggle = viewModel::toggleTemplate,
                    onNext = goNext,
                )
                2 -> CustomizePage(
                    uiState = uiState,
                    onNameChange = viewModel::updateDraftName,
                    onColorChange = viewModel::updateDraftColor,
                    onNext = goNext,
                )
                3 -> WidgetSetupPage(
                    onSkip = goNext,
                    onNext = goNext,
                )
                4 -> NotificationsPage(
                    uiState = uiState,
                    onMorningToggle = viewModel::setMorningMotivation,
                    onRemindersToggle = viewModel::setHabitReminders,
                    onNext = goNext,
                )
                5 -> DonePage(
                    uiState = uiState,
                    onStart = {
                        viewModel.completeOnboarding(onComplete)
                    },
                )
            }
        }
    }
}

// ── Page 1: Welcome ───────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF30A14E)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🌿", fontSize = 56.sp)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Verdant",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Grow your habits,\none day at a time",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(64.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                "Get Started",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// ── Page 2: Pick Habits ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickHabitsPage(
    uiState: OnboardingUiState,
    onToggle: (StarterTemplate) -> Unit,
    onNext: () -> Unit,
) {
    val categories = starterTemplates.groupBy { it.category }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                "Pick your habits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pick 3–5 habits to start with",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            categories.forEach { (category, templates) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        templates.forEach { template ->
                            val selected = template in uiState.selectedTemplates
                            TemplateChip(
                                template = template,
                                selected = selected,
                                onClick = { onToggle(template) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onNext,
                enabled = uiState.selectedTemplates.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Next (${uiState.selectedTemplates.size} selected)")
            }
        }
    }
}

@Composable
private fun TemplateChip(
    template: StarterTemplate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(template.color).copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color(template.color)
        else Color.Transparent,
        label = "chip_border",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(if (selected) 2.dp else 0.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(template.icon, fontSize = 18.sp)
        Text(template.name, style = MaterialTheme.typography.bodyMedium)
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(template.color),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Page 3: Customize ─────────────────────────────────────────────────────────

@Composable
private fun CustomizePage(
    uiState: OnboardingUiState,
    onNameChange: (Int, String) -> Unit,
    onColorChange: (Int, Long) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                "Customize",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Personalize your habits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(uiState.drafts) { index, draft ->
                DraftCard(
                    draft = draft,
                    onNameChange = { onNameChange(index, it) },
                    onColorChange = { onColorChange(index, it) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Next")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftCard(
    draft: OnboardingDraft,
    onNameChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(draft.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(draft.icon, fontSize = 24.sp)
                }
                OutlinedTextField(
                    value = draft.displayName,
                    onValueChange = onNameChange,
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Color",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presetColors.forEach { colorLong ->
                    val selected = draft.color == colorLong
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(colorLong))
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorChange(colorLong) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Page 4: Widget Setup ──────────────────────────────────────────────────────

@Composable
private fun WidgetSetupPage(
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Widgets,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Add a widget",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Add a Verdant widget to your home screen for quick habit tracking without opening the app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Widget preview illustration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌿", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Verdant Widget",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Long-press home screen → Widgets → Verdant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Next")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("I'll do this later")
        }
    }
}

// ── Page 5: Notifications ─────────────────────────────────────────────────────

@Composable
private fun NotificationsPage(
    uiState: OnboardingUiState,
    onMorningToggle: (Boolean) -> Unit,
    onRemindersToggle: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Stay on track",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Choose how Verdant motivates you",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        NotificationToggleCard(
            emoji = "☀️",
            title = "Morning motivation",
            subtitle = "A daily nudge to start your habits strong",
            checked = uiState.morningMotivation,
            onCheckedChange = onMorningToggle,
        )

        Spacer(Modifier.height(12.dp))

        NotificationToggleCard(
            emoji = "🔔",
            title = "Habit reminders",
            subtitle = "Timely reminders based on your habit schedule",
            checked = uiState.habitReminders,
            onCheckedChange = onRemindersToggle,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Next")
        }
    }
}

@Composable
private fun NotificationToggleCard(
    emoji: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(emoji, fontSize = 32.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// ── Page 6: Done ──────────────────────────────────────────────────────────────

@Composable
private fun DonePage(
    uiState: OnboardingUiState,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 64.sp)

        Spacer(Modifier.height(24.dp))

        Text(
            "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Your first green square awaits.",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // Mini habit preview
        if (uiState.drafts.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Your habits",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    uiState.drafts.take(5).forEach { draft ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(draft.color)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(draft.icon, fontSize = 18.sp)
                            }
                            Text(
                                draft.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            // Empty grid dots preview
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(7) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        if (uiState.isSaving) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF30A14E),
                ),
            ) {
                Text(
                    "Start tracking",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
