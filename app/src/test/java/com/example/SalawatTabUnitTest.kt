package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.SalawatConfig
import com.example.data.SalawatPreferencesRepository
import com.example.media.SalawatZipManager
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
class SalawatTabUnitTest {

    private lateinit var context: Context
    private lateinit var repo: SalawatPreferencesRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repo = SalawatPreferencesRepository(context)
        SalawatZipManager.clearAllAudios(context)
    }

    @Test
    fun testSalawatConfigPersistence() {
        val config = SalawatConfig(
            isEnabled = true,
            intervalMinutes = 45,
            isAllDay = false,
            startHour = 7,
            startMinute = 30,
            endHour = 23,
            endMinute = 0,
            isFridayOnly = true,
            vibrate = true,
            audioSelectionMode = "SPECIFIC",
            selectedAudioFileName = "custom_salawat.mp3",
            showPersistentNotification = true,
            nextScheduledTriggerMillis = 1750000000000L
        )
        repo.setConfig(config)

        val retrieved = repo.getConfig()
        assertTrue(retrieved.isEnabled)
        assertEquals(45, retrieved.intervalMinutes)
        assertFalse(retrieved.isAllDay)
        assertEquals(7, retrieved.startHour)
        assertEquals(30, retrieved.startMinute)
        assertEquals(23, retrieved.endHour)
        assertEquals(0, retrieved.endMinute)
        assertTrue(retrieved.isFridayOnly)
        assertTrue(retrieved.vibrate)
        assertEquals("SPECIFIC", retrieved.audioSelectionMode)
        assertEquals("custom_salawat.mp3", retrieved.selectedAudioFileName)
        assertTrue(retrieved.showPersistentNotification)
        assertEquals(1750000000000L, retrieved.nextScheduledTriggerMillis)
    }

    @Test
    fun testNextTriggerCalculation() {
        val config = SalawatConfig(
            isEnabled = true,
            intervalMinutes = 30,
            isAllDay = true
        )
        val now = System.currentTimeMillis()
        val next = repo.calculateNextTriggerTime(config, now)
        assertTrue(next > now)
        assertEquals(now + 30 * 60 * 1000L, next)

        val disabledConfig = config.copy(isEnabled = false)
        val nextDisabled = repo.calculateNextTriggerTime(disabledConfig, now)
        assertEquals(-1L, nextDisabled)
    }

    @Test
    fun testSalawatZipExtractionAndRandomAudio() {
        // Create a mock zip file containing audio files
        val zipFile = File(context.cacheDir, "test_salawat.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Audio 1
            zos.putNextEntry(ZipEntry("salawat_audio_1.mp3"))
            zos.write("Fake MP3 Audio 1 Content".toByteArray())
            zos.closeEntry()

            // Audio 2
            zos.putNextEntry(ZipEntry("subfolder/salawat_audio_2.wav"))
            zos.write("Fake WAV Audio 2 Content".toByteArray())
            zos.closeEntry()

            // Non-audio file
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("Instructions".toByteArray())
            zos.closeEntry()
        }

        val result = SalawatZipManager.extractSalawatZip(context, Uri.fromFile(zipFile), replaceExisting = true)
        assertTrue(result.success)
        assertEquals(2, result.extractedCount)
        assertEquals(1, result.ignoredCount)

        val audioFiles = SalawatZipManager.getAudioFiles(context)
        assertEquals(2, audioFiles.size)

        val randomAudio = SalawatZipManager.getRandomAudio(context)
        assertNotNull(randomAudio)
        assertTrue(randomAudio!!.exists())

        // Test finding specific audio by name
        val specificFile = SalawatZipManager.getAudioFileByName(context, "salawat_audio_1.mp3")
        assertNotNull(specificFile)
        assertTrue(specificFile!!.name.endsWith("salawat_audio_1.mp3"))

        // Test getEffectiveAudio with SPECIFIC mode
        val specificConfig = SalawatConfig(
            audioSelectionMode = "SPECIFIC",
            selectedAudioFileName = specificFile.name
        )
        val effectiveSpecific = SalawatZipManager.getEffectiveAudio(context, specificConfig)
        assertNotNull(effectiveSpecific)
        assertEquals(specificFile.name, effectiveSpecific!!.name)

        // Test getEffectiveAudio with RANDOM mode
        val randomConfig = SalawatConfig(
            audioSelectionMode = "RANDOM"
        )
        val effectiveRandom = SalawatZipManager.getEffectiveAudio(context, randomConfig)
        assertNotNull(effectiveRandom)
        assertTrue(effectiveRandom!!.name.contains("salawat_audio"))

        // Clear
        SalawatZipManager.clearAllAudios(context)
        val emptyList = SalawatZipManager.getAudioFiles(context)
        assertEquals(0, emptyList.size)
        assertNull(SalawatZipManager.getRandomAudio(context))
    }

    @Test
    fun testSalawatPersistentNotificationBuilding() {
        val nextTime = System.currentTimeMillis() + 15 * 60 * 1000L
        val notification = com.example.notifications.PrayerNotificationHelper.buildSalawatPersistentNotification(context, nextTime)
        assertNotNull(notification)
        assertNotNull(notification.contentView)
        assertNotNull(notification.bigContentView)
    }
}
