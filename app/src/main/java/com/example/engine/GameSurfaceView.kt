package com.example.engine

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GameSurfaceView(
  renderer: RaceGlRenderer,
  allowTurntableDrag: Boolean,
  modifier: Modifier = Modifier,
) {
  AndroidView(
    modifier = modifier,
    factory = { context ->
      var lastTouchX = 0f
      object : GLSurfaceView(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
          if (!allowTurntableDrag || renderer.renderMode != RaceGlRenderer.RenderMode.SHOWROOM) {
            return false
          }
          when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
              lastTouchX = event.x
              return true
            }
            MotionEvent.ACTION_MOVE -> {
              val dx = event.x - lastTouchX
              lastTouchX = event.x
              renderer.showroomYawDeg = (renderer.showroomYawDeg + dx * 0.45f) % 360f
              return true
            }
          }
          return false
        }
      }.apply {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
      }
    },
    update = { view ->
      // Keep render mode active; drag handled via renderer properties
      if (view.renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
        view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
      }
    },
  )
}
