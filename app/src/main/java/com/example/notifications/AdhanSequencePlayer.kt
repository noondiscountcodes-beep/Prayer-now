package com.example.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.example.data.AdhanPreferencesRepository
import com.example.data.PrayerAudioConfig
import com.example.engine.PrayerType
import com.example.media.MediaHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AdhanPlaybackStage {
    IDLE,
    PLAYING_ALERT,
    PLAYING_ADHAN,
    FINISHED
}

data class SequencePlaybackState(
    val stage: AdhanPlaybackStage = AdhanPlaybackStage.IDLE,
    val prayer: PrayerType = PrayerType.FAJR,
    val isPlaying: Boolean = false
)

object AdhanSequencePlayer {
    private const val TAG = "AdhanSequencePlayer"
    private var currentPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val _playbackState = MutableStateFlow(SequencePlaybackState())
    val playbackState: StateFlow<SequencePlaybackState> = _playbackState.asStateFlow()

    private fun acquireWakeLock(context: Context) {
        try {
            releaseWakeLock()
            val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MosqueClock:AdhanPlaybackWakeLock")?.apply {
                setReferenceCounted(false)
                acquire(15 * 60 * 1000L) // Keep CPU active for up to 15 minutes to guarantee full Adhan recitation
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        } finally {
            wakeLock = null
        }
    }

    fun playPrayerSequence(
        context: Context,
        prayer: PrayerType,
        onStageChanged: ((AdhanPlaybackStage) -> Unit)? = null,
        onAdhanFinished: (() -> Unit)? = null
    ) {
        stopAll()
        acquireWakeLock(context)

        val repo = AdhanPreferencesRepository(context)
        val alertConfig = repo.getAlertConfig(prayer)
        val audioConfig = repo.getAudioConfig(prayer)

        val hasValidAlert = alertConfig.isEnabled &&
                !alertConfig.uriString.isNullOrBlank() &&
                MediaHelper.isUriAvailable(context, alertConfig.uriString)

        if (hasValidAlert) {
            // Stage 1: Play Alert Sound at exact prayer time
            _playbackState.value = SequencePlaybackState(
                stage = AdhanPlaybackStage.PLAYING_ALERT,
                prayer = prayer,
                isPlaying = true
            )
            onStageChanged?.invoke(AdhanPlaybackStage.PLAYING_ALERT)

            playAudioUri(
                context = context,
                uriString = alertConfig.uriString,
                onComplete = {
                    // Alert finished! Start Adhan immediately with zero user intervention!
                    playAdhanStage(context, prayer, audioConfig, onStageChanged, onAdhanFinished)
                },
                onError = {
                    // Fallback to Adhan immediately if alert sound errors out
                    playAdhanStage(context, prayer, audioConfig, onStageChanged, onAdhanFinished)
                }
            )
        } else {
            // No alert configured or disabled: start Adhan immediately
            playAdhanStage(context, prayer, audioConfig, onStageChanged, onAdhanFinished)
        }
    }

    private fun playAdhanStage(
        context: Context,
        prayer: PrayerType,
        audioConfig: PrayerAudioConfig,
        onStageChanged: ((AdhanPlaybackStage) -> Unit)?,
        onAdhanFinished: (() -> Unit)?
    ) {
        if (!audioConfig.isEnabled) {
            releaseWakeLock()
            _playbackState.value = SequencePlaybackState(
                stage = AdhanPlaybackStage.FINISHED,
                prayer = prayer,
                isPlaying = false
            )
            onStageChanged?.invoke(AdhanPlaybackStage.FINISHED)
            onAdhanFinished?.invoke()
            return
        }

        _playbackState.value = SequencePlaybackState(
            stage = AdhanPlaybackStage.PLAYING_ADHAN,
            prayer = prayer,
            isPlaying = true
        )
        onStageChanged?.invoke(AdhanPlaybackStage.PLAYING_ADHAN)

        val adhanUri = audioConfig.uriString
        if (!adhanUri.isNullOrBlank() && MediaHelper.isUriAvailable(context, adhanUri)) {
            playAudioUri(
                context = context,
                uriString = adhanUri,
                onComplete = {
                    stopCurrentPlayer()
                    releaseWakeLock()
                    _playbackState.value = SequencePlaybackState(
                        stage = AdhanPlaybackStage.FINISHED,
                        prayer = prayer,
                        isPlaying = false
                    )
                    onStageChanged?.invoke(AdhanPlaybackStage.FINISHED)
                    onAdhanFinished?.invoke()
                },
                onError = {
                    stopCurrentPlayer()
                    releaseWakeLock()
                    _playbackState.value = SequencePlaybackState(
                        stage = AdhanPlaybackStage.FINISHED,
                        prayer = prayer,
                        isPlaying = false
                    )
                    onStageChanged?.invoke(AdhanPlaybackStage.FINISHED)
                    onAdhanFinished?.invoke()
                }
            )
        } else {
            // Play default alarm/notification tone if no custom file selected
            playDefaultTone(
                context = context,
                onComplete = {
                    stopCurrentPlayer()
                    releaseWakeLock()
                    _playbackState.value = SequencePlaybackState(
                        stage = AdhanPlaybackStage.FINISHED,
                        prayer = prayer,
                        isPlaying = false
                    )
                    onStageChanged?.invoke(AdhanPlaybackStage.FINISHED)
                    onAdhanFinished?.invoke()
                }
            )
        }
    }

    private fun playAudioUri(
        context: Context,
        uriString: String?,
        onComplete: () -> Unit,
        onError: () -> Unit
    ) {
        stopCurrentPlayer()
        if (uriString.isNullOrBlank()) {
            onError()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val player = MediaPlayer().apply {
                setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                if (uri.scheme == "file") {
                    setDataSource(uri.path ?: "")
                } else {
                    setDataSource(context, uri)
                }
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopCurrentPlayer()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopCurrentPlayer()
                    onError()
                    true
                }
                prepareAsync()
            }
            currentPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize player for uri $uriString: ${e.message}")
            stopCurrentPlayer()
            onError()
        }
    }

    private fun playDefaultTone(context: Context, onComplete: () -> Unit) {
        stopCurrentPlayer()
        try {
            val toneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val player = MediaPlayer().apply {
                setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(context, toneUri)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    stopCurrentPlayer()
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Default tone error: what=$what, extra=$extra")
                    stopCurrentPlayer()
                    onComplete()
                    true
                }
                prepareAsync()
            }
            currentPlayer = player
        } catch (e: Exception) {
            onComplete()
        }
    }

    fun stopAll() {
        stopCurrentPlayer()
        releaseWakeLock()
        _playbackState.value = SequencePlaybackState(
            stage = AdhanPlaybackStage.IDLE,
            isPlaying = false
        )
    }

    private fun stopCurrentPlayer() {
        try {
            currentPlayer?.stop()
            currentPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            currentPlayer = null
        }
    }
}
