package com.verdant.feature.habits.create

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Microphone
import compose.icons.tablericons.Send
import compose.icons.tablericons.Stars

@Composable
internal fun ConversationalEntrySection(
    inputText: String,
    isLoading: Boolean,
    error: String?,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onTemplateSelected: (HabitTemplate) -> Unit,
    onCreateManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        // Stars icon
        Icon(
            TablerIcons.Stars,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        // Prompt
        Text(
            text = "What do you want to\nbuild or track?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Describe it in your own words",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        // Large text input
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    "e.g., Run 5km every weekday morning",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (inputText.isNotBlank()) onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        Spacer(Modifier.height(12.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { /* TODO: voice input via SpeechRecognizer */ }) {
                Icon(
                    TablerIcons.Microphone,
                    contentDescription = "Voice input",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                IconButton(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank(),
                ) {
                    Icon(
                        TablerIcons.Send,
                        contentDescription = "Parse habit",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // Error message
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(32.dp))

        // Quick template chips
        Text(
            text = "Or pick a popular habit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            popularTemplates.forEach { template ->
                FilterChip(
                    selected = false,
                    onClick = { onTemplateSelected(template) },
                    label = { Text(template.name, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Text(template.icon, fontSize = 14.sp) },
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Create manually link
        TextButton(onClick = onCreateManually) {
            Text(
                "Create manually",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
