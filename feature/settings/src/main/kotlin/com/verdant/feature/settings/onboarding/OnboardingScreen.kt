package com.verdant.feature.settings.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.pager.HorizontalPager
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdant.core.designsystem.theme.BurntOrange
import com.verdant.core.designsystem.theme.DarkPeach
import com.verdant.core.designsystem.theme.DarkSage
import com.verdant.core.designsystem.theme.DarkSurfaceVariant
import com.verdant.core.designsystem.theme.DeepCharcoal
import com.verdant.core.designsystem.theme.MutedSage
import kotlin.math.absoluteValue
private data class OnboardingPageData(
    val emoji: String,
    val circleColorLight: Color,
    val circleColorDark: Color,
    val headline: String,
    val description: String,
)

private val pages = listOf(
    OnboardingPageData(
        emoji = "\uD83C\uDF3F",
        circleColorLight = DeepCharcoal,
        circleColorDark = DarkSurfaceVariant,
        headline = "Build habits\nthat actually\nstick.",
        description = "Track, understand, and grow your daily routines with Verdant.",
    ),
    OnboardingPageData(
        emoji = "✨",
        circleColorLight = MutedSage,
        circleColorDark = DarkSage,
        headline = "Simple habit\ntracking",
        description = "Log any habit in seconds. From daily check-ins to detailed measurements.",
    ),
    OnboardingPageData(
        emoji = "\uD83E\uDDE0",
        circleColorLight = BurntOrange,
        circleColorDark = DarkPeach,
        headline = "AI-powered\ninsights",
        description = "Verdant learns your patterns and gives you personalized insights.",
    ),
    OnboardingPageData(
        emoji = "\uD83D\uDD14",
        circleColorLight = DeepCharcoal,
        circleColorDark = DarkSurfaceVariant,
        headline = "Gentle\nreminders",
        description = "Smart notifications that know when and how to nudge you.",
    ),
)

private const val PAGE_COUNT = 4
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    webClientId: String = "",
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val isDark = isSystemInDarkTheme()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // When the user signs in during onboarding, mark it done and navigate to home.
    // wasSignedInAtStart prevents auto-navigating if the user was already signed in.
    val wasSignedInAtStart = remember { isSignedIn }
    LaunchedEffect(isSignedIn) {
        if (isSignedIn && !wasSignedInAtStart) {
            viewModel.markOnboardingDone()
            onComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, top = 56.dp, end = 28.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDF3F",
                fontSize = 22.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Verdant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true,
        ) { page ->
            OnboardingPage(
                data = pages[page],
                pageIndex = page,
                pagerState = pagerState,
                isDark = isDark,
            )
        }
        BottomBar(
            currentPage = pagerState.currentPage,
            pageCount = PAGE_COUNT,
            isSignedIn = isSignedIn,
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onSignIn = { viewModel.signInWithGoogle(context, webClientId) },
            onGetStarted = { viewModel.markOnboardingDone(); onComplete() },
        )
    }
}
@Composable
private fun OnboardingPage(
    data: OnboardingPageData,
    pageIndex: Int,
    pagerState: PagerState,
    isDark: Boolean,
) {
    // Compute how far this page is from the settled position (-1..0..+1)
    val pageOffset = ((pagerState.currentPage - pageIndex)
            + pagerState.currentPageOffsetFraction)

    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)

    // Circle scale: bounces in from 0.85
    val circleScale by animateFloatAsState(
        targetValue = 1f - (absOffset * 0.15f),
        animationSpec = spring(dampingRatio = 0.7f),
        label = "circleScale",
    )

    // Parallax offsets (px)
    val circleTranslateY = absOffset * 30f
    val headlineTranslateY = absOffset * 50f
    val descTranslateY = absOffset * 70f
    val contentAlpha = 1f - (absOffset * 0.4f)

    val circleColor = if (isDark) data.circleColorDark else data.circleColorLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.weight(0.06f))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    scaleX = circleScale
                    scaleY = circleScale
                    translationY = circleTranslateY
                    alpha = contentAlpha
                }
                .size(260.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = data.emoji,
                fontSize = 80.sp,
            )
        }

        Spacer(Modifier.height(44.dp))
        Text(
            text = data.headline,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = headlineTranslateY
                    alpha = contentAlpha
                },
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = data.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = descTranslateY
                    alpha = contentAlpha
                },
        )

        Spacer(Modifier.weight(0.15f))
    }
}
@Composable
private fun BottomBar(
    currentPage: Int,
    pageCount: Int,
    isSignedIn: Boolean,
    onNext: () -> Unit,
    onSignIn: () -> Unit,
    onGetStarted: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Action button — centered
        val isLastPage = currentPage == pageCount - 1
        Crossfade(
            targetState = isLastPage,
            animationSpec = tween(250),
            label = "bottomAction",
        ) { last ->
            if (last) {
                Button(
                    onClick = if (isSignedIn) onGetStarted else onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = if (isSignedIn) "Get Started" else "Sign in to get started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Page indicator — bottom center
        PageIndicatorDots(
            currentPage = currentPage,
            pageCount = pageCount,
        )
    }
}
@Composable
private fun PageIndicatorDots(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = currentPage == index

            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(300),
                label = "dotWidth",
            )

            val color by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = tween(300),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}
