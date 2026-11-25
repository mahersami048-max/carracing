package com.example.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Mesh(vertices: FloatArray) {
  companion object {
    const val FLOATS_PER_VERTEX = 11 // pos(3) + norm(3) + color(3) + uv(2)
    const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
  }

  val vertexCount: Int = vertices.size / FLOATS_PER_VERTEX
  private val buffer: FloatBuffer =
    ByteBuffer.allocateDirect(vertices.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply {
        put(vertices)
        position(0)
      }

  fun draw(shader: ShaderProgram) {
    buffer.position(0)
    GLES20.glVertexAttribPointer(
      shader.aPosition,
      3,
      GLES20.GL_FLOAT,
      false,
      STRIDE_BYTES,
      buffer,
    )
    GLES20.glEnableVertexAttribArray(shader.aPosition)

    buffer.position(3)
    GLES20.glVertexAttribPointer(
      shader.aNormal,
      3,
      GLES20.GL_FLOAT,
      false,
      STRIDE_BYTES,
      buffer,
    )
    GLES20.glEnableVertexAttribArray(shader.aNormal)

    buffer.position(6)
    GLES20.glVertexAttribPointer(
      shader.aColor,
      3,
      GLES20.GL_FLOAT,
      false,
      STRIDE_BYTES,
      buffer,
    )
    GLES20.glEnableVertexAttribArray(shader.aColor)

    buffer.position(9)
    GLES20.glVertexAttribPointer(
      shader.aUv,
      2,
      GLES20.GL_FLOAT,
      false,
      STRIDE_BYTES,
      buffer,
    )
    GLES20.glEnableVertexAttribArray(shader.aUv)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
  }
}

class MeshBuilder {
  private val data = ArrayList<Float>(1024)

  fun build(): Mesh = Mesh(data.toFloatArray())

  fun addVertex(
    x: Float,
    y: Float,
    z: Float,
    nx: Float,
    ny: Float,
    nz: Float,
    r: Float,
    g: Float,
    b: Float,
    u: Float = 0f,
    v: Float = 0f,
  ) {
    data.add(x)
    data.add(y)
    data.add(z)
    data.add(nx)
    data.add(ny)
    data.add(nz)
    data.add(r)
    data.add(g)
    data.add(b)
    data.add(u)
    data.add(v)
  }

  fun addTri(
    ax: Float,
    ay: Float,
    az: Float,
    bx: Float,
    by: Float,
    bz: Float,
    cx: Float,
    cy: Float,
    cz: Float,
    r: Float,
    g: Float,
    b: Float,
    u0: Float = 0f,
    v0: Float = 0f,
    u1: Float = 1f,
    v1: Float = 0f,
    u2: Float = 0.5f,
    v2: Float = 1f,
  ) {
    val ux = bx - ax
    val uy = by - ay
    val uz = bz - az
    val vx = cx - ax
    val vy = cy - ay
    val vz = cz - az
    var nx = uy * vz - uz * vy
    var ny = uz * vx - ux * vz
    var nz = ux * vy - uy * vx
    val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
    nx /= len
    ny /= len
    nz /= len
    addVertex(ax, ay, az, nx, ny, nz, r, g, b, u0, v0)
    addVertex(bx, by, bz, nx, ny, nz, r, g, b, u1, v1)
    addVertex(cx, cy, cz, nx, ny, nz, r, g, b, u2, v2)
  }

  fun addQuad(
    ax: Float,
    ay: Float,
    az: Float,
    bx: Float,
    by: Float,
    bz: Float,
    cx: Float,
    cy: Float,
    cz: Float,
    dx: Float,
    dy: Float,
    dz: Float,
    r: Float,
    g: Float,
    b: Float,
    uScale: Float = 1f,
    vScale: Float = 1f,
  ) {
    addTri(ax, ay, az, bx, by, bz, cx, cy, cz, r, g, b, 0f, 0f, uScale, 0f, uScale, vScale)
    addTri(ax, ay, az, cx, cy, cz, dx, dy, dz, r, g, b, 0f, 0f, uScale, vScale, 0f, vScale)
  }

  fun addBox(
    cx: Float,
    cy: Float,
    cz: Float,
    hx: Float,
    hy: Float,
    hz: Float,
    r: Float,
    g: Float,
    b: Float,
    topTaperX: Float = 1f,
    topTaperZ: Float = 1f,
    topOffsetZ: Float = 0f,
  ) {
    val x0 = cx - hx
    val x1 = cx + hx
    val y0 = cy - hy
    val y1 = cy + hy
    val z0 = cz - hz
    val z1 = cz + hz

    val tx0 = cx - hx * topTaperX
    val tx1 = cx + hx * topTaperX
    val tz0 = cz - hz * topTaperZ + topOffsetZ
    val tz1 = cz + hz * topTaperZ + topOffsetZ

    // Top
    addQuad(tx0, y1, tz0, tx1, y1, tz0, tx1, y1, tz1, tx0, y1, tz1, r, g, b)
    // Bottom
    addQuad(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, r * 0.7f, g * 0.7f, b * 0.7f)
    // Front (+Z)
    addQuad(x0, y0, z1, tx0, y1, tz1, tx1, y1, tz1, x1, y0, z1, r * 0.92f, g * 0.92f, b * 0.92f)
    // Back (-Z)
    addQuad(x1, y0, z0, tx1, y1, tz0, tx0, y1, tz0, x0, y0, z0, r * 0.88f, g * 0.88f, b * 0.88f)
    // Left (-X)
    addQuad(x0, y0, z0, tx0, y1, tz0, tx0, y1, tz1, x0, y0, z1, r * 0.95f, g * 0.95f, b * 0.95f)
    // Right (+X)
    addQuad(x1, y0, z1, tx1, y1, tz1, tx1, y1, tz0, x1, y0, z0, r * 0.95f, g * 0.95f, b * 0.95f)
  }

  /** Adds a wheel cylinder oriented along the X axis (axle along X). */
  fun addWheelCylinder(
    cx: Float,
    cy: Float,
    cz: Float,
    radius: Float,
    width: Float,
    segments: Int = 14,
    tireR: Float = 0.12f,
    tireG: Float = 0.13f,
    tireB: Float = 0.15f,
    rimR: Float = 0.78f,
    rimG: Float = 0.82f,
    rimB: Float = 0.88f,
  ) {
    val hw = width * 0.5f
    val xL = cx - hw
    val xR = cx + hw
    val rimRadius = radius * 0.62f

    for (i in 0 until segments) {
      val a0 = (2.0 * PI * i / segments).toFloat()
      val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
      val y0 = cy + cos(a0) * radius
      val z0 = cz + sin(a0) * radius
      val y1 = cy + cos(a1) * radius
      val z1 = cz + sin(a1) * radius

      // Tread
      addQuad(xL, y0, z0, xR, y0, z0, xR, y1, z1, xL, y1, z1, tireR, tireG, tireB)

      // Outer rim & tire sidewall (both sides)
      val ry0 = cy + cos(a0) * rimRadius
      val rz0 = cz + sin(a0) * rimRadius
      val ry1 = cy + cos(a1) * rimRadius
      val rz1 = cz + sin(a1) * rimRadius

      val spokeTint = if (i % 2 == 0) 1.0f else 0.45f
      // Right cap (+X)
      addTri(xR, cy, cz, xR, ry0, rz0, xR, ry1, rz1, rimR * spokeTint, rimG * spokeTint, rimB * spokeTint)
      addQuad(xR, ry0, rz0, xR, y0, z0, xR, y1, z1, xR, ry1, rz1, tireR, tireG, tireB)
      // Left cap (-X)
      addTri(xL, cy, cz, xL, ry1, rz1, xL, ry0, rz0, rimR * spokeTint, rimG * spokeTint, rimB * spokeTint)
      addQuad(xL, ry1, rz1, xL, y1, z1, xL, y0, z0, xL, ry0, rz0, tireR, tireG, tireB)
    }
  }

  /** Adds a vertical cylinder or cone along the Y axis. */
  fun addVerticalConeOrCylinder(
    cx: Float,
    baseY: Float,
    cz: Float,
    bottomRadius: Float,
    topRadius: Float,
    height: Float,
    segments: Int,
    r: Float,
    g: Float,
    b: Float,
  ) {
    val topY = baseY + height
    for (i in 0 until segments) {
      val a0 = (2.0 * PI * i / segments).toFloat()
      val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
      val bx0 = cx + cos(a0) * bottomRadius
      val bz0 = cz + sin(a0) * bottomRadius
      val bx1 = cx + cos(a1) * bottomRadius
      val bz1 = cz + sin(a1) * bottomRadius

      if (topRadius <= 0.001f) {
        addTri(bx0, baseY, bz0, bx1, baseY, bz1, cx, topY, cz, r, g, b)
      } else {
        val tx0 = cx + cos(a0) * topRadius
        val tz0 = cz + sin(a0) * topRadius
        val tx1 = cx + cos(a1) * topRadius
        val tz1 = cz + sin(a1) * topRadius
        addQuad(bx0, baseY, bz0, bx1, baseY, bz1, tx1, topY, tz1, tx0, topY, tz0, r, g, b)
      }
    }
  }
}
