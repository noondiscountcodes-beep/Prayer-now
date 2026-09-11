package com.example.engine

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PrayerTimesCalculator {

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    data class SunCoordinates(
        val declination: Double,
        val equationOfTime: Double
    )

    private fun sunPosition(jd: Double): SunCoordinates {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l))))) / 15.0
        val dRad = asin(sin(d2r(e)) * sin(d2r(l)))
        val declination = r2d(dRad)
        val equationOfTime = q / 15.0 - ra
        return SunCoordinates(declination, equationOfTime)
    }

    // Solar noon in hours
    private fun midDay(eot: Double, longitude: Double, timezoneOffsetHours: Double): Double {
        return fixHour(12.0 + timezoneOffsetHours - (longitude / 15.0) - eot)
    }

    // Hour angle for a given sun altitude angle below or above horizon
    private fun sunAngleTime(
        angle: Double,
        latitude: Double,
        declination: Double,
        direction: String
    ): Double {
        val latRad = d2r(latitude)
        val decRad = d2r(declination)
        val altRad = d2r(angle)
        val cosT = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clampedCosT = cosT.coerceIn(-1.0, 1.0)
        val t = r2d(acos(clampedCosT)) / 15.0
        return if (direction == "ccw") -t else t
    }

    // Sun altitude for Asr calculation according to shadow ratio
    private fun asrAltitude(shadowRatio: Double, latitude: Double, declination: Double): Double {
        val d = abs(latitude - declination)
        val cotA = shadowRatio + tan(d2r(d))
        return r2d(atan(1.0 / cotA))
    }

    fun calculate(
        year: Int,
        month: Int, // 1-12
        day: Int,
        latitude: Double,
        longitude: Double,
        timezone: TimeZone,
        dstSetting: DstSetting = DstSetting.AUTO,
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        madhab: Madhab = Madhab.SHAFI,
        customFajrAngle: Double = 18.0,
        customIshaAngle: Double = 17.0,
        customIshaIntervalMinutes: Int? = null,
        fajrOffsetMinutes: Int = 0,
        sunriseOffsetMinutes: Int = 0,
        dhuhrOffsetMinutes: Int = 0,
        asrOffsetMinutes: Int = 0,
        maghribOffsetMinutes: Int = 0,
        ishaOffsetMinutes: Int = 0
    ): PrayerDaySchedule {
        val cal = Calendar.getInstance(timezone)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Effective timezone offset in hours
        val rawOffsetHours = timezone.rawOffset / (1000.0 * 3600.0)
        val dstOffsetHours = when (dstSetting) {
            DstSetting.AUTO -> if (timezone.inDaylightTime(cal.time)) 1.0 else 0.0
            DstSetting.ON -> 1.0
            DstSetting.OFF -> 0.0
        }
        val effectiveTzOffset = rawOffsetHours + dstOffsetHours

        val jd = julianDay(year, month, day)
        val sunCoords = sunPosition(jd)
        val noon = midDay(sunCoords.equationOfTime, longitude, effectiveTzOffset)

        // Fajr angle
        val fajrAngle = if (method == CalculationMethod.CUSTOM) customFajrAngle else method.defaultFajrAngle
        val fajrDiff = sunAngleTime(-fajrAngle, latitude, sunCoords.declination, "ccw")
        val fajrHour = fixHour(noon + fajrDiff)

        // Sunrise (with atmospheric refraction -0.833 degrees)
        val sunriseDiff = sunAngleTime(-0.833, latitude, sunCoords.declination, "ccw")
        val sunriseHour = fixHour(noon + sunriseDiff)

        // Dhuhr (solar noon + slight safety buffer of ~1 min)
        val dhuhrHour = fixHour(noon + (1.0 / 60.0))

        // Asr
        val asrAlt = asrAltitude(madhab.shadowRatio, latitude, sunCoords.declination)
        val asrDiff = sunAngleTime(asrAlt, latitude, sunCoords.declination, "cw")
        val asrHour = fixHour(noon + asrDiff)

        // Sunset
        val sunsetDiff = sunAngleTime(-0.833, latitude, sunCoords.declination, "cw")
        val sunsetHour = fixHour(noon + sunsetDiff)

        // Maghrib
        val maghribHour = if (method.maghribAngle != null) {
            val maghribDiff = sunAngleTime(-method.maghribAngle, latitude, sunCoords.declination, "cw")
            fixHour(noon + maghribDiff)
        } else if (method.maghribMinutes != null) {
            fixHour(sunsetHour + (method.maghribMinutes / 60.0))
        } else {
            // Standard sunset + 2 minutes safety
            fixHour(sunsetHour + (2.0 / 60.0))
        }

        // Isha
        val ishaInterval = if (method == CalculationMethod.CUSTOM) customIshaIntervalMinutes else method.ishaIntervalMinutes
        val ishaHour = if (ishaInterval != null && ishaInterval > 0) {
            fixHour(maghribHour + (ishaInterval / 60.0))
        } else {
            val ishaAngle = if (method == CalculationMethod.CUSTOM) customIshaAngle else method.defaultIshaAngle
            val ishaDiff = sunAngleTime(-ishaAngle, latitude, sunCoords.declination, "cw")
            fixHour(noon + ishaDiff)
        }

        fun toTimeEntry(type: PrayerType, hourFraction: Double, offsetMinutes: Int = 0): PrayerTimeEntry {
            var totalMinutes = (hourFraction * 60.0).roundToInt() + offsetMinutes
            val minutesInDay = 24 * 60
            totalMinutes = ((totalMinutes % minutesInDay) + minutesInDay) % minutesInDay
            val h = totalMinutes / 60
            val m = totalMinutes % 60

            val entryCal = Calendar.getInstance(timezone)
            entryCal.set(Calendar.YEAR, year)
            entryCal.set(Calendar.MONTH, month - 1)
            entryCal.set(Calendar.DAY_OF_MONTH, day)
            entryCal.set(Calendar.HOUR_OF_DAY, h)
            entryCal.set(Calendar.MINUTE, m)
            entryCal.set(Calendar.SECOND, 0)
            entryCal.set(Calendar.MILLISECOND, 0)

            val f24 = String.format(Locale.US, "%02d:%02d", h, m)
            val ampm = if (h < 12) "AM" else "PM"
            val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
            val f12 = String.format(Locale.US, "%02d:%02d %s", h12, m, ampm)

            return PrayerTimeEntry(type, entryCal.timeInMillis, f24, f12)
        }

        return PrayerDaySchedule(
            dateYear = year,
            dateMonth = month,
            dateDay = day,
            fajr = toTimeEntry(PrayerType.FAJR, fajrHour, fajrOffsetMinutes),
            sunrise = toTimeEntry(PrayerType.SUNRISE, sunriseHour, sunriseOffsetMinutes),
            dhuhr = toTimeEntry(PrayerType.DHUHR, dhuhrHour, dhuhrOffsetMinutes),
            asr = toTimeEntry(PrayerType.ASR, asrHour, asrOffsetMinutes),
            maghrib = toTimeEntry(PrayerType.MAGHRIB, maghribHour, maghribOffsetMinutes),
            isha = toTimeEntry(PrayerType.ISHA, ishaHour, ishaOffsetMinutes)
        )
    }

    fun calculateWithPreferences(
        year: Int,
        month: Int,
        day: Int,
        prefs: com.example.data.AppPreferences
    ): PrayerDaySchedule {
        return calculate(
            year = year,
            month = month,
            day = day,
            latitude = prefs.getLatitude(),
            longitude = prefs.getLongitude(),
            timezone = prefs.getTimezone(),
            dstSetting = prefs.getDstSetting(),
            method = prefs.getCalculationMethod(),
            madhab = prefs.getMadhab(),
            customFajrAngle = prefs.getCustomFajrAngle(),
            customIshaAngle = prefs.getCustomIshaAngle(),
            fajrOffsetMinutes = prefs.getPrayerOffset(PrayerType.FAJR),
            sunriseOffsetMinutes = prefs.getPrayerOffset(PrayerType.SUNRISE),
            dhuhrOffsetMinutes = prefs.getPrayerOffset(PrayerType.DHUHR),
            asrOffsetMinutes = prefs.getPrayerOffset(PrayerType.ASR),
            maghribOffsetMinutes = prefs.getPrayerOffset(PrayerType.MAGHRIB),
            ishaOffsetMinutes = prefs.getPrayerOffset(PrayerType.ISHA)
        )
    }
}

enum class DstSetting {
    AUTO,
    ON,
    OFF
}
