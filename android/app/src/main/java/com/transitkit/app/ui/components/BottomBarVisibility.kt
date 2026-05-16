package com.transitkit.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Counter — incremented by full-screen overlays inside a tab destination that
 * want the activity's bottom navigation hidden while they are visible. The
 * activity reads this in MainActivity and renders the tab bar only when the
 * counter is zero.
 *
 * Counter (not boolean) so multiple overlays can stack without races on
 * dismiss.
 */
val LocalHideBottomBarRequests = staticCompositionLocalOf<MutableState<Int>> {
    error("LocalHideBottomBarRequests not provided")
}

/**
 * Side-effect that requests the tab bar be hidden for the lifetime of the
 * composable, then restores it on dispose.
 */
@Composable
fun HideBottomBarWhileVisible() {
    val counter = LocalHideBottomBarRequests.current
    DisposableEffect(Unit) {
        counter.value += 1
        onDispose { counter.value -= 1 }
    }
}
