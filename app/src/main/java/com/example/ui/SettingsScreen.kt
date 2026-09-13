package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Settings Screen built with Material Design 3.
 * Allows users to configure:
 * 1. Double tap seek duration (5 to 60 seconds, default 10s) with live slider and preset chips.
 * 2. Long press speed multiplier (1.5x, 2x, 2.5x, 3x, default 2x) with YouTube-style preview.
 * 3. Reset to defaults option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsRepository = remember(context) { SettingsRepository(context) }

    val currentSeekDuration by settingsRepository.seekDurationSecondsFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.DEFAULT_SEEK_SECONDS)
    val currentSpeedMultiplier by settingsRepository.longPressSpeedMultiplierFlow
        .collectAsStateWithLifecycle(initialValue = SettingsRepository.DEFAULT_SPEED_MULTIPLIER)

    // Local state for smooth slider interaction without writing to disk on every frame
    var sliderSeekValue by remember(currentSeekDuration) {
        mutableFloatStateOf(currentSeekDuration.toFloat())
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("settings_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                settingsRepository.resetToDefaults()
                            }
                        },
                        modifier = Modifier.testTag("settings_reset_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(id = R.string.reset_defaults)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_content_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section Header
            item {
                Text(
                    text = stringResource(id = R.string.playback_settings_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Card 1: Double Tap Seek Duration
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seek_duration_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title row with icon & duration badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Timelapse,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.double_tap_seek_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "نطاق 5 إلى 60 ثانية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Current value badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("seek_duration_badge")
                            ) {
                                Text(
                                    text = stringResource(
                                        id = R.string.seconds_count,
                                        sliderSeekValue.roundToInt()
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.double_tap_seek_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        // Slider control (5 to 60 seconds)
                        Column {
                            Slider(
                                value = sliderSeekValue,
                                onValueChange = { newValue ->
                                    sliderSeekValue = newValue
                                },
                                onValueChangeFinished = {
                                    val finalValue = sliderSeekValue.roundToInt()
                                    coroutineScope.launch {
                                        settingsRepository.setSeekDurationSeconds(finalValue)
                                    }
                                },
                                valueRange = 5f..60f,
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("seek_duration_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "5 ثواني",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "60 ثانية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quick Presets Chips
                        Text(
                            text = "اختيارات سريعة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val seekPresets = listOf(5, 10, 15, 20, 30, 60)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(seekPresets) { preset ->
                                val isSelected = (currentSeekDuration == preset)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        sliderSeekValue = preset.toFloat()
                                        coroutineScope.launch {
                                            settingsRepository.setSeekDurationSeconds(preset)
                                        }
                                    },
                                    label = {
                                        Text(text = "${preset}s")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("seek_preset_chip_$preset")
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Long Press Speed Multiplier
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("long_press_speed_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title row with icon & current speed badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.long_press_speed_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تسريع مؤقت بأسلوب يوتيوب",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Current speed badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.testTag("long_press_speed_badge")
                            ) {
                                Text(
                                    text = formatSpeedLabel(currentSpeedMultiplier),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(id = R.string.long_press_speed_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        // Segmented choice chips for speed options (1.5x, 2x, 2.5x, 3x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsRepository.AVAILABLE_LONG_PRESS_SPEEDS.forEach { speed ->
                                val isSelected = (currentSpeedMultiplier == speed)
                                val speedLabel = formatSpeedLabel(speed)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            settingsRepository.setLongPressSpeedMultiplier(speed)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = speedLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("speed_option_${speed.toString().replace('.', '_')}")
                                )
                            }
                        }

                        // YouTube Style Preview Pill
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141414))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "معاينة المؤشر أثناء الضغط المطول على الفيديو:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.85f),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .testTag("speed_indicator_preview")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(
                                            id = R.string.speed_hud_text,
                                            formatSpeedLabel(currentSpeedMultiplier)
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Text(
                                text = "يتم الرجوع للسرعة السابقة فور رفع الإصبع عن الشاشة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Card 3: Reset to Defaults
            item {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            settingsRepository.resetToDefaults()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_defaults_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.reset_defaults))
                }
            }
        }
    }
}

private fun formatSpeedLabel(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}
