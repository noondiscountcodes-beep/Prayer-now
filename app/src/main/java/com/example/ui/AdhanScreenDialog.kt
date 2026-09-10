package com.example.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.AdhanPreferencesRepository
import com.example.data.AdhanScreenDisplayMode
import com.example.data.AppPreferences
import com.example.engine.HijriCalendarHelper
import com.example.engine.PrayerDaySchedule
import com.example.engine.PrayerTimesCalculator
import com.example.engine.PrayerType
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.media.AdhanZipManager
import com.example.notifications.AdhanPlaybackStage
import com.example.notifications.AdhanSequencePlayer
import com.example.ui.theme.IslamicGoldLight
import com.example.ui.theme.IslamicGoldPrimary
import com.example.ui.theme.MosqueDarkBackground
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Truly full-screen immersive view for Adhan, Pre-Prayer Alerts, Suhoor, and Ramadan Iftar Cannon.
 * Takes 100% of the display edge-to-edge behind status and navigation bars.
 */
@Composable
fun AdhanFullScreenView(
    prayer: PrayerType,
    prefs: AppPreferences,
    triggerType: String = "ADHAN",
    alertMinutes: Int? = null,
    onDismiss: () -> Unit,
    onOpenDuaVideo: ((uri: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val repo = remember { AdhanPreferencesRepository(context) }
    val screenConfig = remember { repo.getScreenConfig(prayer) }
    val duaConfig = remember { repo.getDuaConfig(prayer) }
    val language = prefs.getLanguage()

    // Handle back button / gesture to stop audio and dismiss immediately
    BackHandler {
        AdhanSequencePlayer.stopAll()
        onDismiss()
    }

    // Load available images
    val extractedImages = remember { AdhanZipManager.getExtractedImages(context, prayer) }
    val activeImages = remember(extractedImages, screenConfig.selectedImageNames) {
        if (screenConfig.selectedImageNames.isNotEmpty()) {
            val selected = extractedImages.filter { screenConfig.selectedImageNames.contains(it.name) }
            if (selected.isNotEmpty()) selected else extractedImages
        } else {
            extractedImages
        }
    }

    // Current slide index for slideshow
    var currentImageIndex by remember { mutableIntStateOf(0) }

    // Live clock
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Sequence playback stage
    val playbackState by AdhanSequencePlayer.playbackState.collectAsState()

    // Timer effect for clock and slideshow
    LaunchedEffect(activeImages, screenConfig) {
        val intervalMs = (screenConfig.slideIntervalSec.coerceAtLeast(1) * 1000).toLong()
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
            if (screenConfig.displayMode == AdhanScreenDisplayMode.SLIDESHOW && activeImages.size > 1) {
                if ((currentTimeMillis / 1000) % screenConfig.slideIntervalSec.coerceAtLeast(1) == 0L) {
                    currentImageIndex = (currentImageIndex + 1) % activeImages.size
                }
            }
        }
    }

    // Auto-launch Dua video when Adhan sequence completes
    LaunchedEffect(playbackState.stage) {
        if (playbackState.stage == AdhanPlaybackStage.FINISHED) {
            val fallbackUri = com.example.engine.PrayerType.entries.map { repo.getDuaConfig(it) }
                .firstOrNull { it.isEnabled && !it.uriString.isNullOrBlank() }?.uriString

            val targetUri = if (duaConfig.isEnabled && !duaConfig.uriString.isNullOrBlank()) {
                duaConfig.uriString
            } else {
                fallbackUri
            }

            if (!targetUri.isNullOrBlank()) {
                delay(400)
                onOpenDuaVideo?.invoke(targetUri)
            }
        }
    }

    val cal = Calendar.getInstance(prefs.getTimezone()).apply {
        timeInMillis = currentTimeMillis
    }
    val timeFormat = SimpleDateFormat("hh:mm:ss a", if (language.code == "ar") Locale("ar") else Locale.ENGLISH)
    val timeStr = timeFormat.format(cal.time)

    val hijri = HijriCalendarHelper.fromGregorian(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
        prefs.getHijriAdjustment()
    )
    val hijriStr = hijri.format(language.code)

    val gregFormat = SimpleDateFormat("EEEE, d MMMM yyyy", if (language.code == "ar") Locale("ar") else Locale.ENGLISH)
    val gregStr = gregFormat.format(cal.time)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MosqueDarkBackground)
    ) {
        // 1. Edge-to-Edge Background Image / Slideshow filling 100% of the screen
        if (activeImages.isNotEmpty()) {
            val currentFile = activeImages.getOrNull(currentImageIndex) ?: activeImages.first()
            AnimatedContent(
                targetState = currentFile.absolutePath,
                transitionSpec = {
                    fadeIn(animationSpec = tween(800)) togetherWith fadeOut(animationSpec = tween(800))
                },
                label = "SlideshowImage"
            ) { imagePath ->
                Image(
                    painter = rememberAsyncImagePainter(File(imagePath)),
                    contentDescription = "Adhan Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Full-Screen Vignette & Dark Gradient for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // 3. Content Layout - true full-screen experience with no artificial frame
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(16.dp),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(16.dp),
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Force LTR row so Controls are on the physical LEFT and Adhan title is on physical RIGHT
            val prayerName = AppStrings.getPrayerName(prayer, language)
            val isAdhan = (triggerType != "ALERT" && triggerType != "SUHOOR" && triggerType != "IFTAR_CANNON")

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action buttons on physical LEFT
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { AdhanSequencePlayer.stopAll() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = "Mute", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                AdhanSequencePlayer.stopAll()
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Top-Right on physical RIGHT: "أذان + اسم الصلاة" at Adhan
                    if (isAdhan) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.70f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGoldLight.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language.code == "ar") "أذان $prayerName" else "Adhan $prayerName",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = IslamicGoldPrimary,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    } else if (triggerType == "SUHOOR") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF4F46E5).copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (language.code == "ar") "السحور" else "Suhoor",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else if (triggerType == "IFTAR_CANNON") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE11D48).copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (language.code == "ar") "مدفع الإفطار" else "Iftar Cannon",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        // In ALERT mode: top right is kept clean
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            }

            // Center Area:
            // At ALERT: Prominently centered "يتبقى + عدد الدقائق + على أذان + اسم الصلاة"
            // At ADHAN: Clean view, glowing digital clock (no text clutter)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (triggerType == "ALERT") {
                    val alertMins = alertMinutes ?: 15
                    val minWord = when {
                        alertMins == 1 -> if (language.code == "ar") "دقيقة واحدة" else "1 minute"
                        alertMins == 2 -> if (language.code == "ar") "دقيقتان" else "2 minutes"
                        alertMins in 3..10 -> if (language.code == "ar") "$alertMins دقائق" else "$alertMins minutes"
                        else -> if (language.code == "ar") "$alertMins دقيقة" else "$alertMins minutes"
                    }
                    val alertCenterText = if (language.code == "ar") {
                        "يتبقى $minWord على أذان $prayerName"
                    } else {
                        "$minWord remaining until $prayerName Adhan"
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = "🔔",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = alertCenterText,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 36.sp
                                ),
                                color = Color(0xFFFDE68A),
                                textAlign = TextAlign.Center
                            )

                            if (screenConfig.showClock) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    ),
                                    color = Color(0xFF38BDF8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Adhan, Suhoor, or Iftar Cannon center view
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (triggerType == "SUHOOR") {
                            Text(
                                text = if (language.code == "ar") "حان موعد السحور المبارك" else "Blessed Suhoor Time",
                                style = MaterialTheme.typography.headlineMedium,
                                color = IslamicGoldPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else if (triggerType == "IFTAR_CANNON") {
                            Text(
                                text = if (language.code == "ar") "💥 مدفع الإفطار.. اضْرِب!" else "💥 Ramadan Iftar Cannon",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFFFB7185),
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (screenConfig.showClock) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.60f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.sp
                                        ),
                                        color = Color(0xFF38BDF8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Dates, Media Buttons & Dismiss action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (screenConfig.showHijriDate) {
                    Text(
                        text = "🌙 $hijriStr",
                        style = MaterialTheme.typography.titleMedium,
                        color = IslamicGoldLight,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (screenConfig.showGregorianDate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 $gregStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val cannonConfig = prefs.getIftarCannonConfig()
                if (triggerType == "IFTAR_CANNON" && cannonConfig.isEnabled && !cannonConfig.uriString.isNullOrBlank()) {
                    Button(
                        onClick = {
                            AdhanSequencePlayer.stopAll()
                            onOpenDuaVideo?.invoke(cannonConfig.uriString)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language.code == "ar") "💥 تشغيل فيديو مدفع الإفطار" else "💥 Play Iftar Cannon Video",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (duaConfig.isEnabled && !duaConfig.uriString.isNullOrBlank()) {
                    Button(
                        onClick = {
                            AdhanSequencePlayer.stopAll()
                            onOpenDuaVideo?.invoke(duaConfig.uriString)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language.code == "ar") "🎬 تشغيل فيديو دعاء بعد الأذان" else "🎬 Play Post-Adhan Du'aa Video",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Explicit Dismiss Button
                OutlinedButton(
                    onClick = {
                        AdhanSequencePlayer.stopAll()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        text = if (language.code == "ar") "إغلاق الشاشة والعودة للساعة" else "Dismiss and Return to Clock",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Dialog wrapper for AdhanFullScreenView to ensure dialog callers also get a true edge-to-edge
 * MATCH_PARENT full screen window without platform dialog padding or dimming.
 */
@Composable
fun AdhanScreenDialog(
    prayer: PrayerType,
    prefs: AppPreferences,
    triggerType: String = "ADHAN",
    alertMinutes: Int? = null,
    onDismiss: () -> Unit,
    onOpenDuaVideo: ((uri: String?) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = {
            AdhanSequencePlayer.stopAll()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                win.setDimAmount(0f)
                WindowCompat.setDecorFitsSystemWindows(win, false)
            }
        }

        AdhanFullScreenView(
            prayer = prayer,
            prefs = prefs,
            triggerType = triggerType,
            alertMinutes = alertMinutes,
            onDismiss = onDismiss,
            onOpenDuaVideo = onOpenDuaVideo
        )
    }
}

