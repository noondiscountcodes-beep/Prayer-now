package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppPreferences
import com.example.data.RamadanMode
import com.example.engine.*
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MosqueClockScreen(
    prefs: AppPreferences,
    onOpenMenu: () -> Unit,
    pendingVideoPrayer: String? = null,
    pendingMusaharati: Boolean = false,
    onVideoHandled: () -> Unit = {}
) {
    val language by prefs.languageFlow.collectAsState()
    val refreshTrigger by prefs.refreshTrigger.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    // 1-second live clock tick
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timezone = remember(refreshTrigger) { prefs.getTimezone() }
    val calendar = remember(currentTimeMillis, timezone) {
        Calendar.getInstance(timezone).apply { timeInMillis = currentTimeMillis }
    }

    // Prayer Schedule for today
    val schedule = remember(calendar.get(Calendar.DAY_OF_YEAR), refreshTrigger) {
        PrayerTimesCalculator.calculate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = timezone,
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle()
        )
    }

    val currentPrayerType = remember(currentTimeMillis, schedule) {
        schedule.getCurrentPrayer(currentTimeMillis)
    }
    val nextPrayerEntry = remember(currentTimeMillis, schedule) {
        schedule.getNextPrayer(currentTimeMillis)
    }
    val remainingSeconds = remember(currentTimeMillis, schedule) {
        schedule.getRemainingSecondsToNext(currentTimeMillis)
    }
    val rawRemaining = remember(remainingSeconds) {
        PrayerDaySchedule.formatRemaining(remainingSeconds)
    }
    val formattedRemaining = remember(rawRemaining, isArabic) {
        formatLocalizedDigits(rawRemaining, isArabic)
    }

    // Clock time format (e.g., 02:45:18 or 02:45)
    val timeFormat = remember(language) {
        SimpleDateFormat("hh:mm", Locale.US).apply { timeZone = timezone }
    }
    val rawClockTime = remember(currentTimeMillis) { timeFormat.format(calendar.time) }
    val clockTimeString = remember(rawClockTime, isArabic) {
        formatLocalizedDigits(rawClockTime, isArabic)
    }

    val amPmFormat = remember {
        SimpleDateFormat("a", Locale.US).apply { timeZone = timezone }
    }
    val amPmString = remember(currentTimeMillis) { amPmFormat.format(calendar.time) }

    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val dayName = remember(dayOfWeek, language) {
        HijriCalendarHelper.getDayOfWeekName(dayOfWeek, language.code)
    }

    val hijriDate = remember(calendar.get(Calendar.DAY_OF_YEAR), refreshTrigger) {
        HijriCalendarHelper.fromGregorian(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    val hijriString = remember(hijriDate, language) {
        val base = hijriDate.format(language.code)
        formatLocalizedDigits(base, isArabic)
    }

    val gregorianFormat = remember(language) {
        SimpleDateFormat("dd MMMM yyyy", when (language) {
            AppLanguage.ARABIC -> Locale("ar")
            AppLanguage.FRENCH -> Locale.FRENCH
            else -> Locale.ENGLISH
        }).apply { timeZone = timezone }
    }
    val rawGregorian = remember(calendar.time, gregorianFormat) {
        gregorianFormat.format(calendar.time)
    }
    val gregorianString = remember(rawGregorian, isArabic) {
        formatLocalizedDigits(rawGregorian, isArabic)
    }

    // Ramadan Banner calculation
    val isRamadanActive = when (prefs.getRamadanMode()) {
        RamadanMode.AUTO -> hijriDate.isRamadan
        RamadanMode.MANUAL_ON -> true
        RamadanMode.MANUAL_OFF -> false
    }

    // Video Dialog state
    var activeVideoUri by remember { mutableStateOf<String?>(null) }
    var activeVideoTitle by remember { mutableStateOf("") }
    var showVideoDialog by remember { mutableStateOf(false) }

    // Auto-launch video dialog if pending trigger arrives from notification
    LaunchedEffect(pendingVideoPrayer) {
        if (pendingVideoPrayer != null) {
            val pt = try { PrayerType.valueOf(pendingVideoPrayer) } catch (e: Exception) { null }
            if (pt != null) {
                val cfg = prefs.getAdhanConfig(pt)
                activeVideoUri = cfg.uriString
                activeVideoTitle = "${AppStrings.tabAdhan(language)}: ${AppStrings.getPrayerName(pt, language)}"
                showVideoDialog = true
                onVideoHandled()
            }
        }
    }
    LaunchedEffect(pendingMusaharati) {
        if (pendingMusaharati) {
            val cfg = prefs.getMusaharatiConfig()
            activeVideoUri = cfg.uriString
            activeVideoTitle = if (language.code == "ar") "فيديو المسحراتي" else "Musaharati Video"
            showVideoDialog = true
            onVideoHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishCanvasBg)
            .testTag("mosque_clock_screen")
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─────────────────────────────────────────────────────────────
            // 1. TOP HEADER: Professional Emerald (#064E3B) with rounded-b-3xl
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(ProfessionalEmerald)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date & Calendar Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hijriString,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = " | ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = gregorianString,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Main Huge Monospaced Digital Clock
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = clockTimeString,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = (-2).sp,
                            lineHeight = 64.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = amPmString,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PolishGold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    // Floating Pill with Next Prayer & Countdown Ticker
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Next Prayer
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AppStrings.nextPrayer(language),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = AppStrings.getPrayerName(nextPrayerEntry.type, language),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PolishGold
                                )
                            }

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )

                            // Countdown Ticker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = AppStrings.remaining(language),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = formattedRemaining,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Ramadan notice (if active)
                    AnimatedVisibility(visible = isRamadanActive) {
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌙 ",
                                fontSize = 12.sp
                            )
                            val ramadanDiff = if (currentTimeMillis < schedule.maghrib.timestampMillis) {
                                val diff = (schedule.maghrib.timestampMillis - currentTimeMillis) / 1000
                                "${if (language.code == "ar") "للإفطار" else "Iftar"}: ${formatLocalizedDigits(PrayerDaySchedule.formatRemaining(diff), isArabic)}"
                            } else {
                                val diff = (schedule.fajr.timestampMillis + 24 * 3600 * 1000L - currentTimeMillis) / 1000
                                "${if (language.code == "ar") "للسحور" else "Suhoor"}: ${formatLocalizedDigits(PrayerDaySchedule.formatRemaining(diff), isArabic)}"
                            }
                            Text(
                                text = ramadanDiff,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PolishGoldLight
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 2. PRAYER TIMES LIST (Professional Polish Card Grid)
            // ─────────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                schedule.allEntries.forEach { entry ->
                    val isNext = entry.type == nextPrayerEntry.type
                    val prayerIcon = getPrayerIcon(entry.type)
                    val rawEntryTime = entry.formattedTime24
                    val localizedTime = remember(rawEntryTime, isArabic) {
                        formatLocalizedDigits(rawEntryTime, isArabic)
                    }

                    if (isNext) {
                        // Highlighted Active Prayer Card (Emerald tint, 2.dp border, side accent)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF064E3B).copy(alpha = 0.06f)
                            ),
                            border = BorderStroke(
                                2.dp,
                                Color(0xFF064E3B).copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Solid Emerald Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ProfessionalEmerald),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = prayerIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = AppStrings.getPrayerName(entry.type, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfessionalEmerald
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = localizedTime,
                                        fontSize = 20.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfessionalEmerald
                                    )
                                    Text(
                                        text = AppStrings.nextPrayer(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ProfessionalEmerald
                                    )
                                }
                            }
                        }
                    } else {
                        // Standard Clean White Card (shadow-sm, border-slate-100)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishSurface),
                            border = BorderStroke(1.dp, PolishBorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Neutral Icon Box (slate-50, text-slate-500)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PolishIconBox),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = prayerIcon,
                                            contentDescription = null,
                                            tint = PolishTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Text(
                                        text = AppStrings.getPrayerName(entry.type, language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PolishTextPrimary
                                    )
                                }

                                Text(
                                    text = localizedTime,
                                    fontSize = 17.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = PolishTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. BOTTOM BAR: Crisp white container, Slate-900 Menu button, Metadata
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PolishSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, PolishBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Slate-900 Menu Button
                Button(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("menu_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PolishSlateButton,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.menu(language),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Metadata row (Location pin + Verified shield)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PolishTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${prefs.getCityName()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                    }

                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = PolishTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PolishTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (language.code == "ar") "مشفر بالكامل ومحلي" else "100% Private & Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PolishTextSecondary
                        )
                    }
                }
            }
        }

        // Active Video Dialog
        if (showVideoDialog) {
            VideoPlayerDialog(
                videoUriString = activeVideoUri,
                videoTitle = activeVideoTitle,
                language = language,
                onDismiss = { showVideoDialog = false }
            )
        }
    }
}

private fun getPrayerIcon(type: PrayerType) = when (type) {
    PrayerType.FAJR -> Icons.Default.Brightness4
    PrayerType.SUNRISE -> Icons.Default.WbSunny
    PrayerType.DHUHR -> Icons.Default.WbSunny
    PrayerType.ASR -> Icons.Default.Brightness5
    PrayerType.MAGHRIB -> Icons.Default.WbTwilight
    PrayerType.ISHA -> Icons.Default.NightsStay
}

private fun formatLocalizedDigits(input: String, isArabic: Boolean): String {
    if (!isArabic) return input
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (ch in input) {
        if (ch in '0'..'9') {
            sb.append(arabicDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}
