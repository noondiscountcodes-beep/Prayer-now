package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.AlertItem
import com.example.engine.PrayerType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.MediaHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlertDialog(
    initialAlert: AlertItem?,
    language: AppLanguage,
    onSave: (AlertItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var targetPrayer by remember { mutableStateOf(initialAlert?.targetPrayer ?: "ALL") }
    var minutesBefore by remember { mutableStateOf(initialAlert?.minutesBefore ?: 15) }
    var customMinutesText by remember { mutableStateOf(minutesBefore.toString()) }
    var selectedDays by remember {
        mutableStateOf(
            initialAlert?.daysOfWeek ?: setOf(
                Calendar.SATURDAY, Calendar.SUNDAY, Calendar.MONDAY,
                Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
    }
    var soundUriString by remember { mutableStateOf(initialAlert?.soundUri) }
    var soundName by remember { mutableStateOf(initialAlert?.soundName) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    // Audio file picker launcher (Zero storage permissions!)
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = false)
            soundUriString = uri.toString()
            soundName = meta.displayName
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MediaHelper.stopAudioPreview()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp)
                .testTag("add_edit_alert_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialAlert == null) {
                        if (language.code == "ar") "إضافة تنبيه جديد" else "Add New Alert"
                    } else {
                        if (language.code == "ar") "تعديل التنبيه" else "Edit Alert"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Target Prayer
                Text(
                    text = if (language.code == "ar") "الصلاة المستهدفة:" else "Target Prayer:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val prayerOptions = listOf(
                        "ALL" to if (language.code == "ar") "الكل" else "All",
                        "FAJR" to AppStrings.getPrayerName(PrayerType.FAJR, language),
                        "DHUHR" to AppStrings.getPrayerName(PrayerType.DHUHR, language),
                        "ASR" to AppStrings.getPrayerName(PrayerType.ASR, language),
                        "MAGHRIB" to AppStrings.getPrayerName(PrayerType.MAGHRIB, language),
                        "ISHA" to AppStrings.getPrayerName(PrayerType.ISHA, language)
                    )
                    prayerOptions.take(3).forEach { (id, label) ->
                        FilterChip(
                            selected = targetPrayer == id,
                            onClick = { targetPrayer = id },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val prayerOptions2 = listOf(
                        "ASR" to AppStrings.getPrayerName(PrayerType.ASR, language),
                        "MAGHRIB" to AppStrings.getPrayerName(PrayerType.MAGHRIB, language),
                        "ISHA" to AppStrings.getPrayerName(PrayerType.ISHA, language)
                    )
                    prayerOptions2.forEach { (id, label) ->
                        FilterChip(
                            selected = targetPrayer == id,
                            onClick = { targetPrayer = id },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Minutes Before
                Text(
                    text = if (language.code == "ar") "وقت التنبيه قبل الأذان:" else "Minutes Before Prayer:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                val minutePresets = listOf(1, 5, 10, 15, 30, 60)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    minutePresets.take(3).forEach { min ->
                        FilterChip(
                            selected = minutesBefore == min,
                            onClick = {
                                minutesBefore = min
                                customMinutesText = min.toString()
                            },
                            label = { Text("$min m") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    minutePresets.drop(3).forEach { min ->
                        FilterChip(
                            selected = minutesBefore == min,
                            onClick = {
                                minutesBefore = min
                                customMinutesText = min.toString()
                            },
                            label = { Text("$min m") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = {
                        customMinutesText = it
                        it.toIntOrNull()?.let { v -> if (v > 0) minutesBefore = v }
                    },
                    label = { Text(if (language.code == "ar") "دقائق مخصصة" else "Custom minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Days of the week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "أيام التكرار:" else "Repeat Days:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(onClick = {
                        selectedDays = if (selectedDays.size == 7) emptySet() else setOf(1, 2, 3, 4, 5, 6, 7)
                    }) {
                        Text(if (selectedDays.size == 7) (if (language.code == "ar") "إلغاء الكل" else "Clear All") else (if (language.code == "ar") "تحديد الكل" else "All Days"))
                    }
                }

                val allWeekDays = listOf(
                    Calendar.SATURDAY to if (language.code == "ar") "سبت" else "Sat",
                    Calendar.SUNDAY to if (language.code == "ar") "أحد" else "Sun",
                    Calendar.MONDAY to if (language.code == "ar") "إثنين" else "Mon",
                    Calendar.TUESDAY to if (language.code == "ar") "ثلاثاء" else "Tue",
                    Calendar.WEDNESDAY to if (language.code == "ar") "أربعاء" else "Wed",
                    Calendar.THURSDAY to if (language.code == "ar") "خميس" else "Thu",
                    Calendar.FRIDAY to if (language.code == "ar") "جمعة" else "Fri"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    allWeekDays.forEach { (dayId, dayTitle) ->
                        val isSel = selectedDays.contains(dayId)
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedDays = if (isSel) selectedDays - dayId else selectedDays + dayId
                            },
                            label = { Text(dayTitle, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Custom sound selection
                Text(
                    text = if (language.code == "ar") "نغمة التنبيه:" else "Alert Sound:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = soundName ?: (if (language.code == "ar") "النغمة الافتراضية للنظام" else "Default System Tone"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { audioPicker.launch("audio/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language.code == "ar") "اختيار صوت" else "Choose Audio")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (isPlayingPreview) {
                                        MediaHelper.stopAudioPreview()
                                        isPlayingPreview = false
                                    } else {
                                        isPlayingPreview = true
                                        MediaHelper.playAudioPreview(context, soundUriString) {
                                            isPlayingPreview = false
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
                            }

                            if (soundUriString != null) {
                                IconButton(onClick = {
                                    soundUriString = null
                                    soundName = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(AppStrings.cancel(language))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val alert = AlertItem(
                                id = initialAlert?.id ?: java.util.UUID.randomUUID().toString(),
                                targetPrayer = targetPrayer,
                                minutesBefore = minutesBefore,
                                daysOfWeek = if (selectedDays.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else selectedDays,
                                soundUri = soundUriString,
                                soundName = soundName,
                                isEnabled = initialAlert?.isEnabled ?: true
                            )
                            onSave(alert)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_alert_item_button")
                    ) {
                        Text(AppStrings.save(language))
                    }
                }
            }
        }
    }
}
