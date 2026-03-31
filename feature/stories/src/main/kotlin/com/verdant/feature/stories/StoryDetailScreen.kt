package com.verdant.feature.stories

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.model.StoryEvent
import com.verdant.core.model.StoryEventType
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowLeft
import compose.icons.tablericons.CurrencyRupee
import compose.icons.tablericons.Heart
import compose.icons.tablericons.ListCheck
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.MoodSmile
import compose.icons.tablericons.Pencil
import compose.icons.tablericons.PlayerPlay
import compose.icons.tablericons.Stars
import compose.icons.tablericons.Trash
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PastelTeal = Color(0xFFD0F0E8)
private val PastelViolet = Color(0xFFEDE4F7)
private val IconTeal = Color(0xFF26A69A)
private val IconViolet = Color(0xFF7C4DFF)
private val TimelineColor = Color(0xFFB2DFDB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.story?.title ?: "Story")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(TablerIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteStory()
                        onNavigateBack()
                    }) {
                        Icon(
                            TablerIcons.Trash,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val story = state.story ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Story header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelTeal),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = story.coverEmoji,
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E2D2B),
                        )
                        val template = story.template
                        if (template != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = IconTeal,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
                        Text(
                            text = dateFormat.format(Date(story.startTime)) +
                                (story.endTime?.let { " - ${dateFormat.format(Date(it))}" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5C5A57),
                        )
                    }
                }
            }

            // AI Summary card
            val aiSummary = story.aiSummary
            if (aiSummary != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PastelViolet),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    TablerIcons.Stars,
                                    contentDescription = null,
                                    tint = IconViolet,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E2D2B),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E2D2B),
                            )
                        }
                    }
                }
            }

            // AI Insights card
            val aiInsights = story.aiInsights
            if (aiInsights != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Behavioral Insights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiInsights,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Analyze button
            if (story.aiSummary == null) {
                item {
                    Button(
                        onClick = { viewModel.analyzeStory() },
                        enabled = !state.isAnalyzing && state.events.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IconViolet),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(TablerIcons.Stars, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze with AI")
                    }

                    val error = state.analyzeError
                    if (error != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Events timeline
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            itemsIndexed(state.events) { index, event ->
                TimelineEventRow(
                    event = event,
                    isFirst = index == 0,
                    isLast = index == state.events.lastIndex,
                )
            }

            if (state.events.isEmpty()) {
                item {
                    Text(
                        text = "No events in this story yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: StoryEvent,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp),
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(TimelineColor),
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(IconTeal),
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(TimelineColor),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Event content
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = eventIcon(event.eventType),
                    contentDescription = null,
                    tint = IconTeal,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    val desc = event.description
                    if (desc != null) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = timeFormat.format(Date(event.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun eventIcon(type: StoryEventType) = when (type) {
    StoryEventType.HABIT_COMPLETION -> TablerIcons.ListCheck
    StoryEventType.TRANSACTION -> TablerIcons.CurrencyRupee
    StoryEventType.HEALTH_METRIC -> TablerIcons.Heart
    StoryEventType.ACTIVITY -> TablerIcons.PlayerPlay
    StoryEventType.LOCATION_VISIT -> TablerIcons.MapPin
    StoryEventType.MOOD_CHECK -> TablerIcons.MoodSmile
    StoryEventType.NOTE -> TablerIcons.Pencil
}
