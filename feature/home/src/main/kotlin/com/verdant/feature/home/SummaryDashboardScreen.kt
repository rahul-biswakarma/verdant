package com.verdant.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.verdant.core.common.auth.AuthUser
import com.verdant.core.designsystem.component.CompletionRing
import com.verdant.core.model.InferredMood
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowRight
import compose.icons.tablericons.Bell
import compose.icons.tablericons.ChartBar
import compose.icons.tablericons.Edit
import compose.icons.tablericons.Flame
import compose.icons.tablericons.Heart
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.Logout
import compose.icons.tablericons.MoodHappy
import compose.icons.tablericons.MoodNeutral
import compose.icons.tablericons.MoodSad
import compose.icons.tablericons.Search
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Stars
import compose.icons.tablericons.Trophy
import compose.icons.tablericons.User
import compose.icons.tablericons.Wallet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── Pastel palette inspired by the design ─────────────────────────
private val PastelRose = Color(0xFFF8E1E4)
private val PastelLavender = Color(0xFFE4D9F5)
private val PastelMint = Color(0xFFD4EBD9)
private val PastelPeach = Color(0xFFFDE5D0)
private val PastelCoral = Color(0xFFF5D0D5)
private val PastelSky = Color(0xFFD6E8F8)

private val IconRose = Color(0xFFD4626E)
private val IconLavender = Color(0xFF8B6FC0)
private val IconMint = Color(0xFF4CAF72)
private val IconPeach = Color(0xFFE8864A)
private val IconCoral = Color(0xFFC94C60)
private val IconSky = Color(0xFF4A90D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryDashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLifeDashboard: () -> Unit = {},
    onNavigateToHabitDetail: (String) -> Unit = {},
    onSignedOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SummaryDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val profileSheetState by viewModel.profileSheetState.collectAsStateWithLifecycle()

    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Header ───────────────────────────────────────────
        DashboardHeader(
            user = user,
            onAvatarClick = { showProfileSheet = true },
            onNavigateToSettings = onNavigateToSettings,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Today's Habits Summary Card (rose) ───────────────
        TodayHabitsCard(state = state)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Your Progress Card (lavender) ────────────────────
        ProgressCard(state = state)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Category Cards Grid (2×2) ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryCard(
                title = "Habits",
                subtitle = "Stay Consistent",
                icon = TablerIcons.ListCheck,
                backgroundColor = PastelMint,
                iconColor = IconMint,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
            CategoryCard(
                title = "Wellness",
                subtitle = "Mind & Body",
                icon = TablerIcons.Heart,
                backgroundColor = PastelLavender,
                iconColor = IconLavender,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryCard(
                title = "Finance",
                subtitle = "Track Spending",
                icon = TablerIcons.Wallet,
                backgroundColor = PastelPeach,
                iconColor = IconPeach,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
            CategoryCard(
                title = "Insights",
                subtitle = "AI Analysis",
                icon = TablerIcons.Stars,
                backgroundColor = PastelCoral,
                iconColor = IconCoral,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Activity Recap + Stats row ───────────────────────
        ActivityRecapCard(state = state, onNavigateToHabitDetail = onNavigateToHabitDetail)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Best Streak",
                value = "${state.bestCurrentStreak}",
                unit = "days",
                backgroundColor = PastelLavender,
                icon = TablerIcons.Flame,
                iconColor = IconLavender,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Today's Score",
                value = "${(state.completionPercent * 100).toInt()}%",
                unit = "",
                backgroundColor = PastelCoral,
                icon = TablerIcons.Trophy,
                iconColor = IconCoral,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Finance Card (rose) ──────────────────────────────
        FinanceCard(state = state)

        Spacer(modifier = Modifier.height(16.dp))

        // ── Life Dashboard Quick Access ──────────────────────
        LifeDashboardCard(onNavigateToLifeDashboard = onNavigateToLifeDashboard)

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── Profile Bottom Sheet ─────────────────────────────────
    if (showProfileSheet) {
        ProfileBottomSheet(
            user = user,
            profileState = profileSheetState,
            onDismiss = { showProfileSheet = false },
            onUpdateName = viewModel::updateDisplayName,
            onNavigateToSettings = {
                showProfileSheet = false
                onNavigateToSettings()
            },
            onSignOut = {
                showProfileSheet = false
                viewModel.signOut()
                onSignedOut()
            },
            onClearError = viewModel::clearProfileError,
        )
    }
}

// ── Header ─────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    user: AuthUser?,
    onAvatarClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Clickable avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            if (user?.photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = user?.displayName?.firstOrNull()?.uppercase() ?: "V",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello${user?.displayName?.let { ", ${it.split(" ").first()}" } ?: ""}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Good ${greeting()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = { }) {
            Icon(
                imageVector = TablerIcons.Search,
                contentDescription = "Search",
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                imageVector = TablerIcons.Bell,
                contentDescription = "Notifications",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ── Profile Bottom Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileBottomSheet(
    user: AuthUser?,
    profileState: ProfileSheetState,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignOut: () -> Unit,
    onClearError: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable(user?.displayName) {
        mutableStateOf(user?.displayName ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, PastelLavender, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (user?.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = TablerIcons.User,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name (editable)
            if (isEditingName) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editedName.isNotBlank()) {
                                onUpdateName(editedName.trim())
                                isEditingName = false
                            }
                        },
                    ),
                    trailingIcon = {
                        if (profileState.isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { isEditingName = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                onUpdateName(editedName.trim())
                                isEditingName = false
                            }
                        },
                        enabled = editedName.isNotBlank() && !profileState.isUpdating,
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = user?.displayName ?: "No name set",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { isEditingName = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = TablerIcons.Edit,
                            contentDescription = "Edit name",
                            modifier = Modifier.size(16.dp),
                            tint = IconLavender,
                        )
                    }
                }
            }

            // Email
            val email = user?.email
            if (email != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Error message
            if (profileState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profileState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Settings row
            ProfileMenuItem(
                icon = TablerIcons.Settings,
                label = "Settings",
                iconColor = IconSky,
                onClick = onNavigateToSettings,
            )

            // Sign out row
            ProfileMenuItem(
                icon = TablerIcons.Logout,
                label = "Sign Out",
                iconColor = IconCoral,
                onClick = onSignOut,
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = TablerIcons.ArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Today's Habits Summary ─────────────────────────────────────────

@Composable
private fun TodayHabitsCard(state: SummaryDashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PastelRose),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IconRose.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TablerIcons.ListCheck,
                            contentDescription = null,
                            tint = IconRose,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Today's Habits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E2D2B),
                        )
                        Text(
                            text = "${state.completedHabits} of ${state.totalHabits} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5C5A57),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatPill(label = "Done", value = "${state.completedHabits}")
                StatPill(label = "Remaining", value = "${state.totalHabits - state.completedHabits}")
                StatPill(label = "Streak", value = "${state.bestCurrentStreak}")
                StatPill(label = "Score", value = "${(state.completionPercent * 100).toInt()}%")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C5A57),
                )
                Text(
                    text = "See Progress",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = IconRose,
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E2D2B),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5C5A57),
        )
    }
}

// ── Your Progress Card ─────────────────────────────────────────────

@Composable
private fun ProgressCard(state: SummaryDashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PastelLavender),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IconLavender.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = TablerIcons.ChartBar,
                            contentDescription = null,
                            tint = IconLavender,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E2D2B),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(state.completionPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2D2B),
                )

                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM")),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C5A57),
                )
            }

            // Circular ring with count inside
            Box(contentAlignment = Alignment.Center) {
                CompletionRing(
                    progress = state.completionPercent,
                    color = IconLavender,
                    size = 80.dp,
                    strokeWidth = 8.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.completedHabits}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E2D2B),
                    )
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5C5A57),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

// ── Category Cards ─────────────────────────────────────────────────

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E2D2B),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C5A57),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Check",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E2D2B),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = TablerIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF2E2D2B),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ── Activity Recap Card ────────────────────────────────────────────

@Composable
private fun ActivityRecapCard(
    state: SummaryDashboardUiState,
    onNavigateToHabitDetail: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "My Activity Recap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.latestInsight != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.latestInsight,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.topHabits.forEach { habit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToHabitDetail(habit.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(habit.color).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = habit.icon,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (habit.completedToday) PastelMint else PastelPeach,
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (habit.completedToday) "Done" else "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (habit.completedToday) IconMint else IconPeach,
                        )
                    }
                }
            }

            if (state.topHabits.isEmpty()) {
                Text(
                    text = "No habits tracked yet. Start building your routine!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Stat Cards ─────────────────────────────────────────────────────

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    backgroundColor: Color,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5C5A57),
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E2D2B),
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5C5A57),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

// ── Finance Card ───────────────────────────────────────────────────

@Composable
private fun FinanceCard(state: SummaryDashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PastelRose),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(IconRose.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TablerIcons.Wallet,
                        contentDescription = null,
                        tint = IconRose,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finance This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E2D2B),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5C5A57),
                    )
                    Text(
                        text = formatCurrency(state.monthlySpent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IconCoral,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5C5A57),
                    )
                    Text(
                        text = formatCurrency(state.monthlyIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IconMint,
                    )
                }
            }
        }
    }
}

// ── Life Dashboard Quick Access ────────────────────────────────────

@Composable
private fun LifeDashboardCard(onNavigateToLifeDashboard: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToLifeDashboard() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PastelSky),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IconSky.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TablerIcons.Trophy,
                    contentDescription = null,
                    tint = IconSky,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Life Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E2D2B),
                )
                Text(
                    text = "Quests, XP & player profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5C5A57),
                )
            }
            Icon(
                imageVector = TablerIcons.ArrowRight,
                contentDescription = null,
                tint = Color(0xFF2E2D2B),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
}

private fun formatCurrency(amount: Double): String {
    return "₹%.0f".format(amount)
}
