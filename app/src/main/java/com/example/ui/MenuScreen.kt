package com.example.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.engine.*
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.MediaHelper
import com.example.notifications.AlarmScheduler
import com.example.notifications.PrayerNotificationHelper
import com.example.widgets.WidgetSyncHelper
import com.example.notifications.PrayerNotificationService
import com.example.ui.components.*
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import java.util.*

enum class MenuTab {
    LANGUAGE,
    LOCATION_METHOD,
    ALERTS,
    ADHAN,
    RAMADAN,
    NOTIFICATION_BAR,
    SETTINGS_SECURITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    prefs: AppPreferences,
    initialTab: MenuTab = MenuTab.LANGUAGE,
    onBackToClock: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    val language by prefs.languageFlow.collectAsState()
    val refreshTrigger by prefs.refreshTrigger.collectAsState()

    // Video preview dialog state
    var previewVideoUri by remember { mutableStateOf<String?>(null) }
    var previewVideoTitle by remember { mutableStateOf("") }
    var showVideoPreviewDialog by remember { mutableStateOf(false) }

    // Adhan & Alert full-screen preview state
    var showAdhanScreenPreview by remember { mutableStateOf(false) }
    var previewAdhanPrayer by remember { mutableStateOf(PrayerType.FAJR) }
    var previewAdhanTriggerType by remember { mutableStateOf("ADHAN") }
    var previewAdhanAlertMinutes by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.menu(language),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToClock,
                        modifier = Modifier.testTag("back_to_clock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfessionalEmerald
                )
            )
        },
        containerColor = PolishCanvasBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Row with 7 tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = ProfessionalEmeraldDark,
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                MenuTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            val title = when (tab) {
                                MenuTab.LANGUAGE -> AppStrings.tabLanguage(language)
                                MenuTab.LOCATION_METHOD -> AppStrings.tabLocation(language)
                                MenuTab.ALERTS -> AppStrings.tabAlerts(language)
                                MenuTab.ADHAN -> AppStrings.tabAdhan(language)
                                MenuTab.RAMADAN -> AppStrings.tabRamadan(language)
                                MenuTab.NOTIFICATION_BAR -> AppStrings.tabNotifications(language)
                                MenuTab.SETTINGS_SECURITY -> AppStrings.tabSettings(language)
                            }
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    MenuTab.LANGUAGE -> LanguageTab(prefs, language)
                    MenuTab.LOCATION_METHOD -> LocationMethodTab(prefs, language)
                    MenuTab.ALERTS -> AlertsTab(prefs, language)
                    MenuTab.ADHAN -> AdhanTab(
                        prefs = prefs,
                        language = language,
                        onPreviewVideo = { uri, title ->
                            previewVideoUri = uri
                            previewVideoTitle = title
                            showVideoPreviewDialog = true
                        },
                        onPreviewAdhanScreen = { prayer ->
                            previewAdhanPrayer = prayer
                            previewAdhanTriggerType = "ADHAN"
                            previewAdhanAlertMinutes = null
                            showAdhanScreenPreview = true
                        },
                        onPreviewAlertScreen = { prayer, minutes ->
                            previewAdhanPrayer = prayer
                            previewAdhanTriggerType = "ALERT"
                            previewAdhanAlertMinutes = minutes
                            showAdhanScreenPreview = true
                        }
                    )
                    MenuTab.RAMADAN -> RamadanTab(
                        prefs = prefs,
                        language = language,
                        onPreviewVideo = { uri, title ->
                            previewVideoUri = uri
                            previewVideoTitle = title
                            showVideoPreviewDialog = true
                        }
                    )
                    MenuTab.NOTIFICATION_BAR -> NotificationBarTab(prefs, language)
                    MenuTab.SETTINGS_SECURITY -> SettingsSecurityTab(prefs, language)
                }
            }
        }
    }

    if (showVideoPreviewDialog) {
        VideoPlayerDialog(
            videoUriString = previewVideoUri,
            videoTitle = previewVideoTitle,
            language = language,
            onDismiss = { showVideoPreviewDialog = false }
        )
    }

    if (showAdhanScreenPreview) {
        AdhanFullScreenView(
            prayer = previewAdhanPrayer,
            prefs = prefs,
            triggerType = previewAdhanTriggerType,
            alertMinutes = previewAdhanAlertMinutes,
            onDismiss = { showAdhanScreenPreview = false },
            onOpenDuaVideo = { uri ->
                showAdhanScreenPreview = false
                previewVideoUri = uri
                previewVideoTitle = "دعاء ما بعد الأذان"
                showVideoPreviewDialog = true
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 1. LANGUAGE TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun LanguageTab(prefs: AppPreferences, language: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (language.code == "ar") "اختر لغة التطبيق" else "Choose Application Language",
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (language.code == "ar")
                "يتم تطبيق اللغة فورًا وتغيير اتجاه الواجهة (RTL للعربية و LTR للإنجليزية والفرنسية) دون الحاجة لإعادة التشغيل."
            else
                "Language and layout direction (RTL for Arabic, LTR for English/French) update instantly without restarting.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFA5BFB9),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppLanguage.values().forEach { lang ->
            val isSelected = language == lang
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        prefs.setLanguage(lang)
                        AlarmScheduler.scheduleAll(prefs.run { prefs.toString(); com.example.localization.AppStrings.hashCode(); null } ?: return@clickable)
                    }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) IslamicGoldPrimary else MosqueCardBorder,
                        shape = RoundedCornerShape(14.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF133C33) else MosqueDarkSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = lang.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) IslamicGoldLight else Color.White
                        )
                        Text(
                            text = if (lang.isRtl) "RTL (من اليمين إلى اليسار)" else "LTR (Left-to-Right)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF88A8A0)
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { prefs.setLanguage(lang) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 2. LOCATION & METHOD TAB
// ─────────────────────────────────────────────────────────────
@Composable
private fun LocationMethodTab(prefs: AppPreferences, language: AppLanguage) {
    val context = LocalContext.current
    var showCitySearch by remember { mutableStateOf(false) }
    var showCustomAngles by remember { mutableStateOf(false) }
    var isLocatingGps by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // GPS permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            isLocatingGps = true
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    isLocatingGps = false
                    if (loc != null) {
                        val lat = loc.latitude
                        val lng = loc.longitude
                        var cityName = "GPS Location"
                        var countryName = ""
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            if (!addresses.isNullOrEmpty()) {
                                cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: "Current Location"
                                countryName = addresses[0].countryName ?: ""
                            }
                        } catch (e: Exception) {
                            // Offline geocoding fallback
                        }
                        prefs.setLocation(
                            cityName = cityName,
                            countryName = countryName,
                            lat = lat,
                            lng = lng,
                            timezoneId = TimeZone.getDefault().id,
                            isGps = true
                        )
                        AlarmScheduler.scheduleAll(context)
                        PrayerNotificationService.start(context)
                        Toast.makeText(context, if (language.code == "ar") "تم تحديث الموقع بنجاح" else "Location updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (language.code == "ar") "تعذر جلب موقع GPS، يرجى تفعيل الموقع" else "Could not fetch GPS location", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    isLocatingGps = false
                    Toast.makeText(context, "GPS error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                isLocatingGps = false
            }
        } else {
            Toast.makeText(context, if (language.code == "ar") "تم رفض إذن الموقع، يمكنك التحديد اليدوي" else "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Current Location Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language.code == "ar") "الموقع الحالي المعتمد" else "Active Location",
                    style = MaterialTheme.typography.labelLarge,
                    color = IslamicGoldPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${prefs.getCityName()}, ${prefs.getCountryName()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${if (language.code == "ar") "الإحداثيات" else "Coords"}: ${prefs.getLatitude()}, ${prefs.getLongitude()} • ${if (prefs.isGpsLocation()) "GPS" else "Manual"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA4C0B9)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Selection Buttons (Manual vs GPS)
        Text(
            text = if (language.code == "ar") "خيارات تحديد الموقع" else "Location Mode",
            style = MaterialTheme.typography.titleMedium,
            color = IslamicGoldLight,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Manual selection
            Button(
                onClick = { showCitySearch = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_city_select_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
            ) {
                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (language.code == "ar") "اختيار يدوي" else "Manual City")
            }

            // GPS selection
            Button(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
                        isLocatingGps = true
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            isLocatingGps = false
                            if (loc != null) {
                                prefs.setLocation("GPS", "", loc.latitude, loc.longitude, TimeZone.getDefault().id, true)
                                AlarmScheduler.scheduleAll(context)
                                PrayerNotificationService.start(context)
                            }
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("gps_location_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary, contentColor = Color.Black)
            ) {
                if (isLocatingGps) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language.code == "ar") "عبر GPS" else "Via GPS")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timezone & DST
        Text(
            text = if (language.code == "ar") "المنطقة الزمنية والتوقيت الصيفي" else "Timezone & DST",
            style = MaterialTheme.typography.titleMedium,
            color = IslamicGoldLight,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (language.code == "ar") "تحديد المنطقة الزمنية تلقائيًا" else "Auto Timezone", color = Color.White)
                        Text(prefs.getTimezone().id, style = MaterialTheme.typography.bodySmall, color = Color(0xFF88A8A0))
                    }
                    Switch(
                        checked = prefs.isTimezoneAuto(),
                        onCheckedChange = { prefs.setTimezoneAuto(it) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MosqueCardBorder)

                Text(if (language.code == "ar") "التوقيت الصيفي (DST)" else "Daylight Saving Time", color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DstSetting.values().forEach { dst ->
                        FilterChip(
                            selected = prefs.getDstSetting() == dst,
                            onClick = { prefs.setDstSetting(dst) },
                            label = {
                                Text(when (dst) {
                                    DstSetting.AUTO -> if (language.code == "ar") "تلقائي" else "Auto"
                                    DstSetting.ON -> if (language.code == "ar") "تشغيل (+1)" else "On (+1)"
                                    DstSetting.OFF -> if (language.code == "ar") "إيقاف" else "Off"
                                })
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculation Method Selection
        Text(
            text = if (language.code == "ar") "طريقة حساب مواقيت الصلاة" else "Calculation Method",
            style = MaterialTheme.typography.titleMedium,
            color = IslamicGoldLight,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        CalculationMethod.values().forEach { method ->
            val isSelected = prefs.getCalculationMethod() == method
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        prefs.setCalculationMethod(method)
                        if (method == CalculationMethod.CUSTOM) {
                            showCustomAngles = true
                        }
                    }
                    .border(
                        1.dp,
                        if (isSelected) IslamicGoldPrimary else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF133C33) else MosqueDarkSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method.getDisplayName(language.code),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) IslamicGoldLight else Color.White
                        )
                        if (method == CalculationMethod.CUSTOM) {
                            Text(
                                text = "${if (language.code == "ar") "الفجر" else "Fajr"}: ${prefs.getCustomFajrAngle()}° • ${if (language.code == "ar") "العشاء" else "Isha"}: ${prefs.getCustomIshaAngle()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = IslamicGoldPrimary
                            )
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            prefs.setCalculationMethod(method)
                            if (method == CalculationMethod.CUSTOM) showCustomAngles = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Madhab Selection for Asr
        Text(
            text = if (language.code == "ar") "المذهب الفقهي لحساب صلاة العصر" else "Juristic Method for Asr",
            style = MaterialTheme.typography.titleMedium,
            color = IslamicGoldLight,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Madhab.values().forEach { madhab ->
            val isSelected = prefs.getMadhab() == madhab
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { prefs.setMadhab(madhab) }
                    .border(1.dp, if (isSelected) IslamicGoldPrimary else Color.Transparent, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF133C33) else MosqueDarkSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = madhab.getDisplayName(language.code),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) IslamicGoldLight else Color.White
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { prefs.setMadhab(madhab) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manual Prayer Time Adjustments (Offsets)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldSecondary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language.code == "ar") "⏱️ تعديل أوقات الصلاة يدوياً" else "⏱️ Manual Prayer Time Adjustments",
                            style = MaterialTheme.typography.titleMedium,
                            color = IslamicGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language.code == "ar")
                                "تقديم أو تأخير التوقيت بالدقائق للتوافق مع أذان المسجد المحلي"
                            else
                                "Adjust minutes (+/-) to match your local mosque exact times",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5BFB9)
                        )
                    }

                    TextButton(
                        onClick = {
                            prefs.resetPrayerOffsets()
                            AlarmScheduler.scheduleAll(context)
                            WidgetSyncHelper.syncAll(context)
                            PrayerNotificationService.start(context)
                            Toast.makeText(context, if (language.code == "ar") "تمت إعادة ضبط التعديلات للافتراضي" else "Offsets reset to default", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = if (language.code == "ar") "إعادة ضبط" else "Reset",
                            color = IslamicGoldPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculate current times for preview
                val todaySchedule = remember(prefs.refreshTrigger.collectAsState().value) {
                    val cal = Calendar.getInstance(prefs.getTimezone())
                    PrayerTimesCalculator.calculateWithPreferences(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH),
                        prefs
                    )
                }

                val allPrayers = listOf(
                    PrayerType.FAJR to todaySchedule.fajr,
                    PrayerType.SUNRISE to todaySchedule.sunrise,
                    PrayerType.DHUHR to todaySchedule.dhuhr,
                    PrayerType.ASR to todaySchedule.asr,
                    PrayerType.MAGHRIB to todaySchedule.maghrib,
                    PrayerType.ISHA to todaySchedule.isha
                )

                allPrayers.forEach { (type, entry) ->
                    val offset = prefs.getPrayerOffset(type)
                    val prayerName = AppStrings.getPrayerName(type, language)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (offset != 0) Color(0xFF133C33) else Color(0xFF0F1E1B),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = prayerName,
                                fontWeight = FontWeight.SemiBold,
                                color = if (offset != 0) IslamicGoldLight else Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val timeFormatted = if (language.code == "ar") {
                                PrayerBannerHelper.toArabicDigits(entry.formattedTime12)
                            } else {
                                entry.formattedTime12
                            }
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = IslamicGoldPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Minus button
                            IconButton(
                                onClick = {
                                    prefs.setPrayerOffset(type, offset - 1)
                                    AlarmScheduler.scheduleAll(context)
                                    WidgetSyncHelper.syncAll(context)
                                    PrayerNotificationService.start(context)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF1E3A34), CircleShape)
                            ) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }

                            // Offset badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (offset != 0) IslamicGoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier.widthIn(min = 55.dp)
                            ) {
                                val offsetText = when {
                                    offset > 0 -> "+$offset ${if (language.code == "ar") "د" else "m"}"
                                    offset < 0 -> "$offset ${if (language.code == "ar") "د" else "m"}"
                                    else -> if (language.code == "ar") "0 د" else "0 m"
                                }
                                val offsetDigits = if (language.code == "ar") PrayerBannerHelper.toArabicDigits(offsetText) else offsetText
                                Text(
                                    text = offsetDigits,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (offset != 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (offset != 0) IslamicGoldLight else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Plus button
                            IconButton(
                                onClick = {
                                    prefs.setPrayerOffset(type, offset + 1)
                                    AlarmScheduler.scheduleAll(context)
                                    WidgetSyncHelper.syncAll(context)
                                    PrayerNotificationService.start(context)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(IslamicGoldPrimary, CircleShape)
                            ) {
                                Text("+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showCitySearch) {
        CitySearchDialog(
            language = language,
            onCitySelected = { city ->
                prefs.setLocation(
                    cityName = city.getCityOnly(language),
                    countryName = if (language.code == "ar") city.countryAr else city.countryEn,
                    lat = city.latitude,
                    lng = city.longitude,
                    timezoneId = city.timezoneId,
                    isGps = false
                )
                AlarmScheduler.scheduleAll(context)
                PrayerNotificationService.start(context)
            },
            onDismiss = { showCitySearch = false }
        )
    }

    if (showCustomAngles) {
        CustomAngleDialog(
            initialFajrAngle = prefs.getCustomFajrAngle(),
            initialIshaAngle = prefs.getCustomIshaAngle(),
            language = language,
            onSave = { f, i ->
                prefs.setCustomFajrAngle(f)
                prefs.setCustomIshaAngle(i)
                AlarmScheduler.scheduleAll(context)
                PrayerNotificationService.start(context)
            },
            onDismiss = { showCustomAngles = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 3. ALERTS TAB (UNLIMITED CUSTOM ALERTS)
// ─────────────────────────────────────────────────────────────
@Composable
private fun AlertsTab(prefs: AppPreferences, language: AppLanguage) {
    val context = LocalContext.current
    val alertsRepo = remember { AlertsRepository(context) }
    var alertsList by remember { mutableStateOf(alertsRepo.getAllAlerts()) }
    var editingAlert by remember { mutableStateOf<AlertItem?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = AppStrings.tabAlerts(language),
                    style = MaterialTheme.typography.titleLarge,
                    color = IslamicGoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language.code == "ar") "تنبيهات مخصصة قبل الأذان بأصواتك المفضلة" else "Custom prayer reminders before Adhan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA5BFB9)
                )
            }

            Button(
                onClick = {
                    editingAlert = null
                    showAddEditDialog = true
                },
                modifier = Modifier.testTag("add_alert_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(AppStrings.add(language))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Reliability / Exact Alarm Status Card
        val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
        val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
        val canExactAlarms = remember(alertsList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
        }
        val isIgnoringBattery = remember(alertsList) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        val canDrawOverlays = remember(alertsList) {
            Settings.canDrawOverlays(context)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!canExactAlarms || !isIgnoringBattery || !canDrawOverlays) Color(0xFFF59E0B) else MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "⚡ دقة انطلاق التنبيهات والأذان" else "⚡ Alarm Precision & Reliability",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGoldLight
                    )
                    IconButton(
                        onClick = {
                            AlarmScheduler.scheduleAll(context)
                            WidgetSyncHelper.syncAll(context)
                            Toast.makeText(context, if (language.code == "ar") "تم فحص وإعادة جدولة كافة التنبيهات والأذان بنجاح" else "All alarms rescheduled successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = IslamicGoldPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exact Alarms Chip / Button
                    if (canExactAlarms) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = if (language.code == "ar") "✓ منبهات دقيقة" else "✓ Exact Alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (language.code == "ar") "⚠️ تفعيل المنبهات" else "⚠️ Exact Alarms",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Battery Optimization Chip / Button
                    if (isIgnoringBattery) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = if (language.code == "ar") "✓ بدون قيود بطارية" else "✓ Unrestricted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                    } catch (e2: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (language.code == "ar") "استثناء البطارية" else "Exempt Battery",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Overlay Permission Chip / Button (إذن الظهور فوق التطبيقات الأخرى)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDrawOverlays) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF065F46).copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Text(
                                text = if (language.code == "ar") "✓ إذن الظهور فوق التطبيقات مفعّل" else "✓ Overlay Permission Granted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (language.code == "ar") "⚠️ منح إذن الظهور فوق التطبيقات الأخرى" else "⚠️ Grant Overlay Permission",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (alertsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (language.code == "ar") "لا توجد تنبيهات مضافة، اضغط إضافة لإنشاء تنبيه" else "No alerts configured. Tap Add to create one.",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alertsList, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alert.getPrayerDisplayName(language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IslamicGoldLight
                                    )
                                    Text(
                                        text = "${if (language.code == "ar") "قبل الصلاة بـ" else "Before:"} ${alert.minutesBefore} ${if (language.code == "ar") "دقيقة" else "min"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = alert.getDaysSummary(language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA5BFB9)
                                    )
                                    if (alert.soundName != null) {
                                        Text(
                                            text = "🔊 ${alert.soundName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LedCyanGlow
                                        )
                                    }
                                }

                                Switch(
                                    checked = alert.isEnabled,
                                    onCheckedChange = { checked ->
                                        alertsList = alertsRepo.toggleAlert(alert.id, checked)
                                        AlarmScheduler.scheduleAll(context)
                                    }
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MosqueCardBorder)

                            // Action buttons: Edit, Copy, Delete, Play Preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (alert.soundUri != null) {
                                    IconButton(
                                        onClick = { MediaHelper.playAudioPreview(context, alert.soundUri) }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = IslamicGoldPrimary)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingAlert = alert
                                        showAddEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                                }

                                IconButton(
                                    onClick = {
                                        alertsList = alertsRepo.duplicateAlert(alert.id)
                                        AlarmScheduler.scheduleAll(context)
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Duplicate", tint = Color.White)
                                }

                                IconButton(
                                    onClick = {
                                        alertsList = alertsRepo.deleteAlert(alert.id)
                                        AlarmScheduler.scheduleAll(context)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditAlertDialog(
            initialAlert = editingAlert,
            language = language,
            onSave = { updatedAlert ->
                alertsList = if (editingAlert == null) {
                    alertsRepo.addAlert(updatedAlert)
                } else {
                    alertsRepo.updateAlert(updatedAlert)
                }
                AlarmScheduler.scheduleAll(context)
                showAddEditDialog = false
            },
            onDismiss = { showAddEditDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 4. ADHAN TAB (4 MAIN SECTIONS: AUDIO, ALERT, DUA, SCREEN)
// ─────────────────────────────────────────────────────────────
@Composable
private fun AdhanTab(
    prefs: AppPreferences,
    language: AppLanguage,
    onPreviewVideo: (uri: String?, title: String) -> Unit,
    onPreviewAdhanScreen: (prayer: PrayerType) -> Unit,
    onPreviewAlertScreen: (prayer: PrayerType, minutes: Int) -> Unit
) {
    AdhanTabContent(
        prefs = prefs,
        language = language,
        onPreviewVideo = onPreviewVideo,
        onPreviewAdhanScreen = onPreviewAdhanScreen,
        onPreviewAlertScreen = onPreviewAlertScreen
    )
}

// ─────────────────────────────────────────────────────────────
// 5. RAMADAN TAB (HIJRI ADJUSTMENT, IFTAR CANNON & MUSAHARATI)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RamadanTab(
    prefs: AppPreferences,
    language: AppLanguage,
    onPreviewVideo: (uri: String?, title: String) -> Unit
) {
    val context = LocalContext.current
    val musaharatiConfig = prefs.getMusaharatiConfig()
    val iftarCannonConfig = prefs.getIftarCannonConfig()
    val adhanRepo = remember { com.example.data.AdhanPreferencesRepository(context) }
    val maghribScreenConfig = adhanRepo.getScreenConfig(PrayerType.MAGHRIB)
    var hijriAdjustment by remember { mutableStateOf(prefs.getHijriAdjustment()) }

    // Video picker for Musaharati
    val musaharatiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = true)
            val internalFile = MediaHelper.copyMediaToInternal(
                context = context,
                sourceUri = uri,
                folderName = "custom_video",
                targetFileNameWithoutExt = "musaharati_video"
            )
            val finalUri = if (internalFile != null) Uri.fromFile(internalFile).toString() else uri.toString()
            val finalSize = internalFile?.length() ?: meta.sizeBytes
            prefs.setMusaharatiConfig(
                AdhanVideoConfig(
                    uriString = finalUri,
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = finalSize,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            AlarmScheduler.scheduleAll(context)
        }
    }

    // Video picker for Iftar Cannon
    val iftarCannonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = true)
            val internalFile = MediaHelper.copyMediaToInternal(
                context = context,
                sourceUri = uri,
                folderName = "custom_video",
                targetFileNameWithoutExt = "iftar_cannon_media"
            )
            val finalUri = if (internalFile != null) Uri.fromFile(internalFile).toString() else uri.toString()
            val finalSize = internalFile?.length() ?: meta.sizeBytes
            prefs.setIftarCannonConfig(
                AdhanVideoConfig(
                    uriString = finalUri,
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = finalSize,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            AlarmScheduler.scheduleAll(context)
        }
    }

    val cal = Calendar.getInstance(prefs.getTimezone())
    val currentHijri = remember(cal.get(Calendar.DAY_OF_YEAR), hijriAdjustment) {
        HijriCalendarHelper.fromGregorian(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            hijriAdjustment
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (language.code == "ar") "🌙 إعدادات رمضان والتقويم الهجري" else "🌙 Ramadan & Hijri Calendar Settings",
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ══════════════════════════════════════════════════════════
        // 1. HIJRI DATE ADJUSTMENT
        // ══════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language.code == "ar") "📅 تعديل التاريخ الهجري" else "📅 Hijri Date Adjustment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGoldLight
                        )
                        Text(
                            text = if (language.code == "ar")
                                "لتوافق التقويم مع الرؤية الشرعية للهلال في بلدك (± أيام)"
                            else
                                "Adjust calendar by days according to local moon sighting",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5BFB9)
                        )
                    }

                    // Adjustment indicator badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hijriAdjustment == 0) Color(0xFF1E293B) else Color(0xFF0F766E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight)
                    ) {
                        Text(
                            text = if (hijriAdjustment > 0) "+$hijriAdjustment" else "$hijriAdjustment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current adjusted date preview box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🌙 ${currentHijri.format(language.code)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = IslamicGoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick preset chips: -2, -1, 0, +1, +2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2, -1, 0, 1, 2).forEach { offset ->
                        FilterChip(
                            selected = hijriAdjustment == offset,
                            onClick = {
                                hijriAdjustment = offset
                                prefs.setHijriAdjustment(offset)
                                AlarmScheduler.scheduleAll(context)
                                WidgetSyncHelper.syncAll(context)
                            },
                            label = {
                                Text(
                                    when (offset) {
                                        0 -> if (language.code == "ar") "0 (تلقائي)" else "0 (Auto)"
                                        else -> if (offset > 0) "+$offset" else "$offset"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fine-tuning Stepper: Minus 1 day / Plus 1 day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val next = (hijriAdjustment - 1).coerceAtLeast(-5)
                            hijriAdjustment = next
                            prefs.setHijriAdjustment(next)
                            AlarmScheduler.scheduleAll(context)
                            WidgetSyncHelper.syncAll(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language.code == "ar") "➖ تقديم يوم (-1)" else "➖ -1 Day")
                    }

                    OutlinedButton(
                        onClick = {
                            val next = (hijriAdjustment + 1).coerceAtMost(5)
                            hijriAdjustment = next
                            prefs.setHijriAdjustment(next)
                            AlarmScheduler.scheduleAll(context)
                            WidgetSyncHelper.syncAll(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language.code == "ar") "➕ تأخير يوم (+1)" else "➕ +1 Day")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ══════════════════════════════════════════════════════════
        // 2. RAMADAN MODE
        // ══════════════════════════════════════════════════════════
        Text(
            text = if (language.code == "ar") "وضع شهر رمضان:" else "Ramadan Mode:",
            style = MaterialTheme.typography.labelLarge,
            color = IslamicGoldLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RamadanMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.getRamadanMode() == mode,
                    onClick = {
                        prefs.setRamadanMode(mode)
                        AlarmScheduler.scheduleAll(context)
                        WidgetSyncHelper.syncAll(context)
                    },
                    label = {
                        Text(when (mode) {
                            RamadanMode.AUTO -> if (language.code == "ar") "تلقائي (بحسب التاريخ)" else "Auto (By Date)"
                            RamadanMode.MANUAL_ON -> if (language.code == "ar") "مفعّل دائمًا" else "Always On"
                            RamadanMode.MANUAL_OFF -> if (language.code == "ar") "معطّل" else "Disabled"
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ══════════════════════════════════════════════════════════
        // 3. IFTAR CANNON (مدفع الإفطار في رمضان)
        // ══════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language.code == "ar") "💥 مدفع الإفطار في رمضان" else "💥 Ramadan Iftar Cannon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDA4AF)
                        )
                        Text(
                            text = if (language.code == "ar")
                                "يعمل قبل أذان المغرب مباشرة مع إطلاق الشاشة والفيديو تلقائياً"
                            else
                                "Fires right before Maghrib with auto screen & video launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5BFB9)
                        )
                    }
                    Switch(
                        checked = iftarCannonConfig.isEnabled,
                        onCheckedChange = { checked ->
                            prefs.setIftarCannonConfig(iftarCannonConfig.copy(isEnabled = checked))
                            AlarmScheduler.scheduleAll(context)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Timing: offset minutes before Maghrib
                Text(
                    text = if (language.code == "ar") "موعد إطلاق مدفع الإفطار:" else "Iftar Cannon Timing:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                val cannonPresets = listOf(0, 1, 2, 5)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    cannonPresets.forEach { offsetMin ->
                        FilterChip(
                            selected = prefs.getIftarCannonOffsetMinutes() == offsetMin,
                            onClick = {
                                prefs.setIftarCannonOffsetMinutes(offsetMin)
                                AlarmScheduler.scheduleAll(context)
                            },
                            label = {
                                Text(
                                    when (offsetMin) {
                                        0 -> if (language.code == "ar") "مع الأذان مباشرة" else "With Adhan"
                                        else -> if (language.code == "ar") "قبل بـ $offsetMin دقيقة" else "$offsetMin min before"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Open screen toggle for Iftar Cannon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language.code == "ar") "تشغيل شاشة الأذان والمدفع تلقائيًا" else "Auto-Open Screen on Cannon",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (language.code == "ar") "تضيء الشاشة وتفتح تلقائياً عند حلول موعد الإفطار" else "Wakes and shows screen automatically at Iftar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA5BFB9)
                        )
                    }
                    Switch(
                        checked = maghribScreenConfig.autoOpenOnIftarCannon,
                        onCheckedChange = { checked ->
                            adhanRepo.setScreenConfig(PrayerType.MAGHRIB, maghribScreenConfig.copy(autoOpenOnIftarCannon = checked))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video/Audio Selector for Iftar Cannon
                if (iftarCannonConfig.uriString != null) {
                    val isAvail = MediaHelper.isUriAvailable(context, iftarCannonConfig.uriString)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MosqueDarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🎬 ${iftarCannonConfig.fileName ?: "iftar_cannon_media"}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (language.code == "ar") "المدة" else "Duration"}: ${MediaHelper.formatDuration(iftarCannonConfig.durationMs)} • ${MediaHelper.formatFileSize(iftarCannonConfig.sizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5BFB9)
                            )
                            if (!isAvail) {
                                Text(
                                    text = if (language.code == "ar") "⚠️ محذوف" else "⚠️ Missing",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "✓ ${if (language.code == "ar") "جاهز للتشغيل" else "Ready"}",
                                    color = LedEmeraldAccent,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onPreviewVideo(
                                    iftarCannonConfig.uriString,
                                    if (language.code == "ar") "💥 فيديو وصوت مدفع الإفطار" else "Iftar Cannon Video"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F1239))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.preview(language))
                        }

                        OutlinedButton(
                            onClick = {
                                iftarCannonPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(AppStrings.replace(language))
                        }

                        IconButton(
                            onClick = {
                                MediaHelper.deleteInternalMedia(context, "custom_video", "iftar_cannon_media")
                                prefs.setIftarCannonConfig(AdhanVideoConfig())
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            iftarCannonPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F1239))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language.code == "ar") "اختيار فيديو أو صوت مدفع الإفطار من الهاتف" else "Select Iftar Cannon Video/Audio")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Iftar Cannon button
                Button(
                    onClick = {
                        val intent = Intent(context, com.example.notifications.AlarmReceiver::class.java).apply {
                            action = com.example.notifications.AlarmReceiver.ACTION_IFTAR_CANNON
                        }
                        context.sendBroadcast(intent)
                        Toast.makeText(context, if (language.code == "ar") "💥 تم إطلاق تنبيه مدفع الإفطار وشاشته" else "Iftar cannon alert triggered", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBE123C), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (language.code == "ar") "💥 تجربة مدفع الإفطار الآن (الشاشة + الفيديو)" else "💥 Test Iftar Cannon Now (Screen + Video)")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ══════════════════════════════════════════════════════════
        // 4. SUHOOR ALERT TIMING & MUSAHARATI
        // ══════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (language.code == "ar") "توقيت تنبيه السحور" else "Suhoor Alert Timing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = prefs.getSuhoorAlertMode() == SuhoorAlertMode.OFFSET_FAJR,
                        onClick = {
                            prefs.setSuhoorAlertMode(SuhoorAlertMode.OFFSET_FAJR)
                            AlarmScheduler.scheduleAll(context)
                        },
                        label = { Text(if (language.code == "ar") "قبل أذان الفجر" else "Before Fajr") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = prefs.getSuhoorAlertMode() == SuhoorAlertMode.FIXED_TIME,
                        onClick = {
                            prefs.setSuhoorAlertMode(SuhoorAlertMode.FIXED_TIME)
                            AlarmScheduler.scheduleAll(context)
                        },
                        label = { Text(if (language.code == "ar") "ساعة ثابتة" else "Fixed Time") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (prefs.getSuhoorAlertMode() == SuhoorAlertMode.OFFSET_FAJR) {
                    val offsetPresets = listOf(30, 45, 60, 90)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        offsetPresets.forEach { min ->
                            FilterChip(
                                selected = prefs.getSuhoorOffsetMinutes() == min,
                                onClick = {
                                    prefs.setSuhoorOffsetMinutes(min)
                                    AlarmScheduler.scheduleAll(context)
                                },
                                label = { Text("$min min") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = prefs.getSuhoorManualTime(),
                        onValueChange = {
                            prefs.setSuhoorManualTime(it)
                            AlarmScheduler.scheduleAll(context)
                        },
                        label = { Text(if (language.code == "ar") "الساعة (مثال: 04:00)" else "Time (e.g. 04:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Musaharati Video Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "فيديو المسحراتي" else "Musaharati Video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGoldLight
                    )
                    Switch(
                        checked = musaharatiConfig.isEnabled,
                        onCheckedChange = { checked ->
                            prefs.setMusaharatiConfig(musaharatiConfig.copy(isEnabled = checked))
                            AlarmScheduler.scheduleAll(context)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (musaharatiConfig.uriString != null) {
                    val isAvail = MediaHelper.isUriAvailable(context, musaharatiConfig.uriString)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MosqueDarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "🥁 ${musaharatiConfig.fileName ?: "musaharati_video"}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (language.code == "ar") "المدة" else "Duration"}: ${MediaHelper.formatDuration(musaharatiConfig.durationMs)} • ${MediaHelper.formatFileSize(musaharatiConfig.sizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5BFB9)
                            )
                            if (!isAvail) {
                                Text(
                                    text = if (language.code == "ar") "⚠️ محذوف" else "⚠️ Missing",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "✓ ${if (language.code == "ar") "جاهز" else "Ready"}",
                                    color = LedEmeraldAccent,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onPreviewVideo(
                                    musaharatiConfig.uriString,
                                    if (language.code == "ar") "فيديو المسحراتي" else "Musaharati Video"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.preview(language))
                        }

                        OutlinedButton(
                            onClick = {
                                musaharatiPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(AppStrings.replace(language))
                        }

                        IconButton(
                            onClick = {
                                MediaHelper.deleteInternalMedia(context, "custom_video", "musaharati_video")
                                prefs.setMusaharatiConfig(AdhanVideoConfig())
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            musaharatiPicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language.code == "ar") "اختيار فيديو المسحراتي من الهاتف" else "Select Musaharati Video")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Musaharati Alert Button
        Button(
            onClick = {
                val intent = Intent(context, com.example.notifications.AlarmReceiver::class.java).apply {
                    action = com.example.notifications.AlarmReceiver.ACTION_MUSAHARATI
                }
                context.sendBroadcast(intent)
                Toast.makeText(context, if (language.code == "ar") "تم إرسال إشعار وتنبيه المسحراتي" else "Musaharati alert sent", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGoldPrimary, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (language.code == "ar") "تجربة إشعار وتنبيه المسحراتي الآن" else "Test Musaharati Alert Now")
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 6. NOTIFICATION BAR TAB (PERSISTENT STATUS NOTIFICATION)
// ─────────────────────────────────────────────────────────────
@Composable
private fun NotificationBarTab(prefs: AppPreferences, language: AppLanguage) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(prefs.isPersistentNotificationEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = AppStrings.tabNotifications(language),
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (language.code == "ar")
                "شريط دائم في لوحة الإشعارات يعرض الصلاة القادمة والوقت المتبقي وجدول اليوم والتاريخ الهجري والميلادي."
            else
                "A persistent bar in the notifications shade showing next prayer, countdown, daily schedule, and Islamic date.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFA5BFB9),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MosqueCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language.code == "ar") "تفعيل شريط مواقيت الصلاة الدائم" else "Enable Persistent Notification Bar",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (language.code == "ar") "يعمل في الخلفية ويحدث العد التنازلي باستمرار" else "Runs in background and continuously updates countdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA5BFB9)
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        prefs.setPersistentNotificationEnabled(checked)
                        if (checked) {
                            PrayerNotificationService.start(context)
                        } else {
                            PrayerNotificationService.stop(context)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (language.code == "ar") "معاينة تصميم الإشعار:" else "Notification Preview:",
            style = MaterialTheme.typography.labelLarge,
            color = IslamicGoldLight
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Visual simulation of the Android notification & widget
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E242B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Top City & Mosque
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕌", fontSize = 16.sp)
                    Text(
                        text = prefs.getCityName(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Cyan Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF007799), Color(0xFF0284C7), Color(0xFF00A8D6))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language.code == "ar") "+ ٤٧:٤٥" else "+ 47:45",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language.code == "ar") "الظهر" else "Dhuhr",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5 Prayers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    data class ColItem(val nameAr: String, val nameEn: String, val time: String, val badge: String?, val isActive: Boolean)
                    val items = listOf(
                        ColItem("العشاء", "Isha", "08:31 م", null, false),
                        ColItem("المغرب", "Maghrib", "07:12 م", null, false),
                        ColItem("العصر", "Asr", "04:26 م", if (language.code == "ar") "لاحقا" else "Next", false),
                        ColItem("الظهر", "Dhuhr", "12:53 م", if (language.code == "ar") "الان" else "Now", true),
                        ColItem("الفجر", "Fajr", "05:01 ص", null, false)
                    )

                    for (item in items) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (item.isActive) Color(0xFF00A3E0) else Color(0xFFDCE4EC)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .background(if (item.badge != null) Color(0xFF0088BA) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.badge != null) {
                                    Text(item.badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (language.code == "ar") item.nameAr else item.nameEn,
                                color = if (item.isActive) Color.White else Color(0xFF334155),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = item.time,
                                color = if (item.isActive) Color.White else Color(0xFF1E293B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Dates Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "6-9-2026",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language.code == "ar") "الأحد 24 ربيع الأول 1448" else "Sunday 24 Rabi' I 1448",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 7. SETTINGS & SECURITY TAB (PRIVACY FIRST)
// ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsSecurityTab(prefs: AppPreferences, language: AppLanguage) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = AppStrings.tabSettings(language),
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy First Pillar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2923)),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldSecondary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = IslamicGoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language.code == "ar") "الخصوصية والأمان أولاً (Privacy First)" else "Privacy & Security First",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGoldLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language.code == "ar")
                        "• لا يتم طلب أي أذونات غير ضرورية كجهات الاتصال، الرسائل، المكالمات، الميكروفون، أو الكاميرا.\n" +
                        "• لا نجمع أي بيانات ولا نقوم بأي تتبع نهائيًا.\n" +
                        "• يتم استخدام Scoped Storage و Photo Picker الرسمي لاختيار الفيديوهات والأصوات، ولا تُنسخ ملفاتك إلى مجلدات عامة.\n" +
                        "• التطبيق يعمل بدون إنترنت 100% بعد أول تحديد للموقع."
                    else
                        "• No unnecessary permissions (no contacts, messages, calls, mic, or camera).\n" +
                        "• Zero telemetry or data collection.\n" +
                        "• Scoped Storage & official Photo Picker ensures media files remain strictly private on device.\n" +
                        "• 100% functional offline after initial location setup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB5D4CC),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear Location Data Button (Explicit request in prompt)
        Button(
            onClick = {
                prefs.clearLocationData()
                AlarmScheduler.scheduleAll(context)
                PrayerNotificationService.start(context)
                Toast.makeText(
                    context,
                    if (language.code == "ar") "تم حذف بيانات الموقع وإعادتها للافتراضي بنجاح" else "Location data cleared successfully",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_location_data_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (language.code == "ar") "حذف بيانات الموقع المخزنة بالكامل" else "Clear Stored Location Data")
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Battery Optimization Guidance
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language.code == "ar") "⚙️ إعدادات البطارية لضمان دقة الأذان" else "⚙️ Battery Optimization Guide",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (language.code == "ar")
                        "لضمان عدم قيام نظام أندرويد بإيقاف تنبيهات الأذان في الخلفية، يُنصح بتعطيل توفير الطاقة (Battery Optimization) للتطبيق واختيار «غير مقيّد / Unrestricted» من إعدادات النظام."
                    else
                        "To ensure Android does not suppress Adhan alarms in background, set battery usage to 'Unrestricted' in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA5BFB9)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (language.code == "ar") "فتح إعدادات البطارية للنظام" else "Open Battery Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overlay / Draw Over Apps Permission Card
        val canDrawOverlays = Settings.canDrawOverlays(context)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MosqueDarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!canDrawOverlays) Color(0xFF3B82F6) else MosqueCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language.code == "ar") "📲 إذن الظهور فوق التطبيقات الأخرى" else "📲 Draw Over Other Apps Permission",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (canDrawOverlays) Color(0xFF065F46).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (canDrawOverlays) Color(0xFF10B981) else Color(0xFFEF4444))
                    ) {
                        Text(
                            text = if (canDrawOverlays) {
                                if (language.code == "ar") "مفعّل" else "Granted"
                            } else {
                                if (language.code == "ar") "مطلوب" else "Required"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canDrawOverlays) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (language.code == "ar")
                        "يتيح للتطبيق فتح شاشة الأذان وشاشات التنبيهات المخصصة ملء الشاشة تلقائياً حتى وإن كان الهاتف قيد الاستخدام في تطبيق آخر كاليوتيوب أو الواتساب أو المتصفح."
                    else
                        "Allows the app to automatically display the full-screen Adhan and prayer reminder views even while you are actively using other applications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA5BFB9)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canDrawOverlays) Color(0xFF065F46) else Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (canDrawOverlays) {
                            if (language.code == "ar") "إدارة إذن الظهور فوق التطبيقات" else "Manage Overlay Permission"
                        } else {
                            if (language.code == "ar") "منح إذن الظهور فوق التطبيقات الآن" else "Grant Overlay Permission Now"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
