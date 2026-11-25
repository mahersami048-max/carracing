package com.example.data

import com.example.BuildConfig
import com.example.model.CarCatalog
import com.example.model.CloudSession
import com.example.model.LeaderboardEntry
import com.example.model.PlayerSaveData
import com.example.model.RaceResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class CloudAuthOutcome {
  data class Authenticated(val session: CloudSession) : CloudAuthOutcome()
  data class ConfirmationEmailSent(val message: String) : CloudAuthOutcome()
  data class Error(val message: String) : CloudAuthOutcome()
}

sealed class LeaderboardOutcome {
  data class Success(val entries: List<LeaderboardEntry>, val totalPlayers: Int) : LeaderboardOutcome()
  data class Error(val message: String) : LeaderboardOutcome()
}

class SupabaseClient {
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
  private val http =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()

  private val baseUrl: String
    get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

  private val anonKey: String
    get() = BuildConfig.SUPABASE_ANON_KEY.trim()

  val isConfigured: Boolean
    get() =
      baseUrl.startsWith("https://") &&
        anonKey.isNotBlank() &&
        anonKey != "MY_SUPABASE_ANON_KEY"

  suspend fun signUp(
    email: String,
    password: String,
    driverName: String,
  ): CloudAuthOutcome = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext CloudAuthOutcome.Error("Add SUPABASE_ANON_KEY in AI Studio Secrets first.")
    }
    val cleanEmail = email.trim()
    val cleanName = sanitizeDriverName(driverName)
    if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
      return@withContext CloudAuthOutcome.Error("Enter a valid email address.")
    }
    if (password.length < 6) {
      return@withContext CloudAuthOutcome.Error("Password must be at least 6 characters.")
    }
    if (cleanName.length < 2) {
      return@withContext CloudAuthOutcome.Error("Driver name must be at least 2 characters.")
    }

    val payload =
      JSONObject().apply {
        put("email", cleanEmail)
        put("password", password)
        put("data", JSONObject().apply { put("driver_name", cleanName) })
      }

    val req =
      Request.Builder()
        .url("$baseUrl/auth/v1/signup")
        .addHeader("apikey", anonKey)
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

    try {
      http.newCall(req).execute().use { resp ->
        val bodyStr = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext CloudAuthOutcome.Error(parseSupabaseError(bodyStr, "Sign up failed."))
        }
        val json = JSONObject(bodyStr)
        val accessToken = json.optString("access_token", "")
        val refreshToken = json.optString("refresh_token", "")
        val userObj = json.optJSONObject("user")
        val userId = userObj?.optString("id", "").orEmpty()
        val userEmail = userObj?.optString("email", cleanEmail).orEmpty().ifBlank { cleanEmail }

        if (accessToken.isNotBlank() && userId.isNotBlank()) {
          CloudAuthOutcome.Authenticated(
            CloudSession(
              userId = userId,
              email = userEmail,
              driverName = cleanName,
              accessToken = accessToken,
              refreshToken = refreshToken,
            )
          )
        } else {
          CloudAuthOutcome.ConfirmationEmailSent(
            "Account created! Verify your email link (or disable Confirm Email in Supabase Auth) and Sign In."
          )
        }
      }
    } catch (e: Exception) {
      CloudAuthOutcome.Error(e.message ?: "Network error during sign up.")
    }
  }

  suspend fun signIn(email: String, password: String): CloudAuthOutcome = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext CloudAuthOutcome.Error("Add SUPABASE_ANON_KEY in AI Studio Secrets first.")
    }
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || password.isBlank()) {
      return@withContext CloudAuthOutcome.Error("Email and password are required.")
    }

    val payload =
      JSONObject().apply {
        put("email", cleanEmail)
        put("password", password)
      }

    val req =
      Request.Builder()
        .url("$baseUrl/auth/v1/token?grant_type=password")
        .addHeader("apikey", anonKey)
        .addHeader("Content-Type", "application/json")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()

    try {
      http.newCall(req).execute().use { resp ->
        val bodyStr = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext CloudAuthOutcome.Error(parseSupabaseError(bodyStr, "Sign in failed."))
        }
        parseTokenResponse(bodyStr, fallbackEmail = cleanEmail)
      }
    } catch (e: Exception) {
      CloudAuthOutcome.Error(e.message ?: "Network error during sign in.")
    }
  }

  suspend fun refreshSession(refreshToken: String, currentEmail: String): CloudSession? =
    withContext(Dispatchers.IO) {
      if (!isConfigured || refreshToken.isBlank()) return@withContext null
      val payload = JSONObject().apply { put("refresh_token", refreshToken) }
      val req =
        Request.Builder()
          .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
          .addHeader("apikey", anonKey)
          .addHeader("Content-Type", "application/json")
          .post(payload.toString().toRequestBody(jsonMediaType))
          .build()
      try {
        http.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@withContext null
          val bodyStr = resp.body?.string().orEmpty()
          val outcome = parseTokenResponse(bodyStr, fallbackEmail = currentEmail)
          (outcome as? CloudAuthOutcome.Authenticated)?.session
        }
      } catch (_: Exception) {
        null
      }
    }

  suspend fun upsertPlayerProfile(
    session: CloudSession,
    saveData: PlayerSaveData,
  ): Result<CloudSession> = withContext(Dispatchers.IO) {
    if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

    val carName = CarCatalog.findById(saveData.selectedCarId).name
    val totalStars = saveData.levelStars.values.sum()
    val payload =
      JSONObject().apply {
        put("id", session.userId)
        put("driver_name", sanitizeDriverName(session.driverName))
        put("total_coins", saveData.coins)
        put("unlocked_career_level", saveData.unlockedCareerLevel)
        put("levels_passed", saveData.levelsPassedCount)
        put("total_stars", totalStars)
        put("selected_car", carName)
        put("best_endless_meters", saveData.bestEndlessMeters.toDouble())
        put("best_time_challenge_sec", saveData.bestTimeChallengeSeconds.toDouble())
        put("best_traffic_overtakes", saveData.bestTrafficOvertakes)
        put("highest_speed_kmh", saveData.highestSpeedKmh.toDouble())
        put("total_races_played", saveData.totalRacesPlayed)
      }

    var activeSession = session
    var respCode = executeUpsertProfile(activeSession.accessToken, payload)
    if (respCode == 401 && activeSession.refreshToken.isNotBlank()) {
      val refreshed = refreshSession(activeSession.refreshToken, activeSession.email)
      if (refreshed != null) {
        activeSession = refreshed
        respCode = executeUpsertProfile(activeSession.accessToken, payload)
      }
    }

    if (respCode in 200..299) {
      Result.success(activeSession)
    } else {
      Result.failure(IllegalStateException("Profile sync failed (HTTP $respCode)"))
    }
  }

  private fun executeUpsertProfile(accessToken: String, payload: JSONObject): Int {
    val req =
      Request.Builder()
        .url("$baseUrl/rest/v1/player_profiles?on_conflict=id")
        .addHeader("apikey", anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()
    return try {
      http.newCall(req).execute().use { it.code }
    } catch (_: Exception) {
      -1
    }
  }

  suspend fun insertRaceScore(
    session: CloudSession,
    carName: String,
    result: RaceResult,
  ): Result<CloudSession> = withContext(Dispatchers.IO) {
    if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase not configured"))

    val payload =
      JSONObject().apply {
        put("user_id", session.userId)
        put("driver_name", sanitizeDriverName(session.driverName))
        put("game_mode", result.mode.displayName)
        put("environment", result.environment.displayName)
        if (result.careerLevelNumber != null) {
          put("career_level", result.careerLevelNumber)
        } else {
          put("career_level", JSONObject.NULL)
        }
        put("is_victory", result.isVictory)
        put("distance_meters", result.distanceMeters.toDouble())
        put("max_speed_kmh", result.maxSpeedKmh.toDouble())
        put("overtakes", result.overtakes)
        put("coins_earned", result.coinsEarned)
        put("stars_earned", result.starsEarned)
        put("car_name", carName)
      }

    var activeSession = session
    var code = executeInsertScore(activeSession.accessToken, payload)
    if (code == 401 && activeSession.refreshToken.isNotBlank()) {
      val refreshed = refreshSession(activeSession.refreshToken, activeSession.email)
      if (refreshed != null) {
        activeSession = refreshed
        code = executeInsertScore(activeSession.accessToken, payload)
      }
    }

    if (code in 200..299) {
      Result.success(activeSession)
    } else {
      Result.failure(IllegalStateException("Score upload failed (HTTP $code)"))
    }
  }

  private fun executeInsertScore(accessToken: String, payload: JSONObject): Int {
    val req =
      Request.Builder()
        .url("$baseUrl/rest/v1/race_scores")
        .addHeader("apikey", anonKey)
        .addHeader("Authorization", "Bearer $accessToken")
        .addHeader("Content-Type", "application/json")
        .addHeader("Prefer", "return=minimal")
        .post(payload.toString().toRequestBody(jsonMediaType))
        .build()
    return try {
      http.newCall(req).execute().use { it.code }
    } catch (_: Exception) {
      -1
    }
  }

  suspend fun fetchLeaderboard(accessToken: String?): LeaderboardOutcome = withContext(Dispatchers.IO) {
    if (!isConfigured) {
      return@withContext LeaderboardOutcome.Error("Add SUPABASE_ANON_KEY in AI Studio Secrets to enable cloud leaderboard.")
    }

    val authBearer = accessToken?.takeIf { it.isNotBlank() } ?: anonKey
    val url =
      "$baseUrl/rest/v1/player_profiles?select=id,driver_name,levels_passed,unlocked_career_level,total_stars,best_endless_meters,highest_speed_kmh,total_coins,selected_car,total_races_played&order=levels_passed.desc,total_stars.desc,best_endless_meters.desc&limit=50"

    val req =
      Request.Builder()
        .url(url)
        .addHeader("apikey", anonKey)
        .addHeader("Authorization", "Bearer $authBearer")
        .addHeader("Prefer", "count=exact")
        .get()
        .build()

    try {
      http.newCall(req).execute().use { resp ->
        val bodyStr = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
          return@withContext LeaderboardOutcome.Error(
            parseSupabaseError(bodyStr, "Could not load leaderboard (HTTP ${resp.code}).")
          )
        }
        val arr = JSONArray(bodyStr)
        val entries = ArrayList<LeaderboardEntry>(arr.length())
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          entries.add(
            LeaderboardEntry(
              id = obj.optString("id", ""),
              driverName = obj.optString("driver_name", "Racer"),
              levelsPassed = obj.optInt("levels_passed", 0),
              unlockedCareerLevel = obj.optInt("unlocked_career_level", 1),
              totalStars = obj.optInt("total_stars", 0),
              bestEndlessMeters = obj.optDouble("best_endless_meters", 0.0).toFloat(),
              highestSpeedKmh = obj.optDouble("highest_speed_kmh", 0.0).toFloat(),
              totalCoins = obj.optInt("total_coins", 0),
              selectedCar = obj.optString("selected_car", "Apex GT"),
              totalRacesPlayed = obj.optInt("total_races_played", 0),
            )
          )
        }
        val contentRange = resp.header("Content-Range") ?: ""
        val total =
          contentRange.substringAfter("/", "").toIntOrNull() ?: entries.size
        LeaderboardOutcome.Success(entries = entries, totalPlayers = total)
      }
    } catch (e: Exception) {
      LeaderboardOutcome.Error(e.message ?: "Network error loading leaderboard.")
    }
  }

  private fun parseTokenResponse(bodyStr: String, fallbackEmail: String): CloudAuthOutcome {
    return try {
      val json = JSONObject(bodyStr)
      val accessToken = json.optString("access_token", "")
      val refreshToken = json.optString("refresh_token", "")
      val userObj = json.optJSONObject("user")
      val userId = userObj?.optString("id", "").orEmpty()
      val email = userObj?.optString("email", fallbackEmail).orEmpty().ifBlank { fallbackEmail }
      val meta = userObj?.optJSONObject("user_metadata")
      val driverName =
        meta?.optString("driver_name", "").orEmpty().ifBlank {
          email.substringBefore("@").ifBlank { "Racer" }
        }
      if (accessToken.isNotBlank() && userId.isNotBlank()) {
        CloudAuthOutcome.Authenticated(
          CloudSession(
            userId = userId,
            email = email,
            driverName = sanitizeDriverName(driverName),
            accessToken = accessToken,
            refreshToken = refreshToken,
          )
        )
      } else {
        CloudAuthOutcome.Error("Invalid auth session returned.")
      }
    } catch (_: Exception) {
      CloudAuthOutcome.Error("Invalid response from auth server.")
    }
  }

  private fun parseSupabaseError(bodyStr: String, fallback: String): String {
    return try {
      val obj = JSONObject(bodyStr)
      obj.optString("msg")
        .ifBlank { obj.optString("error_description") }
        .ifBlank { obj.optString("message") }
        .ifBlank { fallback }
    } catch (_: Exception) {
      fallback
    }
  }

  private fun sanitizeDriverName(raw: String): String {
    return raw.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").take(24)
  }
}
