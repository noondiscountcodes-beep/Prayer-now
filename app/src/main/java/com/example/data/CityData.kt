package com.example.data

import com.example.localization.AppLanguage

data class City(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val nameFr: String,
    val countryEn: String,
    val countryAr: String,
    val countryFr: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String
) {
    fun getDisplayName(lang: AppLanguage): String {
        val cityName = when (lang) {
            AppLanguage.ARABIC -> nameAr
            AppLanguage.FRENCH -> nameFr
            else -> nameEn
        }
        val countryName = when (lang) {
            AppLanguage.ARABIC -> countryAr
            AppLanguage.FRENCH -> countryFr
            else -> countryEn
        }
        return "$cityName, $countryName"
    }

    fun getCityOnly(lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.ARABIC -> nameAr
            AppLanguage.FRENCH -> nameFr
            else -> nameEn
        }
    }
}

object CitiesRepository {
    val defaultCity = City(
        id = "makkah",
        nameEn = "Makkah",
        nameAr = "مكة المكرمة",
        nameFr = "La Mecque",
        countryEn = "Saudi Arabia",
        countryAr = "المملكة العربية السعودية",
        countryFr = "Arabie Saoudite",
        latitude = 21.4225,
        longitude = 39.8262,
        timezoneId = "Asia/Riyadh"
    )

    val cities: List<City> = listOf(
        defaultCity,
        City("madinah", "Madinah", "المدينة المنورة", "Médine", "Saudi Arabia", "السعودية", "Arabie Saoudite", 24.4672, 39.6111, "Asia/Riyadh"),
        City("riyadh", "Riyadh", "الرياض", "Riyad", "Saudi Arabia", "السعودية", "Arabie Saoudite", 24.7136, 46.6753, "Asia/Riyadh"),
        City("jeddah", "Jeddah", "جدة", "Djeddah", "Saudi Arabia", "السعودية", "Arabie Saoudite", 21.5433, 39.1728, "Asia/Riyadh"),
        City("cairo", "Cairo", "القاهرة", "Le Caire", "Egypt", "مصر", "Égypte", 30.0444, 31.2357, "Africa/Cairo"),
        City("alexandria", "Alexandria", "الإسكندرية", "Alexandrie", "Egypt", "مصر", "Égypte", 31.2001, 29.9187, "Africa/Cairo"),
        City("giza", "Giza", "الجيزة", "Gizeh", "Egypt", "مصر", "Égypte", 30.0131, 31.2089, "Africa/Cairo"),
        City("dubai", "Dubai", "دبي", "Dubaï", "United Arab Emirates", "الإمارات", "Émirats Arabes Unis", 25.2048, 55.2708, "Asia/Dubai"),
        City("abudhabi", "Abu Dhabi", "أبوظبي", "Abou Dabi", "United Arab Emirates", "الإمارات", "Émirats Arabes Unis", 24.4539, 54.3773, "Asia/Dubai"),
        City("doha", "Doha", "الدوحة", "Doha", "Qatar", "قطر", "Qatar", 25.2854, 51.5310, "Asia/Qatar"),
        City("kuwait", "Kuwait City", "مدينة الكويت", "Koweït", "Kuwait", "الكويت", "Koweït", 29.3759, 47.9774, "Asia/Kuwait"),
        City("manama", "Manama", "المنامة", "Manama", "Bahrain", "البحرين", "Bahreïn", 26.2285, 50.5860, "Asia/Bahrain"),
        City("muscat", "Muscat", "مسقط", "Mascate", "Oman", "عمان", "Oman", 23.5880, 58.3829, "Asia/Muscat"),
        City("amman", "Amman", "عمان", "Amman", "Jordan", "الأردن", "Jordanie", 31.9454, 35.9284, "Asia/Amman"),
        City("jerusalem", "Jerusalem", "القدس الشريف", "Jérusalem", "Palestine", "فلسطين", "Palestine", 31.7683, 35.2137, "Asia/Jerusalem"),
        City("gaza", "Gaza", "غزة", "Gaza", "Palestine", "فلسطين", "Palestine", 31.5017, 34.4668, "Asia/Gaza"),
        City("beirut", "Beirut", "بيروت", "Beyrouth", "Lebanon", "لبنان", "Liban", 33.8938, 35.5018, "Asia/Beirut"),
        City("damascus", "Damascus", "دمشق", "Damas", "Syria", "سوريا", "Syrie", 33.5138, 36.2765, "Asia/Damascus"),
        City("baghdad", "Baghdad", "بغداد", "Bagdad", "Iraq", "العراق", "Irak", 33.3152, 44.3661, "Asia/Baghdad"),
        City("basra", "Basra", "البصرة", "Bassora", "Iraq", "العراق", "Irak", 30.5081, 47.7835, "Asia/Baghdad"),
        City("casablanca", "Casablanca", "الدار البيضاء", "Casablanca", "Morocco", "المغرب", "Maroc", 33.5731, -7.5898, "Africa/Casablanca"),
        City("rabat", "Rabat", "الرباط", "Rabat", "Morocco", "المغرب", "Maroc", 34.0209, -6.8416, "Africa/Casablanca"),
        City("marrakech", "Marrakech", "مراكش", "Marrakech", "Morocco", "المغرب", "Maroc", 31.6295, -7.9811, "Africa/Casablanca"),
        City("fes", "Fes", "فاس", "Fès", "Morocco", "المغرب", "Maroc", 34.0181, -5.0078, "Africa/Casablanca"),
        City("tangier", "Tangier", "طنجة", "Tanger", "Morocco", "المغرب", "Maroc", 35.7595, -5.8340, "Africa/Casablanca"),
        City("algiers", "Algiers", "الجزائر", "Alger", "Algeria", "الجزائر", "Algérie", 36.7538, 3.0588, "Africa/Algiers"),
        City("oran", "Oran", "وهران", "Oran", "Algeria", "الجزائر", "Algérie", 35.6987, -0.6349, "Africa/Algiers"),
        City("constantine", "Constantine", "قسنطينة", "Constantine", "Algeria", "الجزائر", "Algérie", 36.3650, 6.6147, "Africa/Algiers"),
        City("tunis", "Tunis", "تونس", "Tunis", "Tunisia", "تونس", "Tunisie", 36.8065, 10.1815, "Africa/Tunis"),
        City("sfax", "Sfax", "صفاقس", "Sfax", "Tunisia", "تونس", "Tunisie", 34.7406, 10.7603, "Africa/Tunis"),
        City("tripoli", "Tripoli", "طرابلس", "Tripoli", "Libya", "ليبيا", "Libye", 32.8872, 13.1913, "Africa/Tripoli"),
        City("khartoum", "Khartoum", "الخرطوم", "Khartoum", "Sudan", "السودان", "Soudan", 15.5007, 32.5599, "Africa/Khartoum"),
        City("istanbul", "Istanbul", "إسطنبول", "Istanbul", "Turkey", "تركيا", "Turquie", 41.0082, 28.9784, "Europe/Istanbul"),
        City("ankara", "Ankara", "أنقرة", "Ankara", "Turkey", "تركيا", "Turquie", 39.9334, 32.8597, "Europe/Istanbul"),
        City("jakarta", "Jakarta", "جاكرتا", "Jakarta", "Indonesia", "إندونيسيا", "Indonésie", -6.2088, 106.8456, "Asia/Jakarta"),
        City("kualalumpur", "Kuala Lumpur", "كوالالمبور", "Kuala Lumpur", "Malaysia", "ماليزيا", "Malaisie", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        City("karachi", "Karachi", "كراتشي", "Karachi", "Pakistan", "باكستان", "Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
        City("lahore", "Lahore", "لاهور", "Lahore", "Pakistan", "باكستان", "Pakistan", 31.5204, 74.3587, "Asia/Karachi"),
        City("islamabad", "Islamabad", "إسلام آباد", "Islamabad", "Pakistan", "باكستان", "Pakistan", 33.6844, 73.0479, "Asia/Karachi"),
        City("dhaka", "Dhaka", "دكا", "Dacca", "Bangladesh", "بنغلاديش", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        City("london", "London", "لندن", "Londres", "United Kingdom", "بريطانيا", "Royaume-Uni", 51.5074, -0.1278, "Europe/London"),
        City("paris", "Paris", "باريس", "Paris", "France", "فرنسا", "France", 48.8566, 2.3522, "Europe/Paris"),
        City("marseille", "Marseille", "مارسيليا", "Marseille", "France", "فرنسا", "France", 43.2965, 5.3698, "Europe/Paris"),
        City("lyon", "Lyon", "ليون", "Lyon", "France", "فرنسا", "France", 45.7640, 4.8357, "Europe/Paris"),
        City("berlin", "Berlin", "برلين", "Berlin", "Germany", "ألمانيا", "Allemagne", 52.5200, 13.4050, "Europe/Berlin"),
        City("madrid", "Madrid", "مدريد", "Madrid", "Spain", "إسبانيا", "Espagne", 40.4168, -3.7038, "Europe/Madrid"),
        City("rome", "Rome", "روما", "Rome", "Italy", "إيطاليا", "Italie", 41.9028, 12.4964, "Europe/Rome"),
        City("newyork", "New York", "نيويورك", "New York", "United States", "أمريكا", "États-Unis", 40.7128, -74.0060, "America/New_York"),
        City("losangeles", "Los Angeles", "لوس أنجلوس", "Los Angeles", "United States", "أمريكا", "États-Unis", 34.0522, -118.2437, "America/Los_Angeles"),
        City("chicago", "Chicago", "شيكاغو", "Chicago", "United States", "أمريكا", "États-Unis", 41.8781, -87.6298, "America/Chicago"),
        City("toronto", "Toronto", "تورونتو", "Toronto", "Canada", "كندا", "Canada", 43.6532, -79.3832, "America/Toronto"),
        City("montreal", "Montreal", "مونتريال", "Montréal", "Canada", "كندا", "Canada", 45.5017, -73.5673, "America/Toronto"),
        City("sydney", "Sydney", "سيدني", "Sydney", "Australia", "أستراليا", "Australie", -33.8688, 151.2093, "Australia/Sydney"),
        City("moscow", "Moscow", "موسكو", "Moscou", "Russia", "روسيا", "Russie", 55.7558, 37.6173, "Europe/Moscow"),
        City("singapore", "Singapore", "سنغافورة", "Singapour", "Singapore", "سنغافورة", "Singapour", 1.3521, 103.8198, "Asia/Singapore")
    )

    fun search(query: String, lang: AppLanguage): List<City> {
        if (query.isBlank()) return cities
        val q = query.trim().lowercase()
        return cities.filter {
            it.nameEn.lowercase().contains(q) ||
            it.nameAr.contains(q) ||
            it.nameFr.lowercase().contains(q) ||
            it.countryEn.lowercase().contains(q) ||
            it.countryAr.contains(q) ||
            it.countryFr.lowercase().contains(q)
        }
    }
}
