package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CalculationMethod
import com.example.engine.Madhab
import com.example.engine.PrayerTimesCalculator
import com.example.engine.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Prayer Time & Adhan", appName)
    }

    @Test
    fun `test prayer times calculation for Makkah`() {
        val schedule = PrayerTimesCalculator.calculate(
            year = 2026,
            month = 9,
            day = 6,
            latitude = 21.4225,
            longitude = 39.8262,
            timezone = TimeZone.getTimeZone("Asia/Riyadh"),
            method = CalculationMethod.UMM_AL_QURA,
            madhab = Madhab.SHAFI
        )
        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)
        assertEquals(PrayerType.FAJR, schedule.fajr.type)
        assertEquals(PrayerType.DHUHR, schedule.dhuhr.type)
    }
}
