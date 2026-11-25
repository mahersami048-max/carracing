package com.example.input

class RaceInputState {
  @Volatile var steerLeft: Boolean = false
  @Volatile var steerRight: Boolean = false
  @Volatile var accelerate: Boolean = false
  @Volatile var brake: Boolean = false
  @Volatile var nitro: Boolean = false
  @Volatile var tiltSteer: Float = 0f

  fun reset() {
    steerLeft = false
    steerRight = false
    accelerate = false
    brake = false
    nitro = false
    tiltSteer = 0f
  }

  fun combinedSteer(): Float {
    val touchSteer =
      when {
        steerLeft && !steerRight -> -1f
        steerRight && !steerLeft -> 1f
        else -> 0f
      }
    return if (touchSteer != 0f) touchSteer else tiltSteer.coerceIn(-1f, 1f)
  }
}
