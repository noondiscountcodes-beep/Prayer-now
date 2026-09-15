package com.example.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class SalawatAudioItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String
)

data class SalawatZipResult(
    val success: Boolean,
    val extractedCount: Int = 0,
    val ignoredCount: Int = 0,
    val totalAvailable: Int = 0,
    val errorMessage: String? = null
)

object SalawatZipManager {
    private const val TAG = "SalawatZipManager"

    private const val MAX_FILE_COUNT = 100
    private const val MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB
    private const val MAX_SINGLE_FILE_SIZE_BYTES = 20L * 1024 * 1024 // 20 MB
    private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "ogg", "m4a", "aac", "opus", "flac", "wma", "amr"
    )

    fun getSalawatAudioDirectory(context: Context): File {
        val dir = File(context.filesDir, "salawat_audios")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAudioFiles(context: Context): List<SalawatAudioItem> {
        val dir = getSalawatAudioDirectory(context)
        val files = dir.listFiles { file ->
            file.isFile && SUPPORTED_AUDIO_EXTENSIONS.contains(file.extension.lowercase())
        } ?: emptyArray()

        return files.sortedBy { it.name }.map { file ->
            SalawatAudioItem(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                formattedSize = MediaHelper.formatFileSize(file.length())
            )
        }
    }

    fun getRandomAudio(context: Context): File? {
        val dir = getSalawatAudioDirectory(context)
        val files = dir.listFiles { file ->
            file.isFile && SUPPORTED_AUDIO_EXTENSIONS.contains(file.extension.lowercase()) && file.length() > 0
        }
        if (files.isNullOrEmpty()) return null
        return files.random()
    }

    fun getAudioFileByName(context: Context, fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val dir = getSalawatAudioDirectory(context)
        val directTarget = File(dir, fileName)
        if (directTarget.exists() && directTarget.isFile && directTarget.length() > 0) {
            return directTarget
        }
        val files = dir.listFiles { file ->
            file.isFile && SUPPORTED_AUDIO_EXTENSIONS.contains(file.extension.lowercase()) && file.length() > 0
        } ?: return null
        return files.firstOrNull { it.name == fileName || it.name.endsWith("_$fileName") || it.name.endsWith(fileName) }
    }

    /**
     * Returns the 0-based index of the next audio that will play in sequential order.
     */
    fun getNextSequentialIndex(context: Context): Int {
        val items = getAudioFiles(context)
        if (items.isEmpty()) return -1
        val repo = com.example.data.SalawatPreferencesRepository(context)
        val lastIndex = repo.getLastAudioIndex()
        return if (lastIndex < 0 || lastIndex >= items.size - 1) 0 else lastIndex + 1
    }

    /**
     * Gets the next audio file in sequential order.
     * When advanceIndex is true, increments and saves the sequence pointer.
     */
    fun getNextSequentialAudio(context: Context, advanceIndex: Boolean = true): File? {
        val items = getAudioFiles(context)
        if (items.isEmpty()) return null
        val nextIndex = getNextSequentialIndex(context)
        if (nextIndex < 0 || nextIndex >= items.size) return items.first().file
        if (advanceIndex) {
            val repo = com.example.data.SalawatPreferencesRepository(context)
            repo.setLastAudioIndex(nextIndex)
        }
        return items[nextIndex].file
    }

    /**
     * Peeks at the next sequential audio file without advancing the index.
     */
    fun peekNextSequentialAudio(context: Context): File? {
        return getNextSequentialAudio(context, advanceIndex = false)
    }

    /**
     * Retrieves the effective audio based on the active mode:
     * - SEQUENTIAL (default): plays sounds in order, next one each time
     * - RANDOM: picks a random sound
     * - SPECIFIC: plays user's custom chosen sound
     */
    fun getEffectiveAudio(
        context: Context,
        config: com.example.data.SalawatConfig,
        advanceSequence: Boolean = true
    ): File? {
        if (config.audioSelectionMode == "SPECIFIC" && !config.selectedAudioFileName.isNullOrBlank()) {
            val specific = getAudioFileByName(context, config.selectedAudioFileName)
            if (specific != null && specific.exists() && specific.length() > 0) {
                return specific
            }
        }
        if (config.audioSelectionMode == "RANDOM") {
            return getRandomAudio(context)
        }
        // Default: SEQUENTIAL
        return getNextSequentialAudio(context, advanceIndex = advanceSequence)
    }

    fun deleteAudio(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio file: ${e.message}")
            false
        }
    }

    fun clearAllAudios(context: Context): Boolean {
        return try {
            val dir = getSalawatAudioDirectory(context)
            dir.deleteRecursively()
            dir.mkdirs()
            com.example.data.SalawatPreferencesRepository(context).resetLastAudioIndex()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all audios: ${e.message}")
            false
        }
    }

    fun extractSalawatZip(
        context: Context,
        zipUri: Uri,
        replaceExisting: Boolean = false
    ): SalawatZipResult {
        val outDir = getSalawatAudioDirectory(context)
        if (replaceExisting) {
            clearAllAudios(context)
        }
        val canonicalDestDirPath = outDir.canonicalPath

        var fileCount = 0
        var totalBytesExtracted = 0L
        var ignoredFiles = 0

        var inputStream: InputStream? = null
        var zipInputStream: ZipInputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(zipUri)
                ?: return SalawatZipResult(false, errorMessage = "تعذر فتح ملف الـ ZIP المختار")

            zipInputStream = ZipInputStream(inputStream)
            var entry: ZipEntry? = zipInputStream.nextEntry

            val currentExistingCount = getAudioFiles(context).size

            while (entry != null) {
                if (fileCount + currentExistingCount >= MAX_FILE_COUNT) {
                    Log.w(TAG, "Reached max allowed audio files ($MAX_FILE_COUNT)")
                    break
                }

                val entryName = entry.name
                val isDirectory = entry.isDirectory

                if (!isDirectory) {
                    val ext = entryName.substringAfterLast('.', "").lowercase()

                    if (SUPPORTED_AUDIO_EXTENSIONS.contains(ext)) {
                        val baseFileName = File(entryName).name
                        val safeFileName = "salawat_${System.currentTimeMillis()}_${fileCount}_$baseFileName"
                        val targetFile = File(outDir, safeFileName)

                        // Zip Slip Protection
                        val canonicalTargetFilePath = targetFile.canonicalPath
                        if (!canonicalTargetFilePath.startsWith(canonicalDestDirPath + File.separator)) {
                            Log.e(TAG, "Zip Slip detected: $entryName")
                            ignoredFiles++
                            zipInputStream.closeEntry()
                            entry = zipInputStream.nextEntry
                            continue
                        }

                        // Zip Bomb Protection
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
                                    Log.e(TAG, "Zip Bomb protection triggered! Audio size exceeded limit.")
                                    break
                                }
                                fos.write(buffer, 0, bytesRead)
                            }
                        }

                        if (isFileSafe && targetFile.length() > 0) {
                            fileCount++
                        } else {
                            targetFile.delete()
                            if (!isFileSafe) {
                                return SalawatZipResult(
                                    success = false,
                                    extractedCount = fileCount,
                                    ignoredCount = ignoredFiles,
                                    totalAvailable = getAudioFiles(context).size,
                                    errorMessage = "تم إيقاف فك الضغط: حجم الملفات تجاوز الحد الأقصى المسموح (100 ميجابايت)"
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

            val totalNow = getAudioFiles(context).size
            return SalawatZipResult(
                success = fileCount > 0 || totalNow > 0,
                extractedCount = fileCount,
                ignoredCount = ignoredFiles,
                totalAvailable = totalNow,
                errorMessage = if (fileCount == 0 && totalNow == 0) "لم يتم العثور على ملفات صوتية مدعومة داخل ملف الـ ZIP" else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Salawat zip: ${e.message}", e)
            return SalawatZipResult(
                success = false,
                extractedCount = fileCount,
                ignoredCount = ignoredFiles,
                totalAvailable = getAudioFiles(context).size,
                errorMessage = "خطأ أثناء فك ضغط ملف ZIP: ${e.localizedMessage}"
            )
        } finally {
            try { zipInputStream?.close() } catch (e: Exception) {}
            try { inputStream?.close() } catch (e: Exception) {}
        }
    }
}
