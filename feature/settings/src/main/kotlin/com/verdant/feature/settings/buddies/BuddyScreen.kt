package com.verdant.feature.settings.buddies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.social.BuddyConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuddyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Buddies") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openJoinDialog) {
                Icon(Icons.Default.GroupAdd, "Join with code")
            }
        },
        modifier = modifier,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.habits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Create some habits first to invite buddies",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Habits with buddies
            val habitsWithBuddies = state.habits.filter { state.buddiesByHabit.containsKey(it.id) }
            val habitsWithoutBuddies = state.habits.filter { !state.buddiesByHabit.containsKey(it.id) }

            if (habitsWithBuddies.isNotEmpty()) {
                item {
                    Text(
                        "Active Buddies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(habitsWithBuddies, key = { it.id }) { habit ->
                    HabitBuddyCard(
                        habitName = habit.name,
                        habitIcon = habit.icon,
                        buddies = state.buddiesByHabit[habit.id] ?: emptyList(),
                        onInvite = { viewModel.createInvite(habit.id, habit.name) },
                    )
                }
            }

            if (habitsWithoutBuddies.isNotEmpty()) {
                item {
                    Text(
                        "Invite a buddy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = if (habitsWithBuddies.isNotEmpty()) 8.dp else 0.dp),
                    )
                }
                items(habitsWithoutBuddies, key = { it.id }) { habit ->
                    InviteableHabitCard(
                        habitName = habit.name,
                        habitIcon = habit.icon,
                        onInvite = { viewModel.createInvite(habit.id, habit.name) },
                    )
                }
            }
        }
    }

    // Invite code dialog
    state.inviteCode?.let { code ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInviteDialog,
            title = { Text("Share this code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your buddy can join using this code:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                code,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (4).dp.value.sp,
                            )
                            Spacer(Modifier.width(12.dp))
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                            }) {
                                Icon(Icons.Default.ContentCopy, "Copy")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissInviteDialog) { Text("Done") }
            },
        )
    }

    // Join dialog
    if (state.joinDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::dismissJoinDialog,
            title = { Text("Join a buddy") },
            text = {
                Column {
                    Text("Enter the 6-digit invite code shared by your buddy:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.joinCode,
                        onValueChange = viewModel::onJoinCodeChange,
                        label = { Text("Invite code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = state.joinError != null,
                        supportingText = state.joinError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.joinSuccess) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Successfully joined!",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
            confirmButton = {
                if (state.joinSuccess) {
                    Button(onClick = viewModel::dismissJoinDialog) { Text("Done") }
                } else {
                    Button(onClick = viewModel::acceptInvite) { Text("Join") }
                }
            },
            dismissButton = {
                if (!state.joinSuccess) {
                    TextButton(onClick = viewModel::dismissJoinDialog) { Text("Cancel") }
                }
            },
        )
    }
}

@Composable
private fun HabitBuddyCard(
    habitName: String,
    habitIcon: String,
    buddies: List<BuddyConnection>,
    onInvite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(habitIcon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(habitName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onInvite) {
                    Icon(Icons.Default.PersonAdd, "Invite more", tint = MaterialTheme.colorScheme.primary)
                }
            }
            buddies.forEach { buddy ->
                BuddyRow(buddy)
            }
        }
    }
}

@Composable
private fun BuddyRow(buddy: BuddyConnection) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    buddy.buddyDisplayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(buddy.buddyDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (buddy.buddyStreak > 0) {
            Text(
                "${buddy.buddyStreak} day streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InviteableHabitCard(
    habitName: String,
    habitIcon: String,
    onInvite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(habitIcon.ifEmpty { "🌱" }, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(habitName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onInvite) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Invite")
            }
        }
    }
}

// Extension to support letter spacing in dp-based units
private inline val Float.sp get() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)
