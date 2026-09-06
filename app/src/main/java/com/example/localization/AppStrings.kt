package com.example.localization

import com.example.engine.PrayerType

object AppStrings {

    fun getPrayerName(type: PrayerType, lang: AppLanguage): String {
        return when (type) {
            PrayerType.FAJR -> when (lang) {
                AppLanguage.ARABIC -> "الفجر"
                AppLanguage.FRENCH -> "Fajr"
                AppLanguage.ENGLISH -> "Fajr"
            }
            PrayerType.SUNRISE -> when (lang) {
                AppLanguage.ARABIC -> "الشروق"
                AppLanguage.FRENCH -> "Chourouk"
                AppLanguage.ENGLISH -> "Sunrise"
            }
            PrayerType.DHUHR -> when (lang) {
                AppLanguage.ARABIC -> "الظهر"
                AppLanguage.FRENCH -> "Dhohr"
                AppLanguage.ENGLISH -> "Dhuhr"
            }
            PrayerType.ASR -> when (lang) {
                AppLanguage.ARABIC -> "العصر"
                AppLanguage.FRENCH -> "Asr"
                AppLanguage.ENGLISH -> "Asr"
            }
            PrayerType.MAGHRIB -> when (lang) {
                AppLanguage.ARABIC -> "المغرب"
                AppLanguage.FRENCH -> "Maghrib"
                AppLanguage.ENGLISH -> "Maghrib"
            }
            PrayerType.ISHA -> when (lang) {
                AppLanguage.ARABIC -> "العشاء"
                AppLanguage.FRENCH -> "Icha"
                AppLanguage.ENGLISH -> "Isha"
            }
        }
    }

    fun currentPrayer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "الصلاة الحالية"
        AppLanguage.FRENCH -> "Prière actuelle"
        AppLanguage.ENGLISH -> "Current Prayer"
    }

    fun nextPrayer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "الصلاة القادمة"
        AppLanguage.FRENCH -> "Prochaine prière"
        AppLanguage.ENGLISH -> "Next Prayer"
    }

    fun remainingTime(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "الوقت المتبقي"
        AppLanguage.FRENCH -> "Temps restant"
        AppLanguage.ENGLISH -> "Time Remaining"
    }

    fun remaining(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "متبقي"
        AppLanguage.FRENCH -> "Restant"
        AppLanguage.ENGLISH -> "Remaining"
    }

    fun menu(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "القائمة"
        AppLanguage.FRENCH -> "Menu"
        AppLanguage.ENGLISH -> "Menu"
    }

    // Tabs
    fun tabLanguage(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "اللغة"
        AppLanguage.FRENCH -> "Langue"
        AppLanguage.ENGLISH -> "Language"
    }

    fun tabLocation(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "المكان والحساب"
        AppLanguage.FRENCH -> "Lieu & Calcul"
        AppLanguage.ENGLISH -> "Location & Method"
    }

    fun tabAlerts(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "التنبيهات"
        AppLanguage.FRENCH -> "Rappels"
        AppLanguage.ENGLISH -> "Alerts"
    }

    fun tabAdhan(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "الأذان"
        AppLanguage.FRENCH -> "Adhan"
        AppLanguage.ENGLISH -> "Adhan"
    }

    fun tabRamadan(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "رمضان"
        AppLanguage.FRENCH -> "Ramadan"
        AppLanguage.ENGLISH -> "Ramadan"
    }

    fun tabNotifications(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "شريط الإشعارات"
        AppLanguage.FRENCH -> "Barre d'état"
        AppLanguage.ENGLISH -> "Notification Bar"
    }

    fun tabSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "الإعدادات والأمان"
        AppLanguage.FRENCH -> "Sécurité & Paramètres"
        AppLanguage.ENGLISH -> "Settings & Privacy"
    }

    // Common Actions
    fun save(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "حفظ"
        AppLanguage.FRENCH -> "Enregistrer"
        AppLanguage.ENGLISH -> "Save"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "إلغاء"
        AppLanguage.FRENCH -> "Annuler"
        AppLanguage.ENGLISH -> "Cancel"
    }

    fun delete(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "حذف"
        AppLanguage.FRENCH -> "Supprimer"
        AppLanguage.ENGLISH -> "Delete"
    }

    fun add(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "إضافة"
        AppLanguage.FRENCH -> "Ajouter"
        AppLanguage.ENGLISH -> "Add"
    }

    fun edit(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "تعديل"
        AppLanguage.FRENCH -> "Modifier"
        AppLanguage.ENGLISH -> "Edit"
    }

    fun copy(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "نسخ"
        AppLanguage.FRENCH -> "Dupliquer"
        AppLanguage.ENGLISH -> "Duplicate"
    }

    fun preview(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "معاينة"
        AppLanguage.FRENCH -> "Aperçu"
        AppLanguage.ENGLISH -> "Preview"
    }

    fun replace(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "استبدال"
        AppLanguage.FRENCH -> "Remplacer"
        AppLanguage.ENGLISH -> "Replace"
    }

    fun close(lang: AppLanguage): String = when (lang) {
        AppLanguage.ARABIC -> "إغلاق"
        AppLanguage.FRENCH -> "Fermer"
        AppLanguage.ENGLISH -> "Close"
    }
}
