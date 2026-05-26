package com.transitkit.app.ui.onboarding

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.ui.components.LocalHideBottomBarRequests
import kotlinx.coroutines.launch

/**
 * Onboarding stories-style: 4 pagine full-screen con progress bar in alto e
 * HorizontalPager per lo swipe.
 *
 * Compliance Google Play & UX: i prompt di sistema (POST_NOTIFICATIONS,
 * ACCESS_FINE_LOCATION) vengono richiesti SOLO al tap della CTA primaria,
 * dopo una pagina di pre-primer che spiega chiaramente il beneficio. Ogni
 * pagina permesso ha sempre un "Forse più tardi" non-penalty.
 */
@Composable
fun OnboardingScreen(
    prefs: SharedPreferences,
    onComplete: () -> Unit,
    onLocationGranted: () -> Unit = {},
    onNotificationsGranted: () -> Unit = {},
) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val closeLabel = stringResource(R.string.action_close)

    // Nasconde la bottom tab bar mentre l'onboarding è in scena. Il contatore
    // in MainActivity gestisce richieste sovrapposte (Scaffold collassa la bar
    // quando il counter è > 0).
    val hideBottomBar = LocalHideBottomBarRequests.current
    DisposableEffect(Unit) {
        hideBottomBar.value += 1
        onDispose { hideBottomBar.value -= 1 }
    }

    // ── Permission launchers ─────────────────────────────────────────────────
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onLocationGranted()
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onNotificationsGranted()
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    fun goNext() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            if (pagerState.currentPage < 3) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                markSeen(prefs)
                onComplete()
            }
        }
    }

    fun finish() {
        markSeen(prefs)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { testTag = "onboarding_stories" },
    ) {
        // Background shader brandizzato (stesso della Home) — continuità
        // visiva tra onboarding e app.
        com.transitkit.app.ui.home.OperatorMapBackground()

        // Fade verticale in basso: stacca la CTA dal background lasciando
        // la texture viva nella metà alta della pagina.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.background.copy(alpha = 0.85f),
                        ),
                        startY = 800f,
                    )
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top: progress bar + close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(4) { i ->
                    val active = i <= pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (active) colors.textPrimary.copy(alpha = 0.85f)
                                else colors.textPrimary.copy(alpha = 0.18f)
                            ),
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.bgSecondary)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            finish()
                        }
                        .semantics { contentDescription = closeLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.X),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) { page ->
                when (page) {
                    0 -> WelcomePage(onNext = ::goNext)
                    1 -> LocationPage(
                        onEnable = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val alreadyGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (alreadyGranted) {
                                onLocationGranted()
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        onSkip = ::goNext,
                    )
                    2 -> NotificationsPage(
                        onEnable = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    onNotificationsGranted()
                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                } else {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                // Pre-Android 13 → permission implicita
                                onNotificationsGranted()
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        onSkip = ::goNext,
                    )
                    3 -> DonePage(onFinish = ::finish)
                }
            }
        }
    }
}

// ── Pages ───────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    val colors = TransitTheme.colors
    val context = LocalContext.current
    val logoRes = remember(context) {
        context.resources.getIdentifier("app_logo", "drawable", context.packageName)
            .takeIf { it != 0 }
    }

    PageScaffold(
        primaryCta = stringResource(R.string.onb_welcome_cta),
        onPrimary = onNext,
        accent = colors.accent,
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.BusFront),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        PageText(
            title = stringResource(R.string.onb_welcome_title),
            body = stringResource(R.string.onb_welcome_body),
        )
    }
}

@Composable
private fun LocationPage(onEnable: () -> Unit, onSkip: () -> Unit) {
    val colors = TransitTheme.colors
    PageScaffold(
        primaryCta = stringResource(R.string.onb_location_cta),
        onPrimary = onEnable,
        secondaryCta = stringResource(R.string.onb_location_skip),
        onSecondary = onSkip,
        accent = colors.accent,
    ) {
        AccentIcon(icon = LucideIcons.MapPin, accent = colors.accent)
        Spacer(modifier = Modifier.height(28.dp))
        PageText(
            title = stringResource(R.string.onb_location_title),
            body = stringResource(R.string.onb_location_body),
        )
    }
}

@Composable
private fun NotificationsPage(onEnable: () -> Unit, onSkip: () -> Unit) {
    val colors = TransitTheme.colors
    PageScaffold(
        primaryCta = stringResource(R.string.onb_notif_cta),
        onPrimary = onEnable,
        secondaryCta = stringResource(R.string.onb_notif_skip),
        onSecondary = onSkip,
        accent = colors.accent,
    ) {
        AccentIcon(icon = LucideIcons.Bell, accent = colors.accent)
        Spacer(modifier = Modifier.height(28.dp))
        PageText(
            title = stringResource(R.string.onb_notif_title),
            body = stringResource(R.string.onb_notif_body),
        )
    }
}

@Composable
private fun DonePage(onFinish: () -> Unit) {
    val colors = TransitTheme.colors
    PageScaffold(
        primaryCta = stringResource(R.string.onb_done_cta),
        onPrimary = onFinish,
        accent = colors.accent,
    ) {
        AccentIcon(icon = LucideIcons.BusFront, accent = colors.accent)
        Spacer(modifier = Modifier.height(28.dp))
        PageText(
            title = stringResource(R.string.onb_done_title),
            body = stringResource(R.string.onb_done_body),
        )
    }
}

// ── Components ──────────────────────────────────────────────────────────────

@Composable
private fun PageScaffold(
    primaryCta: String,
    onPrimary: () -> Unit,
    accent: Color,
    secondaryCta: String? = null,
    onSecondary: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        content()
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = primaryCta,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (secondaryCta != null && onSecondary != null) {
            TextButton(
                onClick = onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = secondaryCta,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TransitTheme.colors.textSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AccentIcon(icon: Int, accent: Color) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun PageText(title: String, body: String) {
    val colors = TransitTheme.colors
    Text(
        text = title,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = body,
        fontSize = 16.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

// ── State helpers ───────────────────────────────────────────────────────────

private const val PREF_KEY_SEEN = "has_seen_onboarding"

fun shouldShowOnboarding(prefs: SharedPreferences): Boolean =
    !prefs.getBoolean(PREF_KEY_SEEN, false)

private fun markSeen(prefs: SharedPreferences) {
    prefs.edit().putBoolean(PREF_KEY_SEEN, true).apply()
}
