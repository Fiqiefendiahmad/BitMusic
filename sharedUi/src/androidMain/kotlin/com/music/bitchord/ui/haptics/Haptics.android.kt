package com.music.bitchord.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The phone's motor, with a short pattern per [Haptic]. */
actual class Haptics(private val vibrator: Vibrator?) {
    actual fun play(haptic: Haptic) {
        val motor = vibrator ?: return
        if (!motor.hasVibrator()) return
        motor.vibrate(effectFor(haptic))
    }

    private fun effectFor(haptic: Haptic): VibrationEffect = when (haptic) {
        Haptic.Tick -> VibrationEffect.createOneShot(8, 40)
        Haptic.Tap -> VibrationEffect.createOneShot(12, 80)
        Haptic.Select -> VibrationEffect.createOneShot(16, 120)
        Haptic.ToggleOn -> waveform(longArrayOf(0, 8, 12), intArrayOf(0, 40, 160))
        Haptic.ToggleOff -> waveform(longArrayOf(0, 12, 8), intArrayOf(0, 160, 40))
        Haptic.SkipNext -> waveform(longArrayOf(0, 8, 10, 14), intArrayOf(0, 60, 120, 200))
        Haptic.SkipPrevious -> waveform(longArrayOf(0, 14, 10, 8), intArrayOf(0, 200, 120, 60))
        Haptic.Resume -> waveform(longArrayOf(0, 10, 20), intArrayOf(0, 80, 200))
        Haptic.Pause -> waveform(longArrayOf(0, 16, 10), intArrayOf(0, 200, 60))
        Haptic.Expand -> waveform(longArrayOf(0, 8, 12), intArrayOf(0, 60, 120))
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect =
        VibrationEffect.createWaveform(timings, amplitudes, -1)
}

@Composable
actual fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        Haptics(vibrator)
    }
}
