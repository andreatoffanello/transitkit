package com.transitkit.app.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Inline "when" picker: a small row of pill chips. The first chip is the mode
 * (Now / Depart at / Arrive by) and opens a DropdownMenu. When the mode is not
 * Now, two more chips appear next to it — time and date — each opening a
 * Material3 picker dialog. No dedicated screen, no nav route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhenChipRow(
    selection: PlannerViewModel.WhenSelection,
    onSelectionChange: (PlannerViewModel.WhenSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var modeMenuOpen by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val modeLabel = when (selection.mode) {
        1 -> "Depart at"
        2 -> "Arrive by"
        else -> "Now"
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mode chip + dropdown
        Box {
            WhenChip(
                leadingIcon = LucideIcons.Clock,
                label = modeLabel,
                accent = selection.mode != 0,
                trailingChevron = true,
                onClick = { modeMenuOpen = true },
            )
            // Override the dropdown popup surface to use the app's bgSecondary
            // (Material3 default surfaceContainer is the M3 lavender that we
            // intentionally avoid in the rest of the app).
            val themed = MaterialTheme.colorScheme.copy(
                surface = TransitTheme.colors.bgSecondary,
                surfaceContainer = TransitTheme.colors.bgSecondary,
            )
            MaterialTheme(colorScheme = themed) {
                DropdownMenu(
                    expanded = modeMenuOpen,
                    onDismissRequest = { modeMenuOpen = false },
                ) {
                    listOf(0 to "Now", 1 to "Depart at", 2 to "Arrive by").forEach { (mode, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    label,
                                    color = TransitTheme.colors.textPrimary,
                                    fontWeight = if (mode == selection.mode) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                val newDate = if (selection.mode == 0 && mode != 0) Date() else selection.date
                                onSelectionChange(selection.copy(mode = mode, date = newDate))
                                modeMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        if (selection.mode != 0) {
            WhenChip(
                leadingIcon = null,
                label = timeFmt.format(selection.date),
                accent = true,
                trailingChevron = false,
                onClick = { showTimePicker = true },
            )
            WhenChip(
                leadingIcon = null,
                label = dateFmt.format(selection.date),
                accent = true,
                trailingChevron = false,
                onClick = { showDatePicker = true },
            )
        }
    }

    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { time = selection.date } }
        val tps = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        TimePickerDialogWrapper(
            onConfirm = {
                val c = Calendar.getInstance().apply {
                    time = selection.date
                    set(Calendar.HOUR_OF_DAY, tps.hour)
                    set(Calendar.MINUTE, tps.minute)
                    set(Calendar.SECOND, 0)
                }
                onSelectionChange(selection.copy(date = c.time))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        ) {
            val colors = TransitTheme.colors
            TimePicker(
                state = tps,
                colors = TimePickerDefaults.colors(
                    selectorColor = colors.accent,
                    containerColor = MaterialTheme.colorScheme.surface,
                    clockDialColor = colors.bgSecondary,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    clockDialUnselectedContentColor = colors.textPrimary,
                    timeSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.15f),
                    timeSelectorUnselectedContainerColor = colors.bgSecondary,
                    timeSelectorSelectedContentColor = colors.accent,
                    timeSelectorUnselectedContentColor = colors.textPrimary,
                ),
            )
        }
    }

    if (showDatePicker) {
        val dps = rememberDatePickerState(
            initialSelectedDateMillis = selection.date.time,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dps.selectedDateMillis ?: return@TextButton
                    val newCal = Calendar.getInstance().apply {
                        time = selection.date
                        val pickedCal = Calendar.getInstance().apply { timeInMillis = millis }
                        set(Calendar.YEAR, pickedCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, pickedCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, pickedCal.get(Calendar.DAY_OF_MONTH))
                    }
                    onSelectionChange(selection.copy(date = newCal.time))
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedDayContainerColor = TransitTheme.colors.accent,
                todayContentColor = TransitTheme.colors.accent,
                todayDateBorderColor = TransitTheme.colors.accent,
            ),
        ) {
            DatePicker(
                state = dps,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedDayContainerColor = TransitTheme.colors.accent,
                    todayContentColor = TransitTheme.colors.accent,
                    todayDateBorderColor = TransitTheme.colors.accent,
                ),
            )
        }
    }
}

@Composable
private fun WhenChip(
    leadingIcon: Int?,
    label: String,
    accent: Boolean,
    trailingChevron: Boolean,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bgSecondary,
        border = BorderStroke(0.5.dp, colors.glassBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = if (accent) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (accent) colors.textPrimary else colors.textSecondary,
            )
            if (trailingChevron) {
                Icon(
                    painter = painterResource(LucideIcons.ChevronDown),
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Small wrapper around [Dialog] that gives the embedded TimePicker the
 * familiar Material3 dialog chrome (Cancel/OK actions). M3 still doesn't ship
 * a public TimePickerDialog — this is the recommended workaround.
 */
@Composable
private fun TimePickerDialogWrapper(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
