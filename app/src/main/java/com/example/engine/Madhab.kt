package com.example.engine

enum class Madhab(val shadowRatio: Double) {
    SHAFI(1.0),
    HANAFI(2.0);

    fun getDisplayName(languageCode: String): String {
        return when (this) {
            SHAFI -> when (languageCode) {
                "ar" -> "الشافعي / المالكي / الحنبلي (ظل القامة 1)"
                "fr" -> "Chaféite / Malikite / Hanbalite (Ombre 1x)"
                else -> "Shafi'i / Maliki / Hanbali (Shadow 1x)"
            }
            HANAFI -> when (languageCode) {
                "ar" -> "الحنفي (ظل القامة 2)"
                "fr" -> "Hanafite (Ombre 2x)"
                else -> "Hanafi (Shadow 2x)"
            }
        }
    }
}
