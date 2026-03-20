package com.verdant.feature.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun CoachChatTab(
    state: ChatState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when a new message arrives
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {

        // ── Message list ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (state.messages.isEmpty()) {
                CoachEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = androidx.compose.foundation.layout.PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 12.dp,
                        bottom = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = state.messages,
                        key   = { _, bubble -> bubble.id },
                    ) { _, bubble ->
                        ChatBubbleItem(
                            bubble  = bubble,
                            onRetry = onRetry,
                        )
                    }
                }
            }

            // Clear button — top-right overlay
            this@Column.AnimatedVisibility(
                visible = state.messages.isNotEmpty(),
                enter   = fadeIn(),
                exit    = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector        = Icons.Default.DeleteSweep,
                        contentDescription = "Clear chat",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Surface(
            tonalElevation = 3.dp,
            color          = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value         = state.inputText,
                    onValueChange = onInputChanged,
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Ask your habit coach…") },
                    shape         = RoundedCornerShape(24.dp),
                    minLines      = 1,
                    maxLines      = 5,
                    enabled       = !state.isSending,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (state.inputText.isNotBlank()) onSend() },
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick  = onSend,
                    enabled  = state.inputText.isNotBlank() && !state.isSending,
                    modifier = Modifier.size(48.dp),
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor         = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    ),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Individual bubble ──────────────────────────────────────────────────────────

@Composable
private fun ChatBubbleItem(
    bubble: ChatBubble,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = bubble.role == "user"

    Row(
        modifier          = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {

        // Assistant avatar
        if (!isUser) {
            AssistantAvatar()
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier            = Modifier.widthIn(max = 300.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = 18.dp,
                            topEnd      = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd   = if (isUser) 4.dp else 18.dp,
                        )
                    )
                    .background(
                        when {
                            bubble.isError -> MaterialTheme.colorScheme.errorContainer
                            isUser         -> MaterialTheme.colorScheme.primary
                            else           -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                when {
                    bubble.isStreaming -> TypingIndicator()
                    bubble.isError    -> ErrorBubbleContent(
                        message = bubble.content,
                        onRetry = onRetry,
                    )
                    else              -> Text(
                        text  = bubble.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            bubble.isError -> MaterialTheme.colorScheme.onErrorContainer
                            isUser         -> MaterialTheme.colorScheme.onPrimary
                            else           -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        // User spacer (no avatar needed on right side)
        if (isUser) {
            Spacer(Modifier.width(4.dp))
        }
    }
}

// ── Assistant avatar ───────────────────────────────────────────────────────────

@Composable
private fun AssistantAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = Icons.Default.SmartToy,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier           = Modifier.size(16.dp),
        )
    }
}

// ── Typing indicator (three pulsing dots) ─────────────────────────────────────

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dot1Alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1",
    )
    val dot2Alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2",
    )
    val dot3Alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3",
    )

    val dotColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier            = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        listOf(dot1Alpha.value, dot2Alpha.value, dot3Alpha.value).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha)),
            )
        }
    }
}

// ── Error bubble content ───────────────────────────────────────────────────────

@Composable
private fun ErrorBubbleContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick  = onRetry,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                text  = "Retry",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────────

@Composable
private fun CoachEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier           = Modifier.size(32.dp),
                )
            }
            Text(
                text  = "Your habit coach is ready",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "Ask me anything about your habits — streaks, patterns, motivation, or how to build new routines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // Suggestion chips
            val suggestions = remember {
                listOf(
                    "How am I doing this week?",
                    "Which habit should I focus on?",
                    "Why do I keep missing Thursdays?",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    Surface(
                        shape  = RoundedCornerShape(50),
                        color  = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text     = suggestion,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
