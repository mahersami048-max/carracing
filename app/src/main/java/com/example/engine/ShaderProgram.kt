package com.example.engine

import android.opengl.GLES20

class ShaderProgram {
  val programId: Int

  val aPosition: Int
  val aNormal: Int
  val aColor: Int
  val aUv: Int

  val uMvpMatrix: Int
  val uModelMatrix: Int
  val uCameraPos: Int
  val uLightDir: Int
  val uLightColor: Int
  val uAmbientColor: Int
  val uFogColor: Int
  val uFogDensity: Int
  val uEmissive: Int
  val uColorTint: Int
  val uUseTint: Int
  val uSpecularStrength: Int
  val uShininess: Int
  val uShaderMode: Int
  val uScrollOffset: Int
  val uWetness: Int
  val uTime: Int
  val uSkyTop: Int
  val uSkyHorizon: Int
  val uIsNight: Int
  val uPlayerPos: Int

  init {
    val vertexSrc =
      """
      attribute vec3 aPosition;
      attribute vec3 aNormal;
      attribute vec3 aColor;
      attribute vec2 aUv;

      uniform mat4 uMvpMatrix;
      uniform mat4 uModelMatrix;

      varying vec3 vWorldPos;
      varying vec3 vNormal;
      varying vec3 vColor;
      varying vec2 vUv;

      void main() {
        vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
        vWorldPos = worldPos.xyz;
        vNormal = normalize(mat3(uModelMatrix) * aNormal);
        vColor = aColor;
        vUv = aUv;
        gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
      }
      """
        .trimIndent()

    val fragmentSrc =
      """
      precision mediump float;

      varying vec3 vWorldPos;
      varying vec3 vNormal;
      varying vec3 vColor;
      varying vec2 vUv;

      uniform vec3 uCameraPos;
      uniform vec3 uLightDir;
      uniform vec3 uLightColor;
      uniform vec3 uAmbientColor;
      uniform vec3 uFogColor;
      uniform float uFogDensity;
      uniform vec3 uEmissive;
      uniform vec3 uColorTint;
      uniform float uUseTint;
      uniform float uSpecularStrength;
      uniform float uShininess;
      uniform int uShaderMode;
      uniform float uScrollOffset;
      uniform float uWetness;
      uniform float uTime;
      uniform vec3 uSkyTop;
      uniform vec3 uSkyHorizon;
      uniform float uIsNight;
      uniform vec3 uPlayerPos;

      float hash21(vec2 p) {
        p = fract(p * vec2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
      }

      void main() {
        if (uShaderMode == 2) {
          // Atmospheric Sky Dome with Sun/Moon & Drifting Clouds
          float h = clamp(vUv.y, 0.0, 1.0);
          vec3 sky = mix(uSkyHorizon, uSkyTop, pow(h, 0.62));
          vec3 rayDir = normalize(vWorldPos - uCameraPos);
          vec3 sunD = normalize(uLightDir);
          float sunDot = max(dot(rayDir, sunD), 0.0);
          float sunDisc = smoothstep(0.996, 0.9992, sunDot);
          float sunGlow = pow(sunDot, 14.0) * 0.45;
          vec3 orbColor = (uIsNight > 0.5) ? vec3(0.85, 0.92, 1.0) : vec3(1.0, 0.95, 0.80);
          sky += orbColor * (sunDisc * 1.4 + sunGlow);

          // Procedural clouds / stars
          if (uIsNight > 0.5) {
            vec2 starCell = floor(rayDir.xz / max(0.25, rayDir.y + 0.35) * 90.0);
            float starVal = hash21(starCell);
            if (starVal > 0.985 && h > 0.15) {
              sky += vec3(0.85, 0.92, 1.0) * ((starVal - 0.985) * 55.0);
            }
          } else if (h > 0.08 && h < 0.72) {
            vec2 cloudUv = rayDir.xz / (rayDir.y + 0.35) * 1.6 + vec2(uTime * 0.015, uTime * 0.025);
            float n1 = sin(cloudUv.x * 2.1 + cos(cloudUv.y * 1.7)) * 0.5 + 0.5;
            float n2 = cos(cloudUv.y * 3.4 - sin(cloudUv.x * 2.6)) * 0.5 + 0.5;
            float cloud = smoothstep(0.62, 0.88, n1 * n2 * 1.35);
            vec3 cloudCol = mix(uSkyHorizon, vec3(1.0, 0.99, 0.98), 0.75);
            sky = mix(sky, cloudCol, cloud * 0.45 * smoothstep(0.08, 0.25, h));
          }

          gl_FragColor = vec4(sky, 1.0);
          return;
        }

        vec3 N = normalize(vNormal);
        vec3 L = normalize(uLightDir);
        vec3 V = normalize(uCameraPos - vWorldPos);
        vec3 H = normalize(L + V);

        vec3 baseCol = mix(vColor, vColor * uColorTint, uUseTint);
        float specStrength = uSpecularStrength;
        float shininess = uShininess;
        vec3 extraGlow = uEmissive;

        if (uShaderMode == 1) {
          // Procedural 4-Lane Highway Road
          float roadX = vWorldPos.x; // -6.2 to +6.2
          float roadZ = vWorldPos.z + uScrollOffset;
          float grain = hash21(vec2(floor(roadX * 12.0), floor(roadZ * 4.0))) * 0.04;
          baseCol = baseCol + vec3(grain);

          // Lane dividers at x = -3.0, 0.0, +3.0
          float dashPhase = fract(roadZ * 0.11);
          float isDash = step(0.42, dashPhase);
          float lineCenter = 1.0 - smoothstep(0.05, 0.11, abs(roadX));
          float lineLeft = 1.0 - smoothstep(0.05, 0.11, abs(roadX + 3.0));
          float lineRight = 1.0 - smoothstep(0.05, 0.11, abs(roadX - 3.0));
          float laneMark = max(lineCenter, max(lineLeft, lineRight)) * isDash;
          baseCol = mix(baseCol, vec3(0.92, 0.94, 0.96), laneMark * 0.92);

          // Solid shoulder amber lines at x = +-5.75
          float edgeLine = 1.0 - smoothstep(0.06, 0.13, abs(abs(roadX) - 5.75));
          baseCol = mix(baseCol, vec3(0.96, 0.72, 0.12), edgeLine * 0.95);

          // Wet road specular reflection
          if (uWetness > 0.05) {
            float puddle = sin(roadZ * 0.22 + roadX * 0.8) * 0.5 + 0.5;
            specStrength += uWetness * (0.45 + 0.35 * puddle);
            shininess = mix(shininess, 54.0, uWetness);
            baseCol *= (1.0 - uWetness * 0.22);
          }

          // Dynamic Headlight beam pools ahead of player car (especially Night & Rainy)
          float dz = uPlayerPos.z - vWorldPos.z; // positive ahead of player (-Z direction)
          if (dz > 1.0 && dz < 36.0) {
            float beamHalfWidth = 1.3 + dz * 0.16;
            float latDist = abs(vWorldPos.x - uPlayerPos.x);
            float lateralMask = 1.0 - smoothstep(beamHalfWidth * 0.5, beamHalfWidth, latDist);
            float longMask = (1.0 - smoothstep(12.0, 36.0, dz)) * smoothstep(1.0, 4.0, dz);
            float headlightPower = (uIsNight > 0.5) ? 0.62 : (uWetness * 0.32);
            extraGlow += vec3(1.0, 0.96, 0.82) * lateralMask * longMask * headlightPower;
          }
        }

        float diff = max(dot(N, L), 0.0);
        float spec = pow(max(dot(N, H), 0.0), max(shininess, 2.0)) * specStrength;

        vec3 litColor = baseCol * (uAmbientColor + uLightColor * diff)
                      + uLightColor * spec
                      + extraGlow;

        // Exponential distance fog
        float dist = length(uCameraPos - vWorldPos);
        float fogFactor = clamp(1.0 - exp(-pow(dist * uFogDensity, 1.5)), 0.0, 1.0);
        vec3 finalCol = mix(litColor, uFogColor, fogFactor);

        gl_FragColor = vec4(finalCol, 1.0);
      }
      """
        .trimIndent()

    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
    programId =
      GLES20.glCreateProgram().also { prog ->
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
      }

    aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
    aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
    aColor = GLES20.glGetAttribLocation(programId, "aColor")
    aUv = GLES20.glGetAttribLocation(programId, "aUv")

    uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
    uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
    uCameraPos = GLES20.glGetUniformLocation(programId, "uCameraPos")
    uLightDir = GLES20.glGetUniformLocation(programId, "uLightDir")
    uLightColor = GLES20.glGetUniformLocation(programId, "uLightColor")
    uAmbientColor = GLES20.glGetUniformLocation(programId, "uAmbientColor")
    uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
    uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
    uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
    uColorTint = GLES20.glGetUniformLocation(programId, "uColorTint")
    uUseTint = GLES20.glGetUniformLocation(programId, "uUseTint")
    uSpecularStrength = GLES20.glGetUniformLocation(programId, "uSpecularStrength")
    uShininess = GLES20.glGetUniformLocation(programId, "uShininess")
    uShaderMode = GLES20.glGetUniformLocation(programId, "uShaderMode")
    uScrollOffset = GLES20.glGetUniformLocation(programId, "uScrollOffset")
    uWetness = GLES20.glGetUniformLocation(programId, "uWetness")
    uTime = GLES20.glGetUniformLocation(programId, "uTime")
    uSkyTop = GLES20.glGetUniformLocation(programId, "uSkyTop")
    uSkyHorizon = GLES20.glGetUniformLocation(programId, "uSkyHorizon")
    uIsNight = GLES20.glGetUniformLocation(programId, "uIsNight")
    uPlayerPos = GLES20.glGetUniformLocation(programId, "uPlayerPos")
  }

  private fun compileShader(type: Int, code: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
      GLES20.glShaderSource(shader, code)
      GLES20.glCompileShader(shader)
    }
  }

  fun use() {
    GLES20.glUseProgram(programId)
  }
}
