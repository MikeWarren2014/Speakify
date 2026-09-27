package com.mikewarren.speakify.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val listener: OnShakeListener) : SensorEventListener {

    fun interface OnShakeListener {
        fun onShake()
    }

    constructor(onShake: () -> Unit) : this(OnShakeListener { onShake() })

    private var lastShakeTimestamp: Long = 0
    private var shakeCount: Int = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // gForce will be close to 1 when stationary
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()

            // Ignore shake events that are too close together (debounce 250ms)
            if (lastShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            // Reset shake count if window expired (1500ms)
            if (lastShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0
            }

            lastShakeTimestamp = now
            shakeCount++

            // Trigger shake callback on second shake in window
            if (shakeCount >= 2) {
                shakeCount = 0
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 250
        private const val SHAKE_COUNT_RESET_TIME_MS = 1500
    }
}
