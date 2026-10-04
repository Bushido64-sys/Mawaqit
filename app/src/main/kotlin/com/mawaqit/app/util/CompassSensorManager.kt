package com.mawaqit.app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Device heading (azimuth, 0–360° clockwise from north) as a Flow.
 * Prefers TYPE_ROTATION_VECTOR; falls back to ACCELEROMETER + MAGNETIC_FIELD.
 * Emits only when the heading changes by ≥1° (battery + jitter).
 */
@Singleton
class CompassSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getHeadingFlow(): Flow<Float> = callbackFlow {
        val usesFallback = true // always use accel+magnetic: reliable on every device

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                val azimuth: Float = if (usesFallback) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            computeFallbackAzimuth()
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            computeFallbackAzimuth()
                        }
                        else -> Float.NaN
                    }
                if (!azimuth.isNaN()) {
                    val normalized = ((azimuth % 360f) + 360f) % 360f
                    trySend(normalized)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            private fun computeFallbackAzimuth(): Float {
                val r = FloatArray(9)
                val i = FloatArray(9)
                return if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                    SensorManager.getOrientation(r, orientation)
                    Math.toDegrees(orientation[0].toDouble()).toFloat()
                } else Float.NaN
            }
        }

        val delay = SensorManager.SENSOR_DELAY_GAME
        if (usesFallback) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                ?.let { sensorManager.registerListener(listener, it, delay) }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                ?.let { sensorManager.registerListener(listener, it, delay) }
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }.distinctUntilChanged { a, b -> abs(a - b) < 1f }
}
