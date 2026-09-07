package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.data.*
import com.example.engine.PrayerType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.AdhanZipManager
import com.example.media.MediaHelper
import com.example.notifications.AdhanSequencePlayer
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AdhanTabContent(
    prefs: AppPreferences,
    language: AppLanguage,
    onPreviewVideo: (uri: String?, title: String) -> Unit,
    onPreviewAdhanScreen: (prayer: PrayerType) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { AdhanPreferencesRepository(context) }
    val updates by repo.updates.collectAsState()

    // Subtab selection for the 4 sections
    var activeSubSection by remember { mutableIntStateOf(0) }

    // Active audio preview state
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val fivePrayers = listOf(
        PrayerType.FAJR,
        PrayerType.DHUHR,
        PrayerType.ASR,
        PrayerType.MAGHRIB,
        PrayerType.ISHA
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Main Tab Title
        Text(
            text = AppStrings.tabAdhan(language),
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Main Section Tabs (Sticky at top for quick navigation)
        ScrollableTabRow(
            selectedTabIndex = activeSubSection,
            containerColor = MosqueDarkSurface,
            contentColor = IslamicGoldPrimary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            val sections = if (language.code == "ar") {
                listOf(
                    "صوت الأذان",
                    "التنبيه بالوقت",
                    "دعاء بعد الأذان",
                    "شاشة الأذان"
                )
            } else {
                listOf(
                    "Adhan Audio",
                    "Alert At Time",
                    "Dua Video",
                    "Adhan Screen"
                )
            }

            sections.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubSection == index,
                    onClick = {
                        MediaHelper.stopAudioPreview()
                        currentlyPlayingUri = null
                        activeSubSection = index
                        coroutineScope.launch {
                            scrollState.scrollTo(0)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeSubSection == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (activeSubSection == index) IslamicGoldPrimary else Color.LightGray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable content area for all 5 prayers and settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 90.dp)
            ) {
                // Privacy & Local Storage Guarantee Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3029)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E5245))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = IslamicGoldLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language.code == "ar")
                                "جميع ملفات الأذان، أصوات التنبيه، فيديوهات الدعاء، وصور الشاشة آمنة ومحفوظة محليًا على جهازك فقط."
                            else
                                "All Adhan audios, alert tones, prayer videos, and screen photos remain 100% private and stored locally on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1FAE5),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Render selected section
                when (activeSubSection) {
                    0 -> SectionAdhanAudio(
                        context = context,
                        repo = repo,
                        language = language,
                        prayers = fivePrayers,
                        currentlyPlayingUri = currentlyPlayingUri,
                        onTogglePlayAudio = { uri ->
                            if (currentlyPlayingUri == uri) {
                                MediaHelper.stopAudioPreview()
                                currentlyPlayingUri = null
                            } else {
                                currentlyPlayingUri = uri
                                MediaHelper.playAudioPreview(context, uri) {
                                    currentlyPlayingUri = null
                                }
                            }
                        }
                    )
                    1 -> SectionAdhanAlert(
                        context = context,
                        repo = repo,
                        language = language,
                        prayers = fivePrayers,
                        currentlyPlayingUri = currentlyPlayingUri,
                        onTogglePlayAudio = { uri ->
                            if (currentlyPlayingUri == uri) {
                                MediaHelper.stopAudioPreview()
                                currentlyPlayingUri = null
                            } else {
                                currentlyPlayingUri = uri
                                MediaHelper.playAudioPreview(context, uri) {
                                    currentlyPlayingUri = null
                                }
                            }
                        }
                    )
                    2 -> SectionDuaVideo(
                        context = context,
                        repo = repo,
                        language = language,
                        prayers = fivePrayers,
                        onPreviewVideo = onPreviewVideo
                    )
                    3 -> SectionAdhanScreen(
                        context = context,
                        repo = repo,
                        language = language,
                        prayers = fivePrayers,
                        onPreviewAdhanScreen = onPreviewAdhanScreen
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. SECTION: اختيار صوت الأذان
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionAdhanAudio(
    context: Context,
    repo: AdhanPreferencesRepository,
    language: AppLanguage,
    prayers: List<PrayerType>,
    currentlyPlayingUri: String?,
    onTogglePlayAudio: (String?) -> Unit
) {
    var selectedPrayerForPicker by remember { mutableStateOf<PrayerType?>(null) }

    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val p = selectedPrayerForPicker
        if (uri != null && p != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}

            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = false)
            repo.setAudioConfig(
                p,
                PrayerAudioConfig(
                    uriString = uri.toString(),
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = meta.sizeBytes,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            Toast.makeText(
                context,
                if (language.code == "ar") "تم تعيين صوت أذان ${AppStrings.getPrayerName(p, language)}" else "Adhan audio set",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (language.code == "ar") "اختيار صوت الأذان لكل صلاة" else "Select Adhan Audio for each prayer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IslamicGoldLight
        )
        Text(
            text = if (language.code == "ar")
                "يدعم صيغ MP3 وWAV وM4A وAAC وOGG مع فحص التوافق التلقائي."
            else
                "Supports MP3, WAV, M4A, AAC, and OGG formats with automatic compatibility validation.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        prayers.forEach { prayer ->
            val config = repo.getAudioConfig(prayer)
            val hasCustomAudio = !config.uriString.isNullOrBlank()
            val isAvailable = MediaHelper.isUriAvailable(context, config.uriString)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header with Prayer Name & Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🕌 ${AppStrings.getPrayerName(prayer, language)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { checked ->
                                repo.setAudioConfig(prayer, config.copy(isEnabled = checked))
                            }
                        )
                    }

                    if (hasCustomAudio) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MosqueDarkSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🎵 ${config.fileName ?: "Adhan audio"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IslamicGoldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${MediaHelper.formatDuration(config.durationMs)} • ${MediaHelper.formatFileSize(config.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA5BFB9)
                                )

                                if (!isAvailable) {
                                    Text(
                                        text = if (language.code == "ar") "⚠️ الملف غير متوفر" else "⚠️ File missing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (config.isCompatible) {
                                    Text(
                                        text = "✓ ${if (language.code == "ar") "متوافق" else "Compatible"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedEmeraldAccent
                                    )
                                } else {
                                    Text(
                                        text = "✕ ${if (language.code == "ar") "غير مدعوم" else "Unsupported"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Preview, Change, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPlaying = currentlyPlayingUri == config.uriString
                            Button(
                                onClick = { onTogglePlayAudio(config.uriString) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) Color(0xFFD97706) else Color(0xFF16473D)
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) (if (language.code == "ar") "إيقاف" else "Stop") else (if (language.code == "ar") "معاينة" else "Preview"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedPrayerForPicker = prayer
                                    audioPickerLauncher.launch("audio/*")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language.code == "ar") "تغيير" else "Change", style = MaterialTheme.typography.bodySmall)
                            }

                            IconButton(
                                onClick = {
                                    if (currentlyPlayingUri == config.uriString) {
                                        MediaHelper.stopAudioPreview()
                                    }
                                    repo.resetAudioConfig(prayer)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedPrayerForPicker = prayer
                                audioPickerLauncher.launch("audio/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language.code == "ar") "اختيار صوت أذان من الهاتف" else "Select Adhan Audio")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. SECTION: التنبيه في وقت الأذان مباشرة
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionAdhanAlert(
    context: Context,
    repo: AdhanPreferencesRepository,
    language: AppLanguage,
    prayers: List<PrayerType>,
    currentlyPlayingUri: String?,
    onTogglePlayAudio: (String?) -> Unit
) {
    var selectedPrayerForPicker by remember { mutableStateOf<PrayerType?>(null) }

    val alertPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val p = selectedPrayerForPicker
        if (uri != null && p != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}

            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = false)
            repo.setAlertConfig(
                p,
                PrayerAlertSoundConfig(
                    uriString = uri.toString(),
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = meta.sizeBytes,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            Toast.makeText(
                context,
                if (language.code == "ar") "تم تعيين تنبيه ${AppStrings.getPrayerName(p, language)}" else "Alert tone set",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (language.code == "ar") "التنبيه في وقت الأذان (قبل الأذان مباشرة)" else "Alert At Prayer Time (Immediately Before Adhan)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IslamicGoldLight
        )

        // Important Timing Rule Callout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (language.code == "ar")
                        "يبدأ صوت التنبيه عند دخول وقت الصلاة بالضبط (دون تقديم). وفور انتهاء التنبيه يبدأ الأذان مباشرة وتلقائيًا دون أي تدخل."
                    else
                        "The alert plays at the exact prayer time (no advance offset). Once the alert ends, Adhan starts immediately and automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        prayers.forEach { prayer ->
            val config = repo.getAlertConfig(prayer)
            val hasSound = !config.uriString.isNullOrBlank()
            val isAvailable = MediaHelper.isUriAvailable(context, config.uriString)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 ${AppStrings.getPrayerName(prayer, language)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { checked ->
                                repo.setAlertConfig(prayer, config.copy(isEnabled = checked))
                            }
                        )
                    }

                    if (hasSound) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MosqueDarkSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🔔 ${config.fileName ?: "Alert sound"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IslamicGoldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${MediaHelper.formatDuration(config.durationMs)} • ${MediaHelper.formatFileSize(config.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA5BFB9)
                                )

                                if (!isAvailable) {
                                    Text(
                                        text = if (language.code == "ar") "⚠️ الملف غير متوفر" else "⚠️ Missing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "✓ ${if (language.code == "ar") "متوافق" else "Ready"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedEmeraldAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPlaying = currentlyPlayingUri == config.uriString
                            Button(
                                onClick = { onTogglePlayAudio(config.uriString) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) Color(0xFFD97706) else Color(0xFF1E3A8A)
                                )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) (if (language.code == "ar") "إيقاف" else "Stop") else (if (language.code == "ar") "معاينة" else "Preview"), style = MaterialTheme.typography.bodySmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedPrayerForPicker = prayer
                                    alertPickerLauncher.launch("audio/*")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language.code == "ar") "تغيير" else "Change", style = MaterialTheme.typography.bodySmall)
                            }

                            IconButton(
                                onClick = {
                                    if (currentlyPlayingUri == config.uriString) {
                                        MediaHelper.stopAudioPreview()
                                    }
                                    repo.resetAlertConfig(prayer)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedPrayerForPicker = prayer
                                alertPickerLauncher.launch("audio/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                        ) {
                            Icon(Icons.Default.NotificationAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language.code == "ar") "اختيار صوت التنبيه من الهاتف" else "Select Alert Sound")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. SECTION: دعاء بعد الأذان (فيديو)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionDuaVideo(
    context: Context,
    repo: AdhanPreferencesRepository,
    language: AppLanguage,
    prayers: List<PrayerType>,
    onPreviewVideo: (uri: String?, title: String) -> Unit
) {
    var selectedPrayerForPicker by remember { mutableStateOf<PrayerType?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val p = selectedPrayerForPicker
        if (uri != null && p != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}

            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = true)
            repo.setDuaConfig(
                p,
                PrayerDuaVideoConfig(
                    uriString = uri.toString(),
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = meta.sizeBytes,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            Toast.makeText(
                context,
                if (language.code == "ar") "تم تعيين فيديو دعاء ${AppStrings.getPrayerName(p, language)}" else "Dua video set",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (language.code == "ar") "دعاء بعد الأذان (فيديو لكل صلاة)" else "Post-Adhan Supplication Video",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IslamicGoldLight
        )
        Text(
            text = if (language.code == "ar")
                "يتم تشغيل فيديو الدعاء تلقائيًا عند انتهاء صوت الأذان وفقًا لإمكانيات النظام الرسمية."
            else
                "The supplication video plays automatically after the Adhan finishes using official Android APIs.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        prayers.forEach { prayer ->
            val config = repo.getDuaConfig(prayer)
            val hasVideo = !config.uriString.isNullOrBlank()
            val isAvailable = MediaHelper.isUriAvailable(context, config.uriString)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬 ${AppStrings.getPrayerName(prayer, language)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { checked ->
                                repo.setDuaConfig(prayer, config.copy(isEnabled = checked))
                            }
                        )
                    }

                    if (hasVideo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MosqueDarkSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🎬 ${config.fileName ?: "Dua video"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IslamicGoldLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${MediaHelper.formatDuration(config.durationMs)} • ${MediaHelper.formatFileSize(config.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA5BFB9)
                                )

                                if (!isAvailable) {
                                    Text(
                                        text = if (language.code == "ar") "⚠️ الملف محذوف" else "⚠️ File missing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (config.isCompatible) {
                                    Text(
                                        text = "✓ ${if (language.code == "ar") "متوافق" else "Compatible"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedEmeraldAccent
                                    )
                                } else {
                                    Text(
                                        text = "✕ ${if (language.code == "ar") "غير متوافق" else "Incompatible"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onPreviewVideo(
                                        config.uriString,
                                        "${AppStrings.getPrayerName(prayer, language)} - دعاء ما بعد الأذان"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language.code == "ar") "معاينة" else "Preview", style = MaterialTheme.typography.bodySmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedPrayerForPicker = prayer
                                    videoPickerLauncher.launch("video/*")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (language.code == "ar") "تغيير" else "Change", style = MaterialTheme.typography.bodySmall)
                            }

                            IconButton(
                                onClick = { repo.resetDuaConfig(prayer) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                selectedPrayerForPicker = prayer
                                videoPickerLauncher.launch("video/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                        ) {
                            Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language.code == "ar") "اختيار فيديو من الهاتف" else "Select Video")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. SECTION: شاشة الأذان واستيراد صور ZIP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionAdhanScreen(
    context: Context,
    repo: AdhanPreferencesRepository,
    language: AppLanguage,
    prayers: List<PrayerType>,
    onPreviewAdhanScreen: (prayer: PrayerType) -> Unit
) {
    // Current selected prayer to customize screen for
    var targetPrayer by remember { mutableStateOf(PrayerType.FAJR) }
    var screenConfig by remember(targetPrayer) { mutableStateOf(repo.getScreenConfig(targetPrayer)) }
    var extractedImages by remember(targetPrayer) { mutableStateOf(AdhanZipManager.getExtractedImages(context, targetPrayer)) }

    var fullPreviewImagePath by remember { mutableStateOf<String?>(null) }
    var zipExtractionMessage by remember { mutableStateOf<String?>(null) }
    var isExtracting by remember { mutableStateOf(false) }

    // ZIP file picker with security validation
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtracting = true
            zipExtractionMessage = null

            // Extract securely using AdhanZipManager
            val result = AdhanZipManager.extractZipFile(context, uri, targetPrayer)
            isExtracting = false

            if (result.success) {
                extractedImages = AdhanZipManager.getExtractedImages(context, targetPrayer)
                val allNames = extractedImages.map { it.name }
                screenConfig = screenConfig.copy(selectedImageNames = allNames)
                repo.setScreenConfig(targetPrayer, screenConfig)

                zipExtractionMessage = if (language.code == "ar") {
                    "✓ تم استخراج ${result.extractedFiles.size} صورة متوافقة بنجاح" +
                            if (result.ignoredCount > 0) " (تم تجاهل ${result.ignoredCount} ملفات غير متوافقة بأمان)" else ""
                } else {
                    "✓ Extracted ${result.extractedFiles.size} images successfully"
                }
            } else {
                zipExtractionMessage = "⚠️ ${result.errorMessage ?: "فشل فك ضغط الملف"}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (language.code == "ar") "شاشة الأذان وخلفيات العرض" else "Adhan Screen & Backgrounds",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IslamicGoldLight
        )
        Text(
            text = if (language.code == "ar")
                "تخصيص الصور وطريقة العرض والعناصر المعروضة على شاشة الأذان لكل صلاة."
            else
                "Customize backgrounds, slideshow interval, and screen elements for each prayer.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        // Prayer selector chips
        ScrollableTabRow(
            selectedTabIndex = prayers.indexOf(targetPrayer).coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = IslamicGoldPrimary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            prayers.forEach { p ->
                Tab(
                    selected = targetPrayer == p,
                    onClick = {
                        targetPrayer = p
                        screenConfig = repo.getScreenConfig(p)
                        extractedImages = AdhanZipManager.getExtractedImages(context, p)
                        zipExtractionMessage = null
                    },
                    text = {
                        Text(
                            text = AppStrings.getPrayerName(p, language),
                            fontWeight = if (targetPrayer == p) FontWeight.Bold else FontWeight.Normal,
                            color = if (targetPrayer == p) IslamicGoldPrimary else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ZIP Importer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "استيراد صور شاشة الأذان" else "Import Screen Images",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${extractedImages.size} ${if (language.code == "ar") "صور" else "images"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IslamicGoldLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (language.code == "ar")
                        "اختر ملف ZIP من الهاتف يحتوي على صورك المفضلة (JPG, PNG, WEBP). يتم فحص الملف وحمايته محليًا ضد Zip Slip وZip Bomb."
                    else
                        "Select a ZIP file containing images. Protected against Zip Slip and Zip Bomb, stored securely locally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF164E63)),
                        enabled = !isExtracting
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (extractedImages.isEmpty())
                                (if (language.code == "ar") "اختيار ملف ZIP" else "Select ZIP")
                            else
                                (if (language.code == "ar") "استبدال ملف ZIP" else "Replace ZIP"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (extractedImages.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                AdhanZipManager.deleteImagePack(context, targetPrayer)
                                extractedImages = emptyList()
                                screenConfig = screenConfig.copy(selectedImageNames = emptyList())
                                repo.setScreenConfig(targetPrayer, screenConfig)
                                zipExtractionMessage = if (language.code == "ar") "تم حذف حزمة الصور" else "Image pack deleted"
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language.code == "ar") "حذف" else "Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (zipExtractionMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = zipExtractionMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (zipExtractionMessage!!.startsWith("✓")) LedEmeraldAccent else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Extracted images thumbnail grid
                if (extractedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language.code == "ar") "الصور المستخرجة (اضغط للاختيار أو المعاينة):" else "Extracted Images (Tap to toggle/preview):",
                        style = MaterialTheme.typography.bodySmall,
                        color = IslamicGoldLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        extractedImages.chunked(3).forEach { rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowImages.forEach { imgFile ->
                                    val isSelected = screenConfig.selectedImageNames.isEmpty() ||
                                            screenConfig.selectedImageNames.contains(imgFile.name)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) IslamicGoldPrimary else Color.DarkGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                fullPreviewImagePath = imgFile.absolutePath
                                            }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(imgFile),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Checkbox overlay for selection in slideshow
                                        IconButton(
                                            onClick = {
                                                val currentList = screenConfig.selectedImageNames.toMutableList()
                                                if (currentList.contains(imgFile.name)) {
                                                    currentList.remove(imgFile.name)
                                                } else {
                                                    currentList.add(imgFile.name)
                                                }
                                                screenConfig = screenConfig.copy(selectedImageNames = currentList)
                                                repo.setScreenConfig(targetPrayer, screenConfig)
                                            },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isSelected) LedEmeraldAccent else Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowImages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Display Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (language.code == "ar") "إعدادات عرض الصور" else "Slideshow & Display Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mode: Single vs Slideshow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = screenConfig.displayMode == AdhanScreenDisplayMode.SLIDESHOW,
                        onClick = {
                            screenConfig = screenConfig.copy(displayMode = AdhanScreenDisplayMode.SLIDESHOW)
                            repo.setScreenConfig(targetPrayer, screenConfig)
                        },
                        label = { Text(if (language.code == "ar") "عرض متتابع (Slideshow)" else "Slideshow") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = screenConfig.displayMode == AdhanScreenDisplayMode.SINGLE,
                        onClick = {
                            screenConfig = screenConfig.copy(displayMode = AdhanScreenDisplayMode.SINGLE)
                            repo.setScreenConfig(targetPrayer, screenConfig)
                        },
                        label = { Text(if (language.code == "ar") "صورة واحدة ثابتة" else "Single Image") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Slide duration selection (if Slideshow)
                AnimatedVisibility(visible = screenConfig.displayMode == AdhanScreenDisplayMode.SLIDESHOW) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = if (language.code == "ar") "مدة عرض كل صورة:" else "Slide duration:",
                            style = MaterialTheme.typography.bodySmall,
                            color = IslamicGoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(3, 5, 10, 15).forEach { seconds ->
                                FilterChip(
                                    selected = screenConfig.slideIntervalSec == seconds,
                                    onClick = {
                                        screenConfig = screenConfig.copy(slideIntervalSec = seconds)
                                        repo.setScreenConfig(targetPrayer, screenConfig)
                                    },
                                    label = { Text("$seconds ${if (language.code == "ar") "ثوانٍ" else "s"}", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Screen Elements Toggles Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (language.code == "ar") "عناصر شاشة الأذان المعروضة" else "Visible Screen Elements",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "اسم الصلاة" else "Prayer Name",
                    checked = screenConfig.showPrayerName,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(showPrayerName = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "الساعة الرقمية الكبيرة" else "Digital Clock",
                    checked = screenConfig.showClock,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(showClock = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "التاريخ الهجري" else "Hijri Date",
                    checked = screenConfig.showHijriDate,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(showHijriDate = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "التاريخ الميلادي" else "Gregorian Date",
                    checked = screenConfig.showGregorianDate,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(showGregorianDate = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Automatic Screen Launch & Synchronization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = IslamicGoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language.code == "ar") "التشغيل التلقائي والمزامنة" else "Automatic Launch & Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = if (language.code == "ar")
                        "تشغيل شاشة الأذان تلقائياً وإيقاظ الهاتف حتى لو كان مقفلاً عند المواعيد المحددة ومزامنة شريط الإشعارات والويدجت."
                    else
                        "Automatically wake device and launch screen at prayer, suhoor, or alerts, and synchronize widgets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "تشغيل الشاشة تلقائياً عند حلول الأذان" else "Auto launch on Adhan",
                    checked = screenConfig.autoOpenOnAdhan,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(autoOpenOnAdhan = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "تشغيل الشاشة تلقائياً عند موعد السحور" else "Auto launch on Suhoor",
                    checked = screenConfig.autoOpenOnSuhoor,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(autoOpenOnSuhoor = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                ScreenElementToggleRow(
                    title = if (language.code == "ar") "تشغيل الشاشة تلقائياً عند التنبيهات المسبقة" else "Auto launch on Alerts",
                    checked = screenConfig.autoOpenOnAlerts,
                    onCheckedChange = {
                        screenConfig = screenConfig.copy(autoOpenOnAlerts = it)
                        repo.setScreenConfig(targetPrayer, screenConfig)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        com.example.widgets.WidgetSyncHelper.syncAll(context)
                        Toast.makeText(
                            context,
                            if (language.code == "ar") "✓ تمت مزامنة التطبيقات المصغرة وشريط الإشعارات بنجاح" else "✓ Widgets and notification bar synced",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (language.code == "ar") "مزامنة التطبيقات المصغرة وشريط الإشعارات الآن" else "Sync Widgets & Notifications Now",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Preview Button: "معاينة شاشة الأذان الآن"
        Button(
            onClick = { onPreviewAdhanScreen(targetPrayer) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language.code == "ar")
                    "معاينة شاشة أذان ${AppStrings.getPrayerName(targetPrayer, language)} الآن"
                else
                    "Preview Adhan Screen Now",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Full-size image preview dialog if user clicked an extracted thumbnail
    if (fullPreviewImagePath != null) {
        Dialog(onDismissRequest = { fullPreviewImagePath = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(fullPreviewImagePath!!)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                IconButton(
                    onClick = { fullPreviewImagePath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ScreenElementToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
