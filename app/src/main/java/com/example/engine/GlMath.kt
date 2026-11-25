package com.example.engine

import android.opengl.Matrix
import kotlin.math.sqrt

object GlMath {
  fun setIdentity(m: FloatArray) {
    Matrix.setIdentityM(m, 0)
  }

  fun perspective(m: FloatArray, fovDeg: Float, aspect: Float, near: Float, far: Float) {
    Matrix.perspectiveM(m, 0, fovDeg, aspect, near, far)
  }

  fun lookAt(
    m: FloatArray,
    eyeX: Float,
    eyeY: Float,
    eyeZ: Float,
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    upX: Float,
    upY: Float,
    upZ: Float,
  ) {
    Matrix.setLookAtM(m, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
  }

  fun multiply(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
    Matrix.multiplyMM(result, 0, lhs, 0, rhs, 0)
  }

  fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
    Matrix.translateM(m, 0, x, y, z)
  }

  fun rotateX(m: FloatArray, deg: Float) {
    if (deg != 0f) Matrix.rotateM(m, 0, deg, 1f, 0f, 0f)
  }

  fun rotateY(m: FloatArray, deg: Float) {
    if (deg != 0f) Matrix.rotateM(m, 0, deg, 0f, 1f, 0f)
  }

  fun rotateZ(m: FloatArray, deg: Float) {
    if (deg != 0f) Matrix.rotateM(m, 0, deg, 0f, 0f, 1f)
  }

  fun scale(m: FloatArray, x: Float, y: Float, z: Float) {
    Matrix.scaleM(m, 0, x, y, z)
  }

  fun normalize(v: FloatArray): FloatArray {
    val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).coerceAtLeast(0.0001f)
    return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
  }
}
