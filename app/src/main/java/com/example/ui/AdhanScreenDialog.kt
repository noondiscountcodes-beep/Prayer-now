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
import androidx.compose.ui.platform.LocalView
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

        // 3. Immersive Content Layout (respects safe insets while keeping background 100% full screen)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Close button, Live status badge, and Mute button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        AdhanSequencePlayer.stopAll()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Stage status badge
                val stageBadgeText = when (triggerType) {
                    "ALERT" -> if (language.code == "ar") "🔔 تنبيه اقتراب موعد الصلاة" else "🔔 Prayer Reminder"
                    "SUHOOR" -> if (language.code == "ar") "🌙 وقت السحور المبارك" else "🌙 Blessed Suhoor Time"
                    "IFTAR_CANNON" -> if (language.code == "ar") "💥 مدفع الإفطار في رمضان" else "💥 Ramadan Iftar Cannon"
                    else -> when (playbackState.stage) {
                        AdhanPlaybackStage.PLAYING_ALERT -> if (language.code == "ar") "🔔 صوت التنبيه" else "🔔 Alert Sound"
                        AdhanPlaybackStage.PLAYING_ADHAN -> if (language.code == "ar") "🕌 صوت الأذان المبارك" else "🕌 Adhan Audio"
                        AdhanPlaybackStage.FINISHED -> if (language.code == "ar") "✓ اكتمل الأذان" else "✓ Adhan Completed"
                        AdhanPlaybackStage.IDLE -> if (language.code == "ar") "شاشة الأذان المبارك" else "Adhan Screen"
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when (triggerType) {
                        "ALERT" -> Color(0xFFD97706).copy(alpha = 0.95f)
                        "SUHOOR" -> Color(0xFF4F46E5).copy(alpha = 0.95f)
                        "IFTAR_CANNON" -> Color(0xFFE11D48).copy(alpha = 0.95f)
                        else -> when (playbackState.stage) {
                            AdhanPlaybackStage.PLAYING_ALERT -> Color(0xFFD97706).copy(alpha = 0.95f)
                            AdhanPlaybackStage.PLAYING_ADHAN -> Color(0xFF0D9488).copy(alpha = 0.95f)
                            else -> Color(0xFF1E293B).copy(alpha = 0.90f)
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (playbackState.isPlaying) Icons.Default.VolumeUp else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stageBadgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { AdhanSequencePlayer.stopAll() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.VolumeOff, contentDescription = "Mute", tint = Color.White)
                }
            }

            // Middle: Scrollable center area for responsiveness across phones & tablets
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    style = MaterialTheme.typography.titleLarge,
                    color = IslamicGoldLight,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                val prayerName = AppStrings.getPrayerName(prayer, language)
                if (screenConfig.showPrayerName) {
                    val headlineText = when (triggerType) {
                        "ALERT" -> if (language.code == "ar") "متبقي ${alertMinutes ?: 15} دقيقة على صلاة $prayerName" else "${alertMinutes ?: 15} min before $prayerName"
                        "SUHOOR" -> if (language.code == "ar") "حان موعد السحور المبارك — صياماً مقبولاً" else "Blessed Suhoor Time"
                        "IFTAR_CANNON" -> if (language.code == "ar") "💥 مدفع الإفطار.. اضْرِب!" else "💥 Ramadan Iftar Cannon"
                        else -> if (language.code == "ar") "حان الآن موعد أذان صلاة $prayerName" else "Now Adhan for $prayerName"
                    }
                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.headlineLarge,
                        color = IslamicGoldPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (screenConfig.showClock) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = Color(0xFF38BDF8), // Glowing Cyan LED clock
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calligraphy / Islamic Inscription
                val calligraphyText = when (triggerType) {
                    "ALERT" -> if (language.code == "ar") "حَيَّ عَلَى الصَّلَاةِ • حَيَّ عَلَى الْفَلَاحِ\nتأهب للوضوء والاستعداد لصلاة $prayerName" else "Hurry to prayer • Prepare for $prayerName"
                    "SUHOOR" -> if (language.code == "ar") "«تَسَحَّرُوا فَإِنَّ فِي السَّحُورِ بَرَكَةً»\nاصحى يا نايم وحّد الدايم.. حان موعد السحور المبارك" else "Take Suhoor, for indeed in Suhoor there is blessing"
                    "IFTAR_CANNON" -> if (language.code == "ar") "«ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ»\nاللَّهُمَّ لَكَ صُمْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ • صياماً مقبولاً وإفطاراً شهياً" else "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills"
                    else -> "اللهُ أَكْبَرُ • اللهُ أَكْبَرُ\nأَشْهَدُ أَن لَّا إِلَٰهَ إِلَّا اللَّهُ • أَشْهَدُ أَنَّ مُحَمَّدًا رَّسُولُ اللَّهِ"
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = calligraphyText,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        lineHeight = 28.sp
                    )
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

