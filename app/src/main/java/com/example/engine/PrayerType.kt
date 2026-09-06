package com.example.engine

enum class PrayerType {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA;

    val isObligatoryPrayer: Boolean
        get() = this != SUNRISE
}
