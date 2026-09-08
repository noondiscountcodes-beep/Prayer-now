package com.example.media

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.AdhanPreferencesRepository
import com.example.engine.PrayerType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ZipExtractResult(
    val success: Boolean,
    val extractedFiles: List<File> = emptyList(),
    val ignoredCount: Int = 0,
    val errorMessage: String? = null
)

object AdhanZipManager {
    private const val TAG = "AdhanZipManager"

    // Security constraints
    private const val MAX_FILE_COUNT = 50
    private const val MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB
    private const val MAX_SINGLE_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10 MB
    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    fun getImagesDirectory(context: Context, prayer: PrayerType): File {
        val dir = File(context.filesDir, "adhan_images/${prayer.name.lowercase()}")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Retrieves extracted images for a prayer.
     * If no images exist specifically for this prayer, it gracefully falls back to any images
     * extracted for other prayers, ensuring the Adhan screen always has beautiful backgrounds.
     */
    fun getExtractedImages(context: Context, prayer: PrayerType): List<File> {
        val dir = getImagesDirectory(context, prayer)
        val files = dir.listFiles { file ->
            file.isFile && SUPPORTED_EXTENSIONS.contains(file.extension.lowercase())
        }
        if (!files.isNullOrEmpty()) {
            return files.sortedBy { it.name }
        }

        // Fallback: check other prayers
        for (otherPrayer in PrayerType.entries) {
            if (otherPrayer != prayer) {
                val otherDir = getImagesDirectory(context, otherPrayer)
                val otherFiles = otherDir.listFiles { file ->
                    file.isFile && SUPPORTED_EXTENSIONS.contains(file.extension.lowercase())
                }
                if (!otherFiles.isNullOrEmpty()) {
                    return otherFiles.sortedBy { it.name }
                }
            }
        }
        return emptyList()
    }

    fun deleteImagePack(context: Context, prayer: PrayerType): Boolean {
        val dir = getImagesDirectory(context, prayer)
        return try {
            dir.deleteRecursively()
            dir.mkdirs()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image pack for $prayer: ${e.message}")
            false
        }
    }

    /**
     * Replicates images from one prayer to all 5 obligatory prayers and updates their preferences.
     */
    fun copyImagesToAllPrayers(context: Context, sourcePrayer: PrayerType): Int {
        val sourceImages = getExtractedImages(context, sourcePrayer)
        if (sourceImages.isEmpty()) return 0

        val repo = AdhanPreferencesRepository(context)
        var updatedCount = 0

        for (targetPrayer in PrayerType.entries) {
            if (targetPrayer != sourcePrayer) {
                val targetDir = getImagesDirectory(context, targetPrayer)
                targetDir.mkdirs()
                targetDir.listFiles()?.forEach { it.delete() }

                sourceImages.forEach { src ->
                    val dest = File(targetDir, src.name)
                    src.copyTo(dest, overwrite = true)
                }

                val currentConfig = repo.getScreenConfig(targetPrayer)
                repo.setScreenConfig(
                    targetPrayer,
                    currentConfig.copy(selectedImageNames = sourceImages.map { it.name })
                )
                updatedCount++
            }
        }
        return updatedCount
    }

    fun extractZipFile(
        context: Context,
        zipUri: Uri,
        prayer: PrayerType,
        applyToAllIfEmpty: Boolean = true
    ): ZipExtractResult {
        val outDir = getImagesDirectory(context, prayer)
        val canonicalDestDirPath = outDir.canonicalPath

        var fileCount = 0
        var totalBytesExtracted = 0L
        var ignoredFiles = 0
        val extractedFiles = mutableListOf<File>()

        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(zipUri)
                ?: return ZipExtractResult(false, errorMessage = "تعذر فتح ملف الـ ZIP المختار")

            zipInputStream = ZipInputStream(inputStream)
            var entry: ZipEntry? = zipInputStream.nextEntry

            while (entry != null) {
                // 1. Check file count limit
                if (fileCount >= MAX_FILE_COUNT) {
                    Log.w(TAG, "Reached max allowed file count ($MAX_FILE_COUNT)")
                    break
                }

                val entryName = entry.name
                val isDirectory = entry.isDirectory

                if (!isDirectory) {
                    val ext = entryName.substringAfterLast('.', "").lowercase()

                    if (SUPPORTED_EXTENSIONS.contains(ext)) {
                        val safeFileName = File(entryName).name
                        val targetFile = File(outDir, "img_${System.currentTimeMillis()}_${fileCount}_$safeFileName")

                        // 2. Zip Slip / Path Traversal Security check
                        val canonicalTargetFilePath = targetFile.canonicalPath
                        if (!canonicalTargetFilePath.startsWith(canonicalDestDirPath + File.separator)) {
                            Log.e(TAG, "Zip Slip security attempt detected: $entryName")
                            ignoredFiles++
                            zipInputStream.closeEntry()
                            entry = zipInputStream.nextEntry
                            continue
                        }

                        // 3. Extract with size monitoring (Zip Bomb protection)
                        var singleFileBytes = 0L
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var isFileSafe = true

                        FileOutputStream(targetFile).use { fos ->
                            while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                                singleFileBytes += bytesRead
                                totalBytesExtracted += bytesRead

                                if (singleFileBytes > MAX_SINGLE_FILE_SIZE_BYTES ||
                                    totalBytesExtracted > MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES
                                ) {
                                    isFileSafe = false
                                    Log.e(TAG, "Zip Bomb protection triggered! File or total uncompressed size exceeded limit.")
                                    break
                                }
                                fos.write(buffer, 0, bytesRead)
                            }
                        }

                        if (isFileSafe && targetFile.length() > 0) {
                            fileCount++
                            extractedFiles.add(targetFile)
                        } else {
                            targetFile.delete()
                            if (!isFileSafe) {
                                return ZipExtractResult(
                                    success = false,
                                    extractedFiles = extractedFiles,
                                    ignoredCount = ignoredFiles,
                                    errorMessage = "تم إيقاف فك الضغط: حجم الملفات تجاوز الحد الأقصى المسموح للأمان (50 ميجابايت)"
                                )
                            }
                        }
                    } else {
                        ignoredFiles++
                    }
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

            if (extractedFiles.isEmpty()) {
                return ZipExtractResult(
                    success = false,
                    ignoredCount = ignoredFiles,
                    errorMessage = "لم يتم العثور على أي صور مدعومة (JPG, PNG, WEBP) داخل ملف الـ ZIP"
                )
            }

            // If other prayers currently have no images, automatically propagate to all prayers
            if (applyToAllIfEmpty) {
                val hasOtherImages = PrayerType.entries.any { p ->
                    if (p == prayer) false else {
                        val oDir = getImagesDirectory(context, p)
                        val oFiles = oDir.listFiles { f -> f.isFile && SUPPORTED_EXTENSIONS.contains(f.extension.lowercase()) }
                        !oFiles.isNullOrEmpty()
                    }
                }
                if (!hasOtherImages) {
                    copyImagesToAllPrayers(context, prayer)
                }
            }

            return ZipExtractResult(
                success = true,
                extractedFiles = extractedFiles,
                ignoredCount = ignoredFiles
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting zip: ${e.message}", e)
            return ZipExtractResult(
                success = false,
                errorMessage = "خطأ أثناء قراءة ملف الـ ZIP: ${e.localizedMessage ?: "تنسيق غير صالح"}"
            )
        } finally {
            try { zipInputStream?.close() } catch (e: Exception) {}
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }
}
