package com.hanziwriter.app.domain.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool
    private val negative2ShortId: Int
    private val positiveShortId: Int
    private val positiveId: Int

    private val vibrator: Vibrator
    private val audioManager: AudioManager

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        negative2ShortId = try {
            soundPool.load(context.assets.openFd("sounds/gui/negative_2_short.ogg"), 1)
        } catch (_: Exception) { 0 }
        positiveShortId = try {
            soundPool.load(context.assets.openFd("sounds/gui/positive_short.ogg"), 1)
        } catch (_: Exception) { 0 }
        positiveId = try {
            soundPool.load(context.assets.openFd("sounds/gui/positive.ogg"), 1)
        } catch (_: Exception) { 0 }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val isSoundEnabled: Boolean
        get() = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL

    fun playMistakeSound() {
        if (isSoundEnabled) {
            soundPool.play(negative2ShortId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playCharacterCompleteSound() {
        if (isSoundEnabled) {
            soundPool.play(positiveShortId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playLessonCompleteSound() {
        val inSilent = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
        if (inSilent) return
        if (isSoundEnabled) {
            soundPool.play(positiveId, 1f, 1f, 1, 0, 1f)
        }
        vibrateLong()
    }

    fun vibrate() {
        val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun vibrateLong() {
        val effect = VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun release() {
        soundPool.release()
    }
}
