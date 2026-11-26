package com.perspectivelive.wallpaper.settings.ui

import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.perspectivelive.wallpaper.data.DayCounterMode
import com.perspectivelive.wallpaper.data.UserPreferences
import com.perspectivelive.wallpaper.service.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_LIFESPAN = 40f
private const val MAX_LIFESPAN = 120f
private const val PERCENT_MULTIPLIER = 100f

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

private enum class ActiveDateDialog {
    NONE,
    BIRTH_DATE,
    START_DATE,
    TARGET_DATE
}

@Composable
fun HorizonPanel(
    prefs: UserPreferences,
    mode: WallpaperMode,
    onBirthDateSelected: (LocalDate) -> Unit,
    onLifespanChanged: (Int) -> Unit,
    onSelectNoTomorrow: () -> Unit,
    onSelectVsYesterday: () -> Unit,
    onSelectCustomObjective: () -> Unit,
    onEventNameChanged: (String) -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    onTargetDateSelected: (LocalDate) -> Unit,
    onHealthMetricSelected: (HealthMetricSpec) -> Unit,
    onHealthGoalChanged: (Float) -> Unit,
    onShowStatOverlayChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDialog by remember { mutableStateOf(ActiveDateDialog.NONE) }

    when (mode) {
        WallpaperMode.MACRO -> MacroHorizonContent(
            prefs = prefs,
            onOpenBirthDatePicker = { activeDialog = ActiveDateDialog.BIRTH_DATE },
            onLifespanChanged = onLifespanChanged,
            modifier = modifier
        )

        WallpaperMode.MICRO -> MicroHorizonContent(
            prefs = prefs,
            onSelectNoTomorrow = onSelectNoTomorrow,
            onSelectVsYesterday = onSelectVsYesterday,
            onSelectCustomObjective = onSelectCustomObjective,
            onEventNameChanged = onEventNameChanged,
            onOpenStartDatePicker = { activeDialog = ActiveDateDialog.START_DATE },
            onOpenTargetDatePicker = { activeDialog = ActiveDateDialog.TARGET_DATE },
            onHealthMetricSelected = onHealthMetricSelected,
            onHealthGoalChanged = onHealthGoalChanged,
            onShowStatOverlayChanged = onShowStatOverlayChanged,
            modifier = modifier
        )
    }

    if (activeDialog != ActiveDateDialog.NONE) {
        val initialDate = when (activeDialog) {
            ActiveDateDialog.BIRTH_DATE -> prefs.birthDate
            ActiveDateDialog.START_DATE -> prefs.countdownStartDate ?: LocalDate.now()
            ActiveDateDialog.TARGET_DATE -> prefs.eventDate ?: LocalDate.now()
            ActiveDateDialog.NONE -> LocalDate.now()
        }
        ExpressiveDatePickerDialog(
            initialDate = initialDate,
            onConfirm = { picked ->
                when (activeDialog) {
                    ActiveDateDialog.BIRTH_DATE -> onBirthDateSelected(picked)
                    ActiveDateDialog.START_DATE -> onStartDateSelected(picked)
                    ActiveDateDialog.TARGET_DATE -> onTargetDateSelected(picked)
                    ActiveDateDialog.NONE -> Unit
                }
                activeDialog = ActiveDateDialog.NONE
            },
            onDismiss = { activeDialog = ActiveDateDialog.NONE }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MacroHorizonContent(
    prefs: UserPreferences,
    onOpenBirthDatePicker: () -> Unit,
    onLifespanChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val yearsLived = ChronoUnit.YEARS.between(prefs.birthDate, today)
        .toInt()
        .coerceIn(0, prefs.expectedLifespan)
    val fraction = if (prefs.expectedLifespan > 0) {
        (yearsLived.toFloat() / prefs.expectedLifespan.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val pctFormatted = String.format(Locale.US, "%.1f%%", fraction * PERCENT_MULTIPLIER)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Non-redundant Life Horizon progress card with % lived + LinearWavyProgressIndicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = pctFormatted,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "lived",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = "$yearsLived / ${prefs.expectedLifespan} yrs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                LinearWavyProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Segmented List Group: Birth Date + Life Expectancy
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBirthDatePicker),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Birth Date",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = prefs.birthDate.format(DATE_FORMATTER),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Life Expectancy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${prefs.expectedLifespan} yrs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val lifespanFloat = prefs.expectedLifespan.toFloat().coerceIn(MIN_LIFESPAN, MAX_LIFESPAN)
                    val sliderState = rememberSliderState(lifespanFloat, 0, MIN_LIFESPAN..MAX_LIFESPAN)
                    LaunchedEffect(lifespanFloat) {
                        if (sliderState.value != lifespanFloat) {
                            sliderState.value = lifespanFloat
                        }
                    }
                    Slider(
                        state = sliderState,
                        onValueChange = { onLifespanChanged(it.roundToInt()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MicroHorizonContent(
    prefs: UserPreferences,
    onSelectNoTomorrow: () -> Unit,
    onSelectVsYesterday: () -> Unit,
    onSelectCustomObjective: () -> Unit,
    onEventNameChanged: (String) -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onOpenTargetDatePicker: () -> Unit,
    onHealthMetricSelected: (HealthMetricSpec) -> Unit,
    onHealthGoalChanged: (Float) -> Unit,
    onShowStatOverlayChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(
        DayCounterMode.NO_TOMORROW to "No Tomorrow",
        DayCounterMode.VS_YESTERDAY to "vs Yesterday",
        DayCounterMode.STATIC to "Objective"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Momentum Mode Connected ButtonGroup
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            modes.forEachIndexed { index, (modeId, label) ->
                ToggleButton(
                    checked = prefs.dayCounterMode == modeId,
                    onCheckedChange = { checked ->
                        if (checked) {
                            when (modeId) {
                                DayCounterMode.NO_TOMORROW -> onSelectNoTomorrow()
                                DayCounterMode.VS_YESTERDAY -> onSelectVsYesterday()
                                else -> onSelectCustomObjective()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        // Compose 1.13 CapturedAnimatedVisibility for Objective fields
        CapturedAnimatedVisibility(
            visible = prefs.dayCounterMode == DayCounterMode.STATIC,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ObjectiveConfigCard(
                prefs = prefs,
                onEventNameChanged = onEventNameChanged,
                onOpenStartDatePicker = onOpenStartDatePicker,
                onOpenTargetDatePicker = onOpenTargetDatePicker
            )
        }

        // Health Connect Card (Micro exclusive)
        HealthConnectCard(
            prefs = prefs,
            onHealthMetricSelected = onHealthMetricSelected,
            onHealthGoalChanged = onHealthGoalChanged,
            onShowStatOverlayChanged = onShowStatOverlayChanged
        )
    }
}

@Composable
private fun ObjectiveConfigCard(
    prefs: UserPreferences,
    onEventNameChanged: (String) -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onOpenTargetDatePicker: () -> Unit
) {
    val nameState = rememberTextFieldState(initialText = prefs.eventName.orEmpty())
    LaunchedEffect(nameState.text) {
        val current = nameState.text.toString()
        if (current != prefs.eventName.orEmpty()) {
            onEventNameChanged(current)
        }
    }

    val daysRemaining = prefs.eventDate?.let { target ->
        ChronoUnit.DAYS.between(LocalDate.now(), target)
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    state = nameState,
                    label = { Text("Objective") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                if (daysRemaining != null) {
                    val badgeText = if (daysRemaining < 0) "${-daysRemaining}d ago" else "${daysRemaining}d left"
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DatePillButton(
                    label = "Start",
                    date = prefs.countdownStartDate ?: LocalDate.now(),
                    onClick = onOpenStartDatePicker,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                DatePillButton(
                    label = "Target",
                    date = prefs.eventDate ?: LocalDate.now(),
                    onClick = onOpenTargetDatePicker,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DatePillButton(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date.format(DATE_FORMATTER),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HealthConnectCard(
    prefs: UserPreferences,
    onHealthMetricSelected: (HealthMetricSpec) -> Unit,
    onHealthGoalChanged: (Float) -> Unit,
    onShowStatOverlayChanged: (Boolean) -> Unit
) {
    val activeSpec = HealthMetricCatalog.find(prefs.healthMetric)
    val isActive = activeSpec.id != HealthConnectManager.METRIC_NONE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Health Connect",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val next = (prefs.healthMetricGoal - activeSpec.step).coerceAtLeast(activeSpec.step)
                                onHealthGoalChanged(next)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease goal",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        val formattedGoal = if (activeSpec.step < 1f) {
                            String.format(Locale.US, "%.1f %s", prefs.healthMetricGoal, activeSpec.unit)
                        } else {
                            String.format(Locale.US, "%,d %s", prefs.healthMetricGoal.roundToInt(), activeSpec.unit)
                        }
                        Text(
                            text = formattedGoal,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalIconButton(
                            onClick = { onHealthGoalChanged(prefs.healthMetricGoal + activeSpec.step) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase goal",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Shape-morphing Health Connect metric toggle buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HealthMetricCatalog.all.forEach { spec ->
                    val checked = prefs.healthMetric == spec.id
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = { if (it) onHealthMetricSelected(spec) },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(text = spec.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            CapturedAnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show stat overlay in dots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpressiveCheckSwitch(
                        checked = prefs.showStatOverlay,
                        onCheckedChange = onShowStatOverlayChanged
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveCheckSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onConfirm(picked)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
