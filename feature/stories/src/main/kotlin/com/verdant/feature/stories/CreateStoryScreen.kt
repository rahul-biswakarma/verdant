package com.verdant.feature.stories

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.StoryEventType
import com.verdant.core.model.StoryTemplate
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.Check
import compose.icons.tablericons.X
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IconTeal = Color(0xFF26A69A)
private val PastelTeal = Color(0xFFD0F0E8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateStoryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CreateStoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.suggestEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Story") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Title
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Story Title") },
                    placeholder = { Text("e.g., Weekend Road Trip") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            // Template chips
            item {
                Text(
                    text = "Template (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StoryTemplate.entries.forEach { template ->
                        val label = template.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() }
                        val emoji = templateEmoji(template)
                        FilterChip(
                            selected = state.selectedTemplate == template,
                            onClick = {
                                viewModel.selectTemplate(
                                    if (state.selectedTemplate == template) null else template,
                                )
                            },
                            label = { Text("$emoji $label") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IconTeal.copy(alpha = 0.15f),
                                selectedLabelColor = IconTeal,
                            ),
                        )
                    }
                }
            }

            // Suggested events from existing data
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Suggested Events",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.isLoadingSuggestions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }

            if (state.suggestedEvents.isEmpty() && !state.isLoadingSuggestions) {
                item {
                    Text(
                        text = "No events found in this time range. Add events manually below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(state.suggestedEvents) { index, suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    onToggle = { viewModel.toggleSuggestion(index) },
                )
            }

            // Manual events
            item {
                Text(
                    text = "Added Events",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            itemsIndexed(state.events) { index, event ->
                ManualEventRow(
                    event = event,
                    onRemove = { viewModel.removeEvent(index) },
                )
            }

            // Add note button
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.addManualEvent(
                                title = "Note",
                                description = null,
                                type = StoryEventType.NOTE,
                            )
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "+", fontWeight = FontWeight.Bold, color = IconTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add a note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Save button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveStory(onComplete = onNavigateBack) },
                    enabled = state.title.isNotBlank() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IconTeal),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(TablerIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Story")
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SuggestedEvent,
    onToggle: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (suggestion.isSelected)
                PastelTeal else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = suggestion.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = dateFormat.format(Date(suggestion.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(IconTeal.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = suggestion.eventType.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = IconTeal,
                )
            }
        }
    }
}

@Composable
private fun ManualEventRow(
    event: DraftEvent,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (event.description != null) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    TablerIcons.X,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun templateEmoji(template: StoryTemplate) = when (template) {
    StoryTemplate.ROAD_TRIP -> "\uD83D\uDE97"
    StoryTemplate.WORKOUT_SESSION -> "\uD83C\uDFCB\uFE0F"
    StoryTemplate.SHOPPING_TRIP -> "\uD83D\uDED2"
    StoryTemplate.COMMUTE -> "\uD83D\uDE8C"
    StoryTemplate.WORK_DAY -> "\uD83D\uDCBC"
    StoryTemplate.EVENING_ROUTINE -> "\uD83C\uDF19"
    StoryTemplate.CUSTOM -> "\u2728"
}
