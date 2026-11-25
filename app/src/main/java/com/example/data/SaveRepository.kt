package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CarCatalog
import com.example.model.CarUpgradeState
import com.example.model.CloudSession
import com.example.model.GraphicsQuality
import com.example.model.PlayerSaveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.saveDataStore: DataStore<Preferences> by preferencesDataStore(name = "highway_rush_save")

class SaveRepository(private val context: Context) {
  private val saveJsonKey = stringPreferencesKey("player_save_json_v1")

  val saveDataFlow: Flow<PlayerSaveData> =
    context.saveDataStore.data.map { prefs ->
      val raw = prefs[saveJsonKey]
      if (raw.isNullOrBlank()) {
        PlayerSaveData()
      } else {
        decodeSaveData(raw)
      }
    }

  suspend fun updateSave(transform: (PlayerSaveData) -> PlayerSaveData) {
    context.saveDataStore.edit { prefs ->
      val current =
        prefs[saveJsonKey]?.takeIf { it.isNotBlank() }?.let(::decodeSaveData) ?: PlayerSaveData()
      val updated = transform(current)
      prefs[saveJsonKey] = encodeSaveData(updated)
    }
  }

  suspend fun resetProgress() {
    context.saveDataStore.edit { prefs ->
      val current =
        prefs[saveJsonKey]?.takeIf { it.isNotBlank() }?.let(::decodeSaveData) ?: PlayerSaveData()
      val fresh =
        PlayerSaveData(
          soundEnabled = current.soundEnabled,
          musicEnabled = current.musicEnabled,
          tiltSteeringEnabled = current.tiltSteeringEnabled,
          graphicsQuality = current.graphicsQuality,
          cloudSession = current.cloudSession,
        )
      prefs[saveJsonKey] = encodeSaveData(fresh)
    }
  }

  private fun encodeSaveData(data: PlayerSaveData): String {
    val root = JSONObject()
    root.put("coins", data.coins)
    root.put("selectedCarId", data.selectedCarId)
    root.put("unlockedCarIds", JSONArray(data.unlockedCarIds.toList()))

    val upgradesObj = JSONObject()
    data.carUpgrades.forEach { (carId, state) ->
      val item =
        JSONObject().apply {
          put("engine", state.engineLevel)
          put("handling", state.handlingLevel)
          put("nitro", state.nitroLevel)
          put("color", state.colorHex)
        }
      upgradesObj.put(carId, item)
    }
    root.put("carUpgrades", upgradesObj)

    root.put("unlockedCareerLevel", data.unlockedCareerLevel)
    val starsObj = JSONObject()
    data.levelStars.forEach { (lvl, stars) -> starsObj.put(lvl.toString(), stars) }
    root.put("levelStars", starsObj)

    root.put("bestEndlessMeters", data.bestEndlessMeters.toDouble())
    root.put("bestTimeChallengeSeconds", data.bestTimeChallengeSeconds.toDouble())
    root.put("bestTrafficOvertakes", data.bestTrafficOvertakes)
    root.put("highestSpeedKmh", data.highestSpeedKmh.toDouble())
    root.put("totalRacesPlayed", data.totalRacesPlayed)
    root.put("soundEnabled", data.soundEnabled)
    root.put("musicEnabled", data.musicEnabled)
    root.put("tiltSteeringEnabled", data.tiltSteeringEnabled)
    root.put("graphicsQuality", data.graphicsQuality.name)

    data.cloudSession?.let { session ->
      val sessObj =
        JSONObject().apply {
          put("userId", session.userId)
          put("email", session.email)
          put("driverName", session.driverName)
          put("accessToken", session.accessToken)
          put("refreshToken", session.refreshToken)
        }
      root.put("cloudSession", sessObj)
    }

    return root.toString()
  }

  private fun decodeSaveData(json: String): PlayerSaveData {
    return try {
      val root = JSONObject(json)
      val unlockedCars = mutableSetOf("apex_gt")
      root.optJSONArray("unlockedCarIds")?.let { arr ->
        for (i in 0 until arr.length()) {
          unlockedCars.add(arr.getString(i))
        }
      }

      val upgrades =
        CarCatalog.cars
          .associate { it.id to CarUpgradeState(colorHex = it.defaultColorHex) }
          .toMutableMap()
      root.optJSONObject("carUpgrades")?.let { obj ->
        val keys = obj.keys()
        while (keys.hasNext()) {
          val carId = keys.next()
          val item = obj.optJSONObject(carId) ?: continue
          upgrades[carId] =
            CarUpgradeState(
              engineLevel = item.optInt("engine", 1).coerceIn(1, 5),
              handlingLevel = item.optInt("handling", 1).coerceIn(1, 5),
              nitroLevel = item.optInt("nitro", 1).coerceIn(1, 5),
              colorHex = item.optInt("color", CarCatalog.findById(carId).defaultColorHex),
            )
        }
      }

      val stars = mutableMapOf<Int, Int>()
      root.optJSONObject("levelStars")?.let { obj ->
        val keys = obj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          k.toIntOrNull()?.let { lvl -> stars[lvl] = obj.optInt(k, 0).coerceIn(0, 3) }
        }
      }

      val qualityName = root.optString("graphicsQuality", GraphicsQuality.HIGH.name)
      val quality =
        GraphicsQuality.entries.firstOrNull { it.name == qualityName } ?: GraphicsQuality.HIGH

      val session =
        root.optJSONObject("cloudSession")?.let { s ->
          val uid = s.optString("userId", "")
          val tok = s.optString("accessToken", "")
          if (uid.isNotBlank() && tok.isNotBlank()) {
            CloudSession(
              userId = uid,
              email = s.optString("email", ""),
              driverName = s.optString("driverName", "Racer"),
              accessToken = tok,
              refreshToken = s.optString("refreshToken", ""),
            )
          } else {
            null
          }
        }

      PlayerSaveData(
        coins = root.optInt("coins", 500).coerceAtLeast(0),
        selectedCarId = root.optString("selectedCarId", "apex_gt"),
        unlockedCarIds = unlockedCars,
        carUpgrades = upgrades,
        unlockedCareerLevel = root.optInt("unlockedCareerLevel", 1).coerceIn(1, 24),
        levelStars = stars,
        bestEndlessMeters = root.optDouble("bestEndlessMeters", 0.0).toFloat(),
        bestTimeChallengeSeconds = root.optDouble("bestTimeChallengeSeconds", 0.0).toFloat(),
        bestTrafficOvertakes = root.optInt("bestTrafficOvertakes", 0),
        highestSpeedKmh = root.optDouble("highestSpeedKmh", 0.0).toFloat(),
        totalRacesPlayed = root.optInt("totalRacesPlayed", 0).coerceAtLeast(0),
        soundEnabled = root.optBoolean("soundEnabled", true),
        musicEnabled = root.optBoolean("musicEnabled", true),
        tiltSteeringEnabled = root.optBoolean("tiltSteeringEnabled", false),
        graphicsQuality = quality,
        cloudSession = session,
      )
    } catch (_: Exception) {
      PlayerSaveData()
    }
  }
}
