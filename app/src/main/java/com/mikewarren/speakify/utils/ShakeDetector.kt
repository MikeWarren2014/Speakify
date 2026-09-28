package com.mikewarren.speakify.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeTimestamp: Long = 0
    private var shakeCount: Int = 0

    private var lastEventTimestampNs: Long = 0L
    private var isTrackingStroke: Boolean = false
    private var strokeDistanceMeters: Float = 0.0f
    private var strokeVelocityMetersPerSec: Float = 0.0f

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Calculate time delta dt in seconds
        val dt = getDeltaT(event)
        lastEventTimestampNs = event.timestamp

        val accelMagnitude = getAccelerationMagnitude(event)

        val gForce = accelMagnitude / SensorManager.GRAVITY_EARTH
        if (gForce in SHAKE_REST_GRAVITY..SHAKE_THRESHOLD_GRAVITY)
            return

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val linearAccel = (accelMagnitude - SensorManager.GRAVITY_EARTH).coerceAtLeast(0.0f)
            strokeDistanceMeters = computeStrokeDistanceFrom(linearAccel, dt)
            return
        }
        if (isTrackingStroke) {
            // Motion impulse has completed/decelerated back down below rest threshold
            isTrackingStroke = false

            // Check if estimated displacement reached the minimum distance threshold
            if (strokeDistanceMeters < MIN_SHAKE_DISTANCE_METERS) {
                Log.d("ShakeDetector", "Motion stroke ignored: distance=${strokeDistanceMeters}m (~${strokeDistanceMeters * FeetPerMeter} ft) < required ${MIN_SHAKE_DISTANCE_METERS}m (~0.167 ft)")
                return
            }

            val now = System.currentTimeMillis()

            // Ignore shake events that are too close together (debounce 250ms)
            if (now < lastShakeTimestamp + SHAKE_SLOP_TIME_MS) {
                return
            }

            handleValidShake()
        }
    }

    private fun getDeltaT(event: SensorEvent): Float {
        val currentNs = event.timestamp
        if (lastEventTimestampNs != 0L && currentNs > lastEventTimestampNs) {
            val deltaSec = (currentNs - lastEventTimestampNs) / 1_000_000_000.0f
            if (deltaSec <= 0.1f)
                return deltaSec
        }
        return 0.02f
    }

    private fun getAccelerationMagnitude(event: SensorEvent): Float {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        return sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    private fun computeStrokeDistanceFrom(linearAccel: Float, dt: Float): Float {
        if (!isTrackingStroke) {
            isTrackingStroke = true
            strokeDistanceMeters = 0.0f
            strokeVelocityMetersPerSec = 0.0f
        }

        // Integrate acceleration -> velocity -> distance
        strokeVelocityMetersPerSec += linearAccel * dt
        val stepDistance = strokeVelocityMetersPerSec * dt
        return strokeDistanceMeters + stepDistance
    }

    private fun handleValidShake() {
        val now = System.currentTimeMillis()

        // Reset shake count if window expired (1500ms)
        if (now > lastShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS) {
            shakeCount = 0
        }

        lastShakeTimestamp = now
        shakeCount++

        Log.d("ShakeDetector", "Valid shake stroke detected: distance=${strokeDistanceMeters}m (~${strokeDistanceMeters * FeetPerMeter} ft), shakeCount=$shakeCount")

        if (shakeCount >= 2) {
            shakeCount = 0
            Log.d("ShakeDetector", "Invoking onShake()")
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.5f
        private const val SHAKE_REST_GRAVITY = 1.8f
        private const val MIN_SHAKE_DISTANCE_METERS = 2f / 39.3701f
        private const val SHAKE_SLOP_TIME_MS = 250
        private const val SHAKE_COUNT_RESET_TIME_MS = 1500

        private const val FeetPerMeter = 3.28084f
    }
}
