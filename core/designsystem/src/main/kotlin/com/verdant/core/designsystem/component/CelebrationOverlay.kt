package com.verdant.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

data class CelebrationData(
    val habitName: String,
    val milestone: Int,
    val message: String,
)

@Composable
fun CelebrationOverlay(
    celebration: CelebrationData,
    onDismiss: () -> Unit,
) {
    val particles = remember { List(80) { ConfettiParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "time",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = p.startX * size.width + sin(time * p.speed * 6.28f + p.phase) * p.amplitude * size.width
                val y = (p.startY + time * p.fallSpeed) % 1f * size.height
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(x, y),
                )
            }
        }

        // Content card
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = milestoneEmoji(celebration.milestone),
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${celebration.milestone}-Day Streak!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = celebration.habitName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = celebration.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text("Continue")
                }
            }
        }
    }
}

private fun milestoneEmoji(milestone: Int): String = when {
    milestone >= 365 -> "\uD83C\uDFC6"
    milestone >= 100 -> "\uD83D\uDCAF"
    milestone >= 30 -> "\uD83D\uDD25"
    milestone >= 7 -> "\u2B50"
    else -> "\uD83C\uDF31"
}

private data class ConfettiParticle(
    val startX: Float = Random.nextFloat(),
    val startY: Float = Random.nextFloat() * -0.5f,
    val speed: Float = 0.5f + Random.nextFloat(),
    val fallSpeed: Float = 0.3f + Random.nextFloat() * 0.7f,
    val amplitude: Float = 0.02f + Random.nextFloat() * 0.05f,
    val phase: Float = Random.nextFloat() * 6.28f,
    val size: Float = 3f + Random.nextFloat() * 6f,
    val color: Color = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFF6BCB77),
        Color(0xFF4D96FF), Color(0xFFFF8CC8), Color(0xFFA66CFF),
    ).random(),
)
