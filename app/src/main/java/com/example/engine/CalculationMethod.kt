package com.example.engine

enum class CalculationMethod(
    val defaultFajrAngle: Double,
    val defaultIshaAngle: Double,
    val ishaIntervalMinutes: Int? = null, // e.g. Umm Al-Qura 90 min after Maghrib
    val maghribAngle: Double? = null,
    val maghribMinutes: Int? = null
) {
    MUSLIM_WORLD_LEAGUE(18.0, 17.0),
    EGYPTIAN(19.5, 17.5),
    UMM_AL_QURA(18.5, 0.0, ishaIntervalMinutes = 90),
    KARACHI(18.0, 18.0),
    ISNA(15.0, 15.0),
    TEHRAN(17.7, 14.0, maghribAngle = 4.5),
    GULF(19.5, 0.0, ishaIntervalMinutes = 90),
    KUWAIT(18.0, 17.5),
    QATAR(18.0, 0.0, ishaIntervalMinutes = 90),
    SINGAPORE(20.0, 18.0),
    FRANCE(12.0, 12.0),
    TURKEY(18.0, 17.0),
    RUSSIA(16.0, 15.0),
    MOONSIGHTING_COMMITTEE(18.0, 18.0),
    CUSTOM(18.0, 17.0);

    fun getDisplayName(languageCode: String): String {
        return when (this) {
            MUSLIM_WORLD_LEAGUE -> when (languageCode) {
                "ar" -> "رابطة العالم الإسلامي"
                "fr" -> "Ligue Islamique Mondiale"
                else -> "Muslim World League"
            }
            EGYPTIAN -> when (languageCode) {
                "ar" -> "الهيئة المصرية العامة للمساحة"
                "fr" -> "Autorité Générale Égyptienne de l'Arpentage"
                else -> "Egyptian General Authority of Survey"
            }
            UMM_AL_QURA -> when (languageCode) {
                "ar" -> "جامعة أم القرى (مكة المكرمة)"
                "fr" -> "Université Oumm Al-Qura (La Mecque)"
                else -> "Umm Al-Qura University"
            }
            KARACHI -> when (languageCode) {
                "ar" -> "جامعة العلوم الإسلامية بكراتشي"
                "fr" -> "Université des Sciences Islamiques, Karachi"
                else -> "University of Islamic Sciences, Karachi"
            }
            ISNA -> when (languageCode) {
                "ar" -> "الجمعية الإسلامية لأمريكا الشمالية (ISNA)"
                "fr" -> "Société Islamique d'Amérique du Nord (ISNA)"
                else -> "ISNA (North America)"
            }
            TEHRAN -> when (languageCode) {
                "ar" -> "معهد الجيوفيزياء بجامعة طهران"
                "fr" -> "Institut de Géophysique, Université de Téhéran"
                else -> "Institute of Geophysics, University of Tehran"
            }
            GULF -> when (languageCode) {
                "ar" -> "منطقة الخليج"
                "fr" -> "Région du Golfe"
                else -> "Gulf Region"
            }
            KUWAIT -> when (languageCode) {
                "ar" -> "الكويت"
                "fr" -> "Koweït"
                else -> "Kuwait"
            }
            QATAR -> when (languageCode) {
                "ar" -> "قطر"
                "fr" -> "Qatar"
                else -> "Qatar"
            }
            SINGAPORE -> when (languageCode) {
                "ar" -> "سنغافورة (MUIS)"
                "fr" -> "Singapour (MUIS)"
                else -> "Singapore (MUIS)"
            }
            FRANCE -> when (languageCode) {
                "ar" -> "فرنسا (اتحاد المنظمات الإسلامية)"
                "fr" -> "France (UOIF)"
                else -> "France (UOIF)"
            }
            TURKEY -> when (languageCode) {
                "ar" -> "تركيا (رئاسة الشؤون الدينية)"
                "fr" -> "Turquie (Diyanet)"
                else -> "Turkey (Diyanet)"
            }
            RUSSIA -> when (languageCode) {
                "ar" -> "روسيا"
                "fr" -> "Russie"
                else -> "Russia"
            }
            MOONSIGHTING_COMMITTEE -> when (languageCode) {
                "ar" -> "لجنة رؤية الهلال"
                "fr" -> "Comité d'Observation du Croissant"
                else -> "Moonsighting Committee"
            }
            CUSTOM -> when (languageCode) {
                "ar" -> "مخصص (زوايا يدوية)"
                "fr" -> "Personnalisé (Angles manuels)"
                else -> "Custom (Manual angles)"
            }
        }
    }
}
