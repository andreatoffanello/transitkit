package com.transitkit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.transitkit.app.config.TransitTheme
import kotlin.math.roundToInt

/**
 * iOS-style vertical wheel picker. Items scroll vertically and snap to the
 * center slot, which is highlighted with a subtle accent band. The centered
 * item is the selection; crossing a slot fires a light haptic tick.
 *
 * No native Material wheel exists, so this is a LazyColumn with snap fling +
 * symmetric content padding so the first/last items can reach the center.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val edge = visibleCount / 2

    // The centered item index = first visible index + rounded fraction scrolled.
    val centerIndex by remember {
        derivedStateOf {
            val itemPx = with(density) { itemHeight.toPx() }
            val raw = listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset / itemPx).roundToInt()
            raw.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices && centerIndex != selectedIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectedIndexChange(centerIndex)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        // Selection band centered on the middle slot.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight * edge),
        ) {
            itemsIndexed(items) { index, label ->
                val selected = index == centerIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (selected) colors.textPrimary else colors.textTertiary,
                    )
                }
            }
        }
    }
}

/**
 * Hour + minute wheels (24h) with a fixed colon separator, matching the iOS
 * wheel time picker. Indices map directly to hour (0..23) and minute (0..59).
 */
@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val hours = remember { (0..23).map { "%02d".format(it) } }
    val minutes = remember { (0..59).map { "%02d".format(it) } }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelPicker(
            items = hours,
            selectedIndex = hour,
            onSelectedIndexChange = { onChange(it, minute) },
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        WheelPicker(
            items = minutes,
            selectedIndex = minute,
            onSelectedIndexChange = { onChange(hour, it) },
            modifier = Modifier.width(72.dp),
        )
    }
}
