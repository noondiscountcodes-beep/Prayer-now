package com.example.ui

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SalawatConfig
import com.example.data.SalawatPreferencesRepository
import com.example.localization.AppLanguage
import com.example.media.MediaHelper
import com.example.media.SalawatAudioItem
import com.example.media.SalawatZipManager
import com.example.notifications.SalawatAlarmScheduler
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SalawatTabContent(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { SalawatPreferencesRepository(context) }

    var config by remember { mutableStateOf(repo.getConfig()) }
    var audioFiles by remember { mutableStateOf(SalawatZipManager.getAudioFiles(context)) }
    var lastAudioIndexState by remember { mutableStateOf(repo.getLastAudioIndex()) }
    val nextSequentialIndex = remember(audioFiles, lastAudioIndexState) {
        SalawatZipManager.getNextSequentialIndex(context)
    }
    var playingAudioName by remember { mutableStateOf<String?>(null) }
    var isExtracting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    fun setNextAudioIndex(index: Int) {
        val targetLastIndex = if (index <= 0) -1 else index - 1
        repo.setLastAudioIndex(targetLastIndex)
        lastAudioIndexState = targetLastIndex
    }

    // Live ticking clock for persistent countdown calculation
    var currentMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Save helper
    fun updateConfig(newConfig: SalawatConfig) {
        config = newConfig
        repo.setConfig(newConfig)
        SalawatAlarmScheduler.scheduleNext(context)
    }

    // ZIP file picker
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtracting = true
            statusMessage = if (language == AppLanguage.ARABIC) "جاري فك ضغط واستخراج ملفات الصوت..." else "Extracting audio files..."
            isErrorMessage = false

            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    SalawatZipManager.extractSalawatZip(context, uri, replaceExisting = false)
                }
                isExtracting = false
                audioFiles = SalawatZipManager.getAudioFiles(context)

                if (result.success) {
                    isErrorMessage = false
                    statusMessage = if (language == AppLanguage.ARABIC) {
                        "تم بنجاح استخراج ${result.extractedCount} ملف صوتي! الإجمالي: ${result.totalAvailable}"
                    } else {
                        "Successfully extracted ${result.extractedCount} audio files! Total: ${result.totalAvailable}"
                    }
                } else {
                    isErrorMessage = true
                    statusMessage = result.errorMessage ?: if (language == AppLanguage.ARABIC) "فشل استخراج الملف" else "Extraction failed"
                }
            }
        }
    }

    // Stop playback when tab leaves composition
    DisposableEffect(Unit) {
        onDispose {
            MediaHelper.stopAudioPreview()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("salawat_tab_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Header Banner & Hadith
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ProfessionalEmerald, ProfessionalEmeraldDark)
                            )
                        )
                        .border(1.dp, IslamicGoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(IslamicGoldPrimary.copy(alpha = 0.2f))
                                .border(1.5.dp, IslamicGoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ﷺ",
                                fontSize = 28.sp,
                                color = IslamicGoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (language == AppLanguage.ARABIC) "الصلاة على النبي ﷺ" else "Salawat on Prophet Muhammad ﷺ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (language == AppLanguage.ARABIC) {
                                "«مَنْ صَلَّى عَلَيَّ صَلَاةً صَلَّى اللهُ عَلَيْهِ بِهَا عَشْرًا»"
                            } else {
                                "\"Whoever sends blessings upon me once, Allah sends blessings upon him tenfold.\""
                            },
                            fontSize = 14.sp,
                            color = IslamicGoldLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = IslamicGoldPrimary.copy(alpha = 0.3f)
                        )

                        // Master Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "تفعيل التذكير بالصلاة على النبي" else "Enable Salawat Reminder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (config.isEnabled) {
                                        if (language == AppLanguage.ARABIC) "التنبيهات الدورية نشطة حالياً" else "Periodic reminders active"
                                    } else {
                                        if (language == AppLanguage.ARABIC) "التنبيهات متوقفة" else "Reminders paused"
                                    },
                                    fontSize = 12.sp,
                                    color = if (config.isEnabled) LedEmeraldAccent else PolishTextMuted
                                )
                            }

                            Switch(
                                checked = config.isEnabled,
                                onCheckedChange = { isChecked ->
                                    updateConfig(config.copy(isEnabled = isChecked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = IslamicGoldPrimary,
                                    checkedTrackColor = ProfessionalEmeraldLight,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.testTag("salawat_master_switch")
                            )
                        }
                    }
                }
            }
        }

        // 2. Persistent Countdown Banner ("شعار دائم يعد عد تنازلي للتذكير القادم")
        item {
            val targetTimeMillis = remember(config, currentMillis) {
                val scheduled = repo.getNextScheduledTriggerTime()
                if (config.isEnabled) {
                    if (scheduled > currentMillis) {
                        scheduled
                    } else {
                        repo.calculateNextTriggerTime(config, currentMillis)
                    }
                } else {
                    0L
                }
            }

            val remainingMillis = if (config.isEnabled && targetTimeMillis > currentMillis) {
                targetTimeMillis - currentMillis
            } else {
                0L
            }

            val totalIntervalMillis = (config.intervalMinutes * 60 * 1000L).coerceAtLeast(1L)
            val progress = if (config.isEnabled && remainingMillis > 0) {
                (1f - (remainingMillis.toFloat() / totalIntervalMillis.toFloat())).coerceIn(0f, 1f)
            } else {
                0f
            }

            val hours = (remainingMillis / (1000 * 60 * 60))
            val minutes = (remainingMillis / (1000 * 60)) % 60
            val seconds = (remainingMillis / 1000) % 60

            val formattedClockTime = remember(targetTimeMillis, language) {
                if (targetTimeMillis > 0L) {
                    val cal = Calendar.getInstance().apply { timeInMillis = targetTimeMillis }
                    val timeFormat = SimpleDateFormat("hh:mm a", if (language == AppLanguage.ARABIC) Locale("ar") else Locale.getDefault())
                    timeFormat.format(cal.time)
                } else {
                    ""
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("salawat_countdown_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (config.isEnabled) ProfessionalEmeraldBorder else PolishBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header of Countdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (config.isEnabled) ProfessionalEmeraldTint else PolishIconBox),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (config.isEnabled) ProfessionalEmerald else PolishTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "العد التنازلي للتذكير القادم" else "Countdown to Next Reminder",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = if (config.isEnabled && formattedClockTime.isNotEmpty()) {
                                        if (language == AppLanguage.ARABIC) "الموعد القادم: $formattedClockTime" else "Target time: $formattedClockTime"
                                    } else {
                                        if (language == AppLanguage.ARABIC) "التذكير متوقف حالياً" else "Reminder currently disabled"
                                    },
                                    fontSize = 12.sp,
                                    color = PolishTextSecondary
                                )
                            }
                        }

                        // Test button
                        FilledTonalButton(
                            onClick = {
                                SalawatAlarmScheduler.triggerNow(context)
                                Toast.makeText(
                                    context,
                                    if (language == AppLanguage.ARABIC) "تم إطلاق تذكير تجريبي بالصلاة على النبي ﷺ" else "Test reminder triggered",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = PolishGold.copy(alpha = 0.15f),
                                contentColor = ProfessionalEmeraldDark
                            ),
                            modifier = Modifier.testTag("salawat_test_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Test",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تجربة الآن" else "Test Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Digit Boxes (HH : MM : SS)
                    if (config.isEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountdownUnitBox(
                                value = hours,
                                label = if (language == AppLanguage.ARABIC) "ساعات" else "Hours"
                            )
                            Text(
                                text = ":",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfessionalEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            CountdownUnitBox(
                                value = minutes,
                                label = if (language == AppLanguage.ARABIC) "دقائق" else "Minutes"
                            )
                            Text(
                                text = ":",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfessionalEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            CountdownUnitBox(
                                value = seconds,
                                label = if (language == AppLanguage.ARABIC) "ثواني" else "Seconds"
                            )
                        }

                        // Linear progress indicator
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ProfessionalEmerald,
                                trackColor = PolishBorderLight
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "تقدّم الدورة" else "Cycle Progress",
                                    fontSize = 11.sp,
                                    color = PolishTextMuted
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfessionalEmerald
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PolishCanvasBg)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) {
                                    "قم بتفعيل مفتاح التذكير أعلاه لبدء العد التنازلي التلقائي."
                                } else {
                                    "Enable the reminder switch above to start the live countdown."
                                },
                                fontSize = 13.sp,
                                color = PolishTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    HorizontalDivider(color = PolishBorderLight)

                    // Persistent Notification Toggle ("شعار دائم في شريط التنبيهات")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PolishIconBox),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = PolishGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "شعار دائم في شريط الإشعارات" else "Persistent Status Bar Notification",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PolishTextPrimary
                                )
                                Text(
                                    text = if (language == AppLanguage.ARABIC) {
                                        "عرض عد تنازلي حي ومستمر في شريط التنبيهات حتى الموعد القادم"
                                    } else {
                                        "Show a persistent live countdown in the notification shade"
                                    },
                                    fontSize = 11.sp,
                                    color = PolishTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = config.showPersistentNotification,
                            onCheckedChange = { showNotif ->
                                updateConfig(config.copy(showPersistentNotification = showNotif))
                            },
                            enabled = config.isEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary),
                            modifier = Modifier.testTag("salawat_persistent_notif_switch")
                        )
                    }
                }
            }
        }

        // 3. Reminder Timing & Intervals Customization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, PolishBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ProfessionalEmerald
                        )
                        Text(
                            text = if (language == AppLanguage.ARABIC) "تحديد وقت وفترة التذكير" else "Reminder Timing & Interval",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                    }

                    // Interval description
                    val intervalText = remember(config.intervalMinutes, language) {
                        val m = config.intervalMinutes
                        if (language == AppLanguage.ARABIC) {
                            when {
                                m < 60 -> "كل $m دقيقة"
                                m == 60 -> "كل ساعة"
                                m % 60 == 0 -> "كل ${m / 60} ساعات"
                                else -> "كل ${m / 60} ساعة و ${m % 60} دقيقة"
                            }
                        } else {
                            when {
                                m < 60 -> "Every $m minutes"
                                m == 60 -> "Every hour"
                                m % 60 == 0 -> "Every ${m / 60} hours"
                                else -> "Every ${m / 60}h ${m % 60}m"
                            }
                        }
                    }

                    // Highlight card for current interval
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ProfessionalEmeraldTint)
                            .border(1.dp, ProfessionalEmeraldBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "الفترة الزمنية المختارة:" else "Selected Interval:",
                                    fontSize = 12.sp,
                                    color = PolishTextSecondary
                                )
                                Text(
                                    text = intervalText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfessionalEmerald
                                )
                            }

                            // Stepper (-5 / +5 min)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val newInterval = (config.intervalMinutes - 5).coerceAtLeast(5)
                                        updateConfig(config.copy(intervalMinutes = newInterval))
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PolishSurface)
                                        .border(1.dp, PolishBorder, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = PolishTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newInterval = (config.intervalMinutes + 5).coerceAtMost(720)
                                        updateConfig(config.copy(intervalMinutes = newInterval))
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PolishSurface)
                                        .border(1.dp, PolishBorder, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = PolishTextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Selection Chips
                    Text(
                        text = if (language == AppLanguage.ARABIC) "خيارات سريعة للتكرار:" else "Quick Preset Intervals:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextSecondary
                    )

                    val quickIntervals = listOf(15, 30, 45, 60, 90, 120, 180, 240)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickIntervals.forEach { minutes ->
                            val isSelected = config.intervalMinutes == minutes
                            val label = if (language == AppLanguage.ARABIC) {
                                when (minutes) {
                                    15 -> "15 دقيقة"
                                    30 -> "30 دقيقة"
                                    45 -> "45 دقيقة"
                                    60 -> "ساعة واحدة"
                                    90 -> "ساعة ونصف"
                                    120 -> "ساعتان"
                                    180 -> "3 ساعات"
                                    240 -> "4 ساعات"
                                    else -> "$minutes د"
                                }
                            } else {
                                when (minutes) {
                                    60 -> "1 Hour"
                                    120 -> "2 Hours"
                                    180 -> "3 Hours"
                                    240 -> "4 Hours"
                                    else -> "$minutes min"
                                }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    updateConfig(config.copy(intervalMinutes = minutes))
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ProfessionalEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Continuous Slider for custom minutes
                    Text(
                        text = if (language == AppLanguage.ARABIC) "أو حدد أي وقت تشاء بالسلايدر (من 5 إلى 360 دقيقة):" else "Or adjust smoothly with slider (5 to 360 min):",
                        fontSize = 12.sp,
                        color = PolishTextMuted
                    )

                    Slider(
                        value = config.intervalMinutes.toFloat().coerceIn(5f, 360f),
                        onValueChange = { newValue ->
                            val snapped = (Math.round(newValue / 5.0) * 5).toInt().coerceIn(5, 360)
                            updateConfig(config.copy(intervalMinutes = snapped))
                        },
                        valueRange = 5f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = ProfessionalEmerald,
                            activeTrackColor = ProfessionalEmerald,
                            inactiveTrackColor = PolishBorder
                        )
                    )

                    HorizontalDivider(color = PolishBorderLight)

                    // Active Hours Window (24h vs daytime range)
                    Text(
                        text = if (language == AppLanguage.ARABIC) "ساعات العمل والتفعيل:" else "Active Hours Window:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PolishTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { updateConfig(config.copy(isAllDay = true)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (config.isAllDay) {
                                ButtonDefaults.outlinedButtonColors(containerColor = ProfessionalEmerald, contentColor = Color.White)
                            } else {
                                ButtonDefaults.outlinedButtonColors(containerColor = PolishSurface, contentColor = PolishTextPrimary)
                            }
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "طوال اليوم (24 س)" else "All Day (24h)",
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { updateConfig(config.copy(isAllDay = false)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (!config.isAllDay) {
                                ButtonDefaults.outlinedButtonColors(containerColor = ProfessionalEmerald, contentColor = Color.White)
                            } else {
                                ButtonDefaults.outlinedButtonColors(containerColor = PolishSurface, contentColor = PolishTextPrimary)
                            }
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "ساعات محددة فقط" else "Custom Hours",
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Start and End time selection if custom range
                    AnimatedVisibility(visible = !config.isAllDay) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PolishCanvasBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "فترة التفعيل اليومية (لتجنب الإزعاج وقت النوم):" else "Daily active window (avoid disturbance during sleep):",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start Time Button
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                updateConfig(config.copy(startHour = hour, startMinute = minute))
                                            },
                                            config.startHour,
                                            config.startMinute,
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val formatted = String.format("%02d:%02d", config.startHour, config.startMinute)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (language == AppLanguage.ARABIC) "من الساعة" else "From", fontSize = 11.sp, color = PolishTextMuted)
                                        Text(formatted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // End Time Button
                                OutlinedButton(
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                updateConfig(config.copy(endHour = hour, endMinute = minute))
                                            },
                                            config.endHour,
                                            config.endMinute,
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val formatted = String.format("%02d:%02d", config.endHour, config.endMinute)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (language == AppLanguage.ARABIC) "إلى الساعة" else "To", fontSize = 11.sp, color = PolishTextMuted)
                                        Text(formatted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Friday only toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "التذكير في يوم الجمعة فقط" else "Remind on Fridays only",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                            Text(
                                text = if (language == AppLanguage.ARABIC) "تخصيص التذكير ليوم الجمعة المبارك" else "Limit reminders strictly to Fridays",
                                fontSize = 12.sp,
                                color = PolishTextSecondary
                            )
                        }

                        Switch(
                            checked = config.isFridayOnly,
                            onCheckedChange = { isFriday ->
                                updateConfig(config.copy(isFridayOnly = isFriday))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary)
                        )
                    }

                    // Vibration toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "الاهتزاز مع التنبيه" else "Vibrate on alert",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PolishTextPrimary
                            )
                        }

                        Switch(
                            checked = config.vibrate,
                            onCheckedChange = { vib ->
                                updateConfig(config.copy(vibrate = vib))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = IslamicGoldPrimary)
                        )
                    }
                }
            }
        }

        // 4. Audio ZIP Management & Custom Selection ("يختار أي صوت علي مزاجه من ملف zip")
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PolishSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, PolishBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = PolishGold
                        )
                        Text(
                            text = if (language == AppLanguage.ARABIC) "أصوات الصلاة على النبي (ملف ZIP)" else "Salawat Audios (ZIP File)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )
                    }

                    Text(
                        text = if (language == AppLanguage.ARABIC) {
                            "يمكنك استيراد ملف ZIP يحتوي على تسجيلات الصلاة على النبي، واختيار تشغيل أي صوت على مزاجك أو تركه يختار عشوائياً عند كل تذكير."
                        } else {
                            "Import a ZIP file with Salawat audio recordings. You can either select any specific audio you like, or let the app pick randomly on each reminder."
                        },
                        fontSize = 13.sp,
                        color = PolishTextSecondary,
                        lineHeight = 18.sp
                    )

                    // Audio Mode Switcher: Random vs Specific Sound ("علي مزاجه")
                    Text(
                        text = if (language == AppLanguage.ARABIC) "طريقة اختيار صوت التنبيه:" else "Audio Selection Mode:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PolishTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Option 1: Sequential (بالترتيب) - Default
                        OutlinedButton(
                            onClick = {
                                updateConfig(config.copy(audioSelectionMode = "SEQUENTIAL"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            colors = if (config.audioSelectionMode == "SEQUENTIAL") {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = ProfessionalEmerald,
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = PolishSurface,
                                    contentColor = PolishTextPrimary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "بالترتيب ﷺ" else "Sequential",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Option 2: Random (عشوائي)
                        OutlinedButton(
                            onClick = {
                                updateConfig(config.copy(audioSelectionMode = "RANDOM"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            colors = if (config.audioSelectionMode == "RANDOM") {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = ProfessionalEmerald,
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = PolishSurface,
                                    contentColor = PolishTextPrimary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "عشوائي" else "Random",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Option 3: Specific ("على مزاجي")
                        OutlinedButton(
                            onClick = {
                                val firstAvailable = audioFiles.firstOrNull()?.name
                                val targetName = config.selectedAudioFileName ?: firstAvailable
                                updateConfig(
                                    config.copy(
                                        audioSelectionMode = "SPECIFIC",
                                        selectedAudioFileName = targetName
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            colors = if (config.audioSelectionMode == "SPECIFIC") {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = ProfessionalEmerald,
                                    contentColor = Color.White
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = PolishSurface,
                                    contentColor = PolishTextPrimary
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "صوت محدد" else "Specific",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // Active Selection Summary Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (config.audioSelectionMode) {
                                    "SPECIFIC" -> PolishGold.copy(alpha = 0.12f)
                                    "RANDOM" -> PolishIconBox
                                    else -> ProfessionalEmeraldTint
                                }
                            )
                            .border(
                                1.dp,
                                when (config.audioSelectionMode) {
                                    "SPECIFIC" -> PolishGold.copy(alpha = 0.4f)
                                    "RANDOM" -> PolishBorderLight
                                    else -> ProfessionalEmeraldBorder
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (config.audioSelectionMode) {
                                        "SPECIFIC" -> Icons.Default.CheckCircle
                                        "RANDOM" -> Icons.Default.Shuffle
                                        else -> Icons.Default.Repeat
                                    },
                                    contentDescription = null,
                                    tint = when (config.audioSelectionMode) {
                                        "SPECIFIC" -> PolishGold
                                        "RANDOM" -> PolishTextSecondary
                                        else -> ProfessionalEmerald
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = when (config.audioSelectionMode) {
                                            "SPECIFIC" -> if (language == AppLanguage.ARABIC) "الصوت المعتمد حالياً للتذكير:" else "Active Reminder Sound:"
                                            "RANDOM" -> if (language == AppLanguage.ARABIC) "الوضع النشط: عشوائي في كل تذكير" else "Active Mode: Random Each Time"
                                            else -> if (language == AppLanguage.ARABIC) "الوضع النشط: بالترتيب (صوت مختلف كل مرة)" else "Active Mode: Sequential In Order"
                                        },
                                        fontSize = 11.sp,
                                        color = PolishTextSecondary
                                    )
                                    Text(
                                        text = when (config.audioSelectionMode) {
                                            "SPECIFIC" -> {
                                                config.selectedAudioFileName ?: if (language == AppLanguage.ARABIC) "اختر صوتاً من القائمة أدناه" else "Pick a sound below"
                                            }
                                            "RANDOM" -> {
                                                if (language == AppLanguage.ARABIC) "اختيار عشوائي بين جميع الملفات المتوفرة (${audioFiles.size})" else "Random among all available files (${audioFiles.size})"
                                            }
                                            else -> { // SEQUENTIAL
                                                if (audioFiles.isNotEmpty()) {
                                                    val nextAudio = audioFiles.getOrNull(nextSequentialIndex)
                                                    val nameStr = nextAudio?.name ?: ""
                                                    if (language == AppLanguage.ARABIC) {
                                                        "التالي بالترتيب: (#${nextSequentialIndex + 1}/${audioFiles.size}) $nameStr"
                                                    } else {
                                                        "Next in order: (#${nextSequentialIndex + 1}/${audioFiles.size}) $nameStr"
                                                    }
                                                } else {
                                                    if (language == AppLanguage.ARABIC) "سيعمل الترتيب فور استيراد ملفات ZIP" else "Order starts upon ZIP import"
                                                }
                                            }
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PolishTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick Actions on the active mode (Reset Order / Preview next)
                            if (config.audioSelectionMode == "SEQUENTIAL" && audioFiles.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Reset order to #1
                                    IconButton(
                                        onClick = {
                                            setNextAudioIndex(0)
                                            Toast.makeText(
                                                context,
                                                if (language == AppLanguage.ARABIC) "تم ضبط الترتيب ليبدأ من الصوت الأول (#1)" else "Order reset to sound #1",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(PolishSurface)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset order",
                                            tint = PolishTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Play next audio
                                    val nextAudio = audioFiles.getOrNull(nextSequentialIndex)
                                    if (nextAudio != null) {
                                        IconButton(
                                            onClick = {
                                                if (playingAudioName == nextAudio.name) {
                                                    MediaHelper.stopAudioPreview()
                                                    playingAudioName = null
                                                } else {
                                                    playingAudioName = nextAudio.name
                                                    MediaHelper.playAudioPreview(context, Uri.fromFile(nextAudio.file).toString()) {
                                                        playingAudioName = null
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(ProfessionalEmerald)
                                        ) {
                                            Icon(
                                                imageVector = if (playingAudioName == nextAudio.name) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "Play Next",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            } else if (config.audioSelectionMode == "SPECIFIC" && !config.selectedAudioFileName.isNullOrEmpty()) {
                                val selectedFile = SalawatZipManager.getAudioFileByName(context, config.selectedAudioFileName)
                                if (selectedFile != null) {
                                    IconButton(
                                        onClick = {
                                            if (playingAudioName == selectedFile.name) {
                                                MediaHelper.stopAudioPreview()
                                                playingAudioName = null
                                            } else {
                                                playingAudioName = selectedFile.name
                                                MediaHelper.playAudioPreview(context, Uri.fromFile(selectedFile).toString()) {
                                                    playingAudioName = null
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PolishSurface)
                                    ) {
                                        Icon(
                                            imageVector = if (playingAudioName == selectedFile.name) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Play Selected",
                                            tint = ProfessionalEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons: Import ZIP, Play Random, Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                zipPickerLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("import_salawat_zip_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfessionalEmerald)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.ARABIC) "استيراد ملف ZIP" else "Import ZIP",
                                fontSize = 13.sp
                            )
                        }

                        if (audioFiles.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val testAudio = when (config.audioSelectionMode) {
                                        "SEQUENTIAL" -> SalawatZipManager.peekNextSequentialAudio(context)
                                        "RANDOM" -> SalawatZipManager.getRandomAudio(context)
                                        else -> {
                                            if (!config.selectedAudioFileName.isNullOrEmpty()) {
                                                SalawatZipManager.getAudioFileByName(context, config.selectedAudioFileName)
                                            } else {
                                                SalawatZipManager.peekNextSequentialAudio(context)
                                            }
                                        }
                                    }
                                    if (testAudio != null && testAudio.exists()) {
                                        playingAudioName = testAudio.name
                                        MediaHelper.playAudioPreview(context, Uri.fromFile(testAudio).toString()) {
                                            playingAudioName = null
                                        }
                                        val toastLabel = when (config.audioSelectionMode) {
                                            "SEQUENTIAL" -> if (language == AppLanguage.ARABIC) "تشغيل الصوت التالي بالترتيب: ${testAudio.name}" else "Playing next in order: ${testAudio.name}"
                                            "RANDOM" -> if (language == AppLanguage.ARABIC) "تشغيل عشوائي: ${testAudio.name}" else "Playing random: ${testAudio.name}"
                                            else -> if (language == AppLanguage.ARABIC) "تشغيل المعتمد: ${testAudio.name}" else "Playing selected: ${testAudio.name}"
                                        }
                                        Toast.makeText(context, toastLabel, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (config.audioSelectionMode == "SEQUENTIAL") Icons.Default.Repeat else Icons.Default.Shuffle,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (config.audioSelectionMode == "SEQUENTIAL") ProfessionalEmerald else PolishGold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (config.audioSelectionMode) {
                                        "SEQUENTIAL" -> if (language == AppLanguage.ARABIC) "تجربة التالي" else "Test Next"
                                        "RANDOM" -> if (language == AppLanguage.ARABIC) "عشوائي" else "Random"
                                        else -> if (language == AppLanguage.ARABIC) "تجربة" else "Test"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = {
                                    SalawatZipManager.clearAllAudios(context)
                                    MediaHelper.stopAudioPreview()
                                    playingAudioName = null
                                    audioFiles = emptyList()
                                    updateConfig(config.copy(selectedAudioFileName = null))
                                    lastAudioIndexState = repo.getLastAudioIndex()
                                    statusMessage = if (language == AppLanguage.ARABIC) "تم حذف جميع الملفات الصوتية" else "All audio files deleted"
                                    isErrorMessage = false
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete all",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Status / Extracting Indicator
                    if (isExtracting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PolishIconBox)
                                .padding(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = statusMessage ?: "",
                                fontSize = 13.sp,
                                color = PolishTextPrimary
                            )
                        }
                    } else if (statusMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isErrorMessage) Color(0xFFFEE2E2) else ProfessionalEmeraldTint)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                fontSize = 12.sp,
                                color = if (isErrorMessage) Color(0xFF991B1B) else ProfessionalEmeraldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Audio Files Count & Active Filter Note
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.ARABIC) {
                                "الأصوات المتوفرة (${audioFiles.size}) - اضغط لاختيار الصوت"
                            } else {
                                "Available Audios (${audioFiles.size}) - Tap to select"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishTextPrimary
                        )

                        if (playingAudioName != null) {
                            TextButton(
                                onClick = {
                                    MediaHelper.stopAudioPreview()
                                    playingAudioName = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.ARABIC) "إيقاف الصوت" else "Stop Audio",
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (audioFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PolishCanvasBg)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicOff,
                                    contentDescription = null,
                                    tint = PolishTextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.ARABIC) {
                                        "لم يتم استيراد ملف ZIP بعد. اضغط على \"استيراد ملف ZIP\" لاختيار الأصوات المفضلة لديك."
                                    } else {
                                        "No ZIP imported yet. Tap \"Import ZIP\" to select your favorite Salawat recordings."
                                    },
                                    fontSize = 12.sp,
                                    color = PolishTextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Extracted Audio Items List with Sequential / Manual Selection
        if (audioFiles.isNotEmpty()) {
            itemsIndexed(audioFiles, key = { _, it -> it.name }) { index, audioItem ->
                val isCurrentSelected = config.audioSelectionMode == "SPECIFIC" && config.selectedAudioFileName == audioItem.name
                val isNextInSequence = config.audioSelectionMode == "SEQUENTIAL" && index == nextSequentialIndex
                AudioItemRow(
                    index = index,
                    totalCount = audioFiles.size,
                    audioItem = audioItem,
                    isPlaying = playingAudioName == audioItem.name,
                    isSelected = isCurrentSelected,
                    isNextInSequence = isNextInSequence,
                    audioSelectionMode = config.audioSelectionMode,
                    language = language,
                    onSelect = {
                        if (config.audioSelectionMode == "SEQUENTIAL") {
                            setNextAudioIndex(index)
                            Toast.makeText(
                                context,
                                if (language == AppLanguage.ARABIC) "تم ضبط هذا الصوت ليكون التالي بالترتيب (#${index + 1})" else "Set as next in sequence (#${index + 1})",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            updateConfig(
                                config.copy(
                                    audioSelectionMode = "SPECIFIC",
                                    selectedAudioFileName = audioItem.name
                                )
                            )
                            Toast.makeText(
                                context,
                                if (language == AppLanguage.ARABIC) "تم اعتماد: ${audioItem.name}" else "Selected: ${audioItem.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onPlayToggle = {
                        if (playingAudioName == audioItem.name) {
                            MediaHelper.stopAudioPreview()
                            playingAudioName = null
                        } else {
                            playingAudioName = audioItem.name
                            MediaHelper.playAudioPreview(context, Uri.fromFile(audioItem.file).toString()) {
                                playingAudioName = null
                            }
                        }
                    },
                    onDelete = {
                        if (playingAudioName == audioItem.name) {
                            MediaHelper.stopAudioPreview()
                            playingAudioName = null
                        }
                        SalawatZipManager.deleteAudio(audioItem.file)
                        audioFiles = SalawatZipManager.getAudioFiles(context)
                        if (config.selectedAudioFileName == audioItem.name) {
                            updateConfig(config.copy(selectedAudioFileName = null))
                        }
                        lastAudioIndexState = repo.getLastAudioIndex()
                    }
                )
            }
        }
    }
}

@Composable
private fun CountdownUnitBox(
    value: Long,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ProfessionalEmeraldTint)
                .border(1.5.dp, ProfessionalEmeraldBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%02d", value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ProfessionalEmeraldDark,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PolishTextSecondary
        )
    }
}

@Composable
private fun AudioItemRow(
    index: Int,
    totalCount: Int,
    audioItem: SalawatAudioItem,
    isPlaying: Boolean,
    isSelected: Boolean,
    isNextInSequence: Boolean,
    audioSelectionMode: String,
    language: AppLanguage,
    onSelect: () -> Unit,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isHighlighted = isSelected || isNextInSequence

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> IslamicGoldPrimary.copy(alpha = 0.12f)
                isNextInSequence -> ProfessionalEmeraldTint
                isPlaying -> ProfessionalEmeraldTint.copy(alpha = 0.5f)
                else -> PolishSurfaceCard
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isHighlighted) 1.5.dp else 1.dp,
            when {
                isSelected -> IslamicGoldPrimary
                isNextInSequence -> ProfessionalEmerald
                isPlaying -> ProfessionalEmerald
                else -> PolishBorderLight
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sequence Number Badge / Selection indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isNextInSequence -> ProfessionalEmerald
                            isSelected -> IslamicGoldPrimary
                            else -> PolishIconBox
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isNextInSequence || isSelected -> Color.White
                        else -> PolishTextSecondary
                    }
                )
            }

            // Play/Pause button
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) ProfessionalEmerald else PolishIconBox)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = if (isPlaying) Color.White else ProfessionalEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name and badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioItem.name,
                    fontSize = 13.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    color = PolishTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = audioItem.formattedSize,
                        fontSize = 11.sp,
                        color = PolishTextMuted
                    )
                    if (isNextInSequence && audioSelectionMode == "SEQUENTIAL") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ProfessionalEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "✓ التالي بالترتيب" else "✓ Next In Order",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfessionalEmeraldDark
                            )
                        }
                    } else if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(IslamicGoldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.ARABIC) "✓ المعتمد حالياً للتنبيه" else "✓ Active Sound",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishGold
                            )
                        }
                    }
                }
            }

            // Delete single audio
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = PolishTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
