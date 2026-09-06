package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
// 4. ADHAN TAB (5 PRAYERS VIDEO CUSTOMIZATION)
// ─────────────────────────────────────────────────────────────
@Composable
private fun AdhanTab(
    prefs: AppPreferences,
    language: AppLanguage,
    onPreviewVideo: (uri: String?, title: String) -> Unit
) {
    val context = LocalContext.current
    var selectedPrayerForPicker by remember { mutableStateOf<PrayerType?>(null) }

    // Official Photo/Video Picker (Zero broad storage permissions!)
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val pt = selectedPrayerForPicker
        if (uri != null && pt != null) {
            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = true)
            val cfg = AdhanVideoConfig(
                uriString = uri.toString(),
                fileName = meta.displayName,
                durationMs = meta.durationMs,
                sizeBytes = meta.sizeBytes,
                isCompatible = meta.isCompatible,
                isEnabled = true
            )
            prefs.setAdhanConfig(pt, cfg)
            AlarmScheduler.scheduleAll(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = AppStrings.tabAdhan(language),
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )

        // Privacy Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3029))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = IslamicGoldLight, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language.code == "ar")
                        "فيديوهاتك آمنة ومحفوظة محليًا على جهازك فقط، ولا يتم رفعها أو مشاركتها مع أي جهة."
                    else
                        "Your videos are stored locally on your device only and are never uploaded or shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IslamicGoldLight
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val fivePrayers = listOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)

        fivePrayers.forEach { prayer ->
            val config = prefs.getAdhanConfig(prayer)
            val hasVideo = config.uriString != null
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
                            text = AppStrings.getPrayerName(prayer, language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { checked ->
                                prefs.setAdhanConfig(prayer, config.copy(isEnabled = checked))
                                AlarmScheduler.scheduleAll(context)
                            }
                        )
                    }

                    if (hasVideo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MosqueDarkSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🎬 ${config.fileName ?: "video"}",
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
                                    text = "${if (language.code == "ar") "المدة" else "Duration"}: ${MediaHelper.formatDuration(config.durationMs)} • ${MediaHelper.formatFileSize(config.sizeBytes)}",
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

                        // Video actions: Preview, Replace, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onPreviewVideo(
                                        config.uriString,
                                        "${AppStrings.getPrayerName(prayer, language)} - Adhan Video"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.preview(language), style = MaterialTheme.typography.bodySmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedPrayerForPicker = prayer
                                    videoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(AppStrings.replace(language), style = MaterialTheme.typography.bodySmall)
                            }

                            IconButton(
                                onClick = { prefs.resetAdhanConfig(prayer) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = AppStrings.delete(language), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                selectedPrayerForPicker = prayer
                                videoPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16473D))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language.code == "ar") "اختيار فيديو أذان من الهاتف" else "Select Adhan Video")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 5. RAMADAN TAB (SUHOOR & MUSAHARATI VIDEO)
// ─────────────────────────────────────────────────────────────
@Composable
private fun RamadanTab(
    prefs: AppPreferences,
    language: AppLanguage,
    onPreviewVideo: (uri: String?, title: String) -> Unit
) {
    val context = LocalContext.current
    val musaharatiConfig = prefs.getMusaharatiConfig()

    // Video picker for Musaharati
    val musaharatiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val meta = MediaHelper.inspectMediaUri(context, uri, isExpectedVideo = true)
            prefs.setMusaharatiConfig(
                AdhanVideoConfig(
                    uriString = uri.toString(),
                    fileName = meta.displayName,
                    durationMs = meta.durationMs,
                    sizeBytes = meta.sizeBytes,
                    isCompatible = meta.isCompatible,
                    isEnabled = true
                )
            )
            AlarmScheduler.scheduleAll(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (language.code == "ar") "🌙 إعدادات رمضان المبارك" else "🌙 Ramadan Settings",
            style = MaterialTheme.typography.titleLarge,
            color = IslamicGoldPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ramadan Mode (Auto / Manual On / Manual Off)
        Text(
            text = if (language.code == "ar") "وضع رمضان:" else "Ramadan Mode:",
            style = MaterialTheme.typography.labelLarge,
            color = IslamicGoldLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RamadanMode.values().forEach { mode ->
                FilterChip(
                    selected = prefs.getRamadanMode() == mode,
                    onClick = { prefs.setRamadanMode(mode) },
                    label = {
                        Text(when (mode) {
                            RamadanMode.AUTO -> if (language.code == "ar") "تلقائي (بحسب التاريخ)" else "Auto (By Hijri Date)"
                            RamadanMode.MANUAL_ON -> if (language.code == "ar") "مفعّل يدويًا" else "Manual On"
                            RamadanMode.MANUAL_OFF -> if (language.code == "ar") "معطّل" else "Disabled"
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Suhoor Alert Timing Settings
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
                        onClick = { prefs.setSuhoorAlertMode(SuhoorAlertMode.OFFSET_FAJR) },
                        label = { Text(if (language.code == "ar") "قبل أذان الفجر" else "Before Fajr") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = prefs.getSuhoorAlertMode() == SuhoorAlertMode.FIXED_TIME,
                        onClick = { prefs.setSuhoorAlertMode(SuhoorAlertMode.FIXED_TIME) },
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
                                onClick = { prefs.setSuhoorOffsetMinutes(min) },
                                label = { Text("$min min") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = prefs.getSuhoorManualTime(),
                        onValueChange = { prefs.setSuhoorManualTime(it) },
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
                            onClick = { prefs.setMusaharatiConfig(AdhanVideoConfig()) }
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
                Toast.makeText(context, if (language.code == "ar") "تم إرسال إشعار تجريبي للمسحراتي" else "Musaharati test notification sent", Toast.LENGTH_SHORT).show()
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

        // Visual simulation of the Android notification
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2624)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = IslamicGoldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Prayer Time & Adhan • 15 Ramadan 1448", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${AppStrings.nextPrayer(language)}: ${AppStrings.getPrayerName(PrayerType.ASR, language)}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${AppStrings.remaining(language)}: 01:24:35",
                    style = MaterialTheme.typography.bodySmall,
                    color = LedCyanGlow
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {}) { Text(if (language.code == "ar") "فتح التطبيق" else "Open App", color = IslamicGoldPrimary) }
                    TextButton(onClick = {}) { Text(if (language.code == "ar") "كتم/تفعيل" else "Mute/Unmute", color = Color.White) }
                    TextButton(onClick = {}) { Text(if (language.code == "ar") "الأذان" else "Adhan", color = Color.White) }
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

        Spacer(modifier = Modifier.height(30.dp))
    }
}
