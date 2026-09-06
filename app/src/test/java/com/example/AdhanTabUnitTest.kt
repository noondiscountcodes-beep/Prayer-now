package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.engine.PrayerType
import com.example.media.AdhanZipManager
import com.example.notifications.AdhanPlaybackStage
import com.example.notifications.AdhanSequencePlayer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdhanTabUnitTest {

    private lateinit var context: Context
    private lateinit var repo: AdhanPreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = AdhanPreferencesRepository(context)
    }

    @Test
    fun testAudioConfigPersistencePerPrayer() {
        val fajrConfig = PrayerAudioConfig(
            uriString = "content://media/external/audio/fajr.mp3",
            fileName = "fajr_makkah.mp3",
            durationMs = 180000L,
            sizeBytes = 3500000L,
            isCompatible = true,
            isEnabled = true
        )
        repo.setAudioConfig(PrayerType.FAJR, fajrConfig)

        val retrievedFajr = repo.getAudioConfig(PrayerType.FAJR)
        assertEquals("content://media/external/audio/fajr.mp3", retrievedFajr.uriString)
        assertEquals("fajr_makkah.mp3", retrievedFajr.fileName)
        assertTrue(retrievedFajr.isEnabled)

        // Verify other prayers remain default
        val dhuhrConfig = repo.getAudioConfig(PrayerType.DHUHR)
        assertNull(dhuhrConfig.uriString)

        // Reset
        repo.resetAudioConfig(PrayerType.FAJR)
        val afterReset = repo.getAudioConfig(PrayerType.FAJR)
        assertNull(afterReset.uriString)
    }

    @Test
    fun testAlertConfigPersistencePerPrayer() {
        val asrAlert = PrayerAlertSoundConfig(
            uriString = "content://media/external/audio/beep.mp3",
            fileName = "beep.mp3",
            durationMs = 4000L,
            sizeBytes = 50000L,
            isCompatible = true,
            isEnabled = true
        )
        repo.setAlertConfig(PrayerType.ASR, asrAlert)

        val retrievedAsr = repo.getAlertConfig(PrayerType.ASR)
        assertEquals("content://media/external/audio/beep.mp3", retrievedAsr.uriString)
        assertTrue(retrievedAsr.isEnabled)

        repo.resetAlertConfig(PrayerType.ASR)
        assertNull(repo.getAlertConfig(PrayerType.ASR).uriString)
    }

    @Test
    fun testDuaVideoConfigPersistencePerPrayer() {
        val maghribDua = PrayerDuaVideoConfig(
            uriString = "content://media/external/video/dua.mp4",
            fileName = "dua_wasila.mp4",
            durationMs = 45000L,
            sizeBytes = 12000000L,
            isCompatible = true,
            isEnabled = true
        )
        repo.setDuaConfig(PrayerType.MAGHRIB, maghribDua)

        val retrieved = repo.getDuaConfig(PrayerType.MAGHRIB)
        assertEquals("content://media/external/video/dua.mp4", retrieved.uriString)
        assertEquals("dua_wasila.mp4", retrieved.fileName)
        assertTrue(retrieved.isEnabled)
    }

    @Test
    fun testScreenConfigPersistence() {
        val screenConfig = AdhanScreenConfig(
            displayMode = AdhanScreenDisplayMode.SLIDESHOW,
            slideIntervalSec = 10,
            showPrayerName = true,
            showClock = true,
            showHijriDate = true,
            showGregorianDate = false,
            selectedImageNames = listOf("kaaba.jpg", "madinah.jpg")
        )
        repo.setScreenConfig(PrayerType.ISHA, screenConfig)

        val retrieved = repo.getScreenConfig(PrayerType.ISHA)
        assertEquals(AdhanScreenDisplayMode.SLIDESHOW, retrieved.displayMode)
        assertEquals(10, retrieved.slideIntervalSec)
        assertTrue(retrieved.showClock)
        assertFalse(retrieved.showGregorianDate)
        assertEquals(2, retrieved.selectedImageNames.size)
        assertTrue(retrieved.selectedImageNames.contains("kaaba.jpg"))
    }

    @Test
    fun testAdhanSequencePlayerStop() {
        AdhanSequencePlayer.stopAll()
        assertEquals(AdhanPlaybackStage.IDLE, AdhanSequencePlayer.playbackState.value.stage)
        assertFalse(AdhanSequencePlayer.playbackState.value.isPlaying)
    }
}
