package com.example.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class TiltSteeringManager(
  private val context: Context,
  private val inputState: RaceInputState,
) : SensorEventListener {
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private var isListening = false
  private var filteredSteer = 0f

  val isSupported: Boolean
    get() = accelerometer != null

  fun setEnabled(enabled: Boolean) {
    if (enabled && !isListening && accelerometer != null) {
      sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
      isListening = true
    } else if (!enabled && isListening) {
      sensorManager?.unregisterListener(this)
      isListening = false
      filteredSteer = 0f
      inputState.tiltSteer = 0f
    }
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
    val rotation =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display?.rotation ?: Surface.ROTATION_90
      } else {
        @Suppress("DEPRECATION")
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.rotation ?: Surface.ROTATION_90
      }

    // In landscape, Y-axis tilt controls left/right steering
    val rawAxis =
      when (rotation) {
        Surface.ROTATION_270 -> -event.values[1]
        Surface.ROTATION_0 -> -event.values[0]
        Surface.ROTATION_180 -> event.values[0]
        else -> event.values[1]
      }

    val deadZone = 0.65f
    val maxTilt = 4.5f
    val normalized =
      if (abs(rawAxis) < deadZone) {
        0f
      } else {
        val sign = if (rawAxis > 0f) 1f else -1f
        ((abs(rawAxis) - deadZone) / (maxTilt - deadZone)).coerceIn(0f, 1f) * sign
      }

    filteredSteer = filteredSteer * 0.72f + normalized * 0.28f
    inputState.tiltSteer = filteredSteer
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
