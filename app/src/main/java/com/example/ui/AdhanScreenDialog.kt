package com.example.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@Composable
fun AdhanScreenDialog(
    prayer: PrayerType,
    prefs: AppPreferences,
    onDismiss: () -> Unit,
    onOpenDuaVideo: ((uri: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val repo = remember { AdhanPreferencesRepository(context) }
    val screenConfig = remember { repo.getScreenConfig(prayer) }
    val duaConfig = remember { repo.getDuaConfig(prayer) }
    val language = prefs.getLanguage()

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
            if (duaConfig.isEnabled && !duaConfig.uriString.isNullOrBlank()) {
                delay(500)
                onOpenDuaVideo?.invoke(duaConfig.uriString)
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
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val hijriStr = hijri.format(language.code)

    val gregFormat = SimpleDateFormat("EEEE, d MMMM yyyy", if (language.code == "ar") Locale("ar") else Locale.ENGLISH)
    val gregStr = gregFormat.format(cal.time)

    Dialog(
        onDismissRequest = {
            AdhanSequencePlayer.stopAll()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MosqueDarkBackground)
        ) {
            // Background Image / Slideshow
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

            // Dark gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Dismiss & Sound controls
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
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Stage status badge
                    val stageBadgeText = when (playbackState.stage) {
                        AdhanPlaybackStage.PLAYING_ALERT -> if (language.code == "ar") "🔔 صوت التنبيه" else "🔔 Alert Sound"
                        AdhanPlaybackStage.PLAYING_ADHAN -> if (language.code == "ar") "🕌 صوت الأذان المبارك" else "🕌 Adhan Audio"
                        AdhanPlaybackStage.FINISHED -> if (language.code == "ar") "✓ اكتمل الأذان" else "✓ Adhan Completed"
                        AdhanPlaybackStage.IDLE -> if (language.code == "ar") "معاينة شاشة الأذان" else "Adhan Screen Preview"
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (playbackState.stage) {
                            AdhanPlaybackStage.PLAYING_ALERT -> Color(0xFFD97706).copy(alpha = 0.9f)
                            AdhanPlaybackStage.PLAYING_ADHAN -> Color(0xFF0D9488).copy(alpha = 0.9f)
                            else -> Color(0xFF1E293B).copy(alpha = 0.85f)
                        }
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
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Mute", tint = Color.White)
                    }
                }

                // Middle: Islamic Arch Ornament, Prayer Name & Big Clock
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        style = MaterialTheme.typography.titleMedium,
                        color = IslamicGoldLight,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (screenConfig.showPrayerName) {
                        val prayerName = AppStrings.getPrayerName(prayer, language)
                        Text(
                            text = if (language.code == "ar") "حان الآن أذان صلاة $prayerName" else "Now Adhan for $prayerName",
                            style = MaterialTheme.typography.headlineMedium,
                            color = IslamicGoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (screenConfig.showClock) {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = Color(0xFF38BDF8), // Cyan LED clock
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calligraphy or Takbeer
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGoldLight.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "اللهُ أَكْبَرُ • اللهُ أَكْبَرُ\nأَشْهَدُ أَن لَّا إِلَٰهَ إِلَّا اللَّهُ • أَشْهَدُ أَنَّ مُحَمَّدًا رَّسُولُ اللَّهِ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            lineHeight = 26.sp
                        )
                    }
                }

                // Bottom: Dates and Dua trigger if available
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (screenConfig.showHijriDate) {
                        Text(
                            text = "🌙 $hijriStr",
                            style = MaterialTheme.typography.bodyLarge,
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // If Dua video is configured, provide direct button or status
                    if (duaConfig.isEnabled && !duaConfig.uriString.isNullOrBlank()) {
                        Button(
                            onClick = {
                                AdhanSequencePlayer.stopAll()
                                onOpenDuaVideo?.invoke(duaConfig.uriString)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = if (language.code == "ar") "🎬 تشغيل فيديو دعاء بعد الأذان" else "🎬 Play Post-Adhan Du'aa Video",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
