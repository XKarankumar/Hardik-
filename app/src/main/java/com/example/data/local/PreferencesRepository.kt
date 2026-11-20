package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettingsState
import com.example.domain.model.HomeTheatreState
import com.example.domain.model.MovieSoundProfile
import com.example.domain.model.RoomSimulation
import com.example.domain.model.SoundStudioState
import com.example.domain.model.SpatialMode
import com.example.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hardikDataStore: DataStore<Preferences> by preferencesDataStore(name = "hardik_buds_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIGH_REFRESH = booleanPreferencesKey("high_refresh_motion")
        val AUTO_POPUP = booleanPreferencesKey("auto_connection_popup")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val GITHUB_REPO = stringPreferencesKey("github_repo")

        val EQ_BANDS = stringPreferencesKey("eq_bands_csv")
        val ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        val BASS_BOOST = floatPreferencesKey("bass_boost")
        val TREBLE_BOOST = floatPreferencesKey("treble_boost")
        val VOCAL_ENHANCER = floatPreferencesKey("vocal_enhancer")
        val CLARITY_ENHANCER = floatPreferencesKey("clarity_enhancer")
        val BALANCE = floatPreferencesKey("balance")
        val MASTER_BYPASS = booleanPreferencesKey("master_bypass")

        val SPATIAL_MODE = stringPreferencesKey("spatial_mode")
        val ROOM_SIM = stringPreferencesKey("room_simulation")
        val MOVIE_PROFILE = stringPreferencesKey("movie_profile")
        val CINEMA_MODE = booleanPreferencesKey("cinema_mode")
        val VIRTUAL_SUB = floatPreferencesKey("virtual_sub")
        val DEEP_BASS = floatPreferencesKey("deep_bass")
        val DIALOGUE_ENHANCE = floatPreferencesKey("dialogue_enhance")
        val STEREO_WIDTH = floatPreferencesKey("stereo_width")
        val THEATRE_BASS_DB = floatPreferencesKey("theatre_bass_db")
        val THEATRE_TREBLE_DB = floatPreferencesKey("theatre_treble_db")
    }

    val appSettings: Flow<AppSettingsState> = context.hardikDataStore.data.map { prefs ->
        AppSettingsState(
            themePreference = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrNull()
            } ?: ThemePreference.DARK,
            highRefreshMotion = prefs[Keys.HIGH_REFRESH] ?: true,
            autoShowConnectionPopup = prefs[Keys.AUTO_POPUP] ?: true,
            githubOwner = prefs[Keys.GITHUB_OWNER] ?: "hardik-buds",
            githubRepo = prefs[Keys.GITHUB_REPO] ?: "hardik-buds-android",
        )
    }

    val soundStudioSettings: Flow<SoundStudioState> = context.hardikDataStore.data.map { prefs ->
        val bands = prefs[Keys.EQ_BANDS]?.split(",")
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == 10 }
            ?: List(10) { 0f }
        SoundStudioState(
            eqBandsDb = bands,
            activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: "flat",
            bassBoost = prefs[Keys.BASS_BOOST] ?: 0f,
            trebleBoost = prefs[Keys.TREBLE_BOOST] ?: 0f,
            vocalEnhancer = prefs[Keys.VOCAL_ENHANCER] ?: 0f,
            clarityEnhancer = prefs[Keys.CLARITY_ENHANCER] ?: 0f,
            balance = prefs[Keys.BALANCE] ?: 0f,
            masterBypass = prefs[Keys.MASTER_BYPASS] ?: false,
        )
    }

    val homeTheatreSettings: Flow<HomeTheatreState> = context.hardikDataStore.data.map { prefs ->
        HomeTheatreState(
            spatialMode = prefs[Keys.SPATIAL_MODE]?.let {
                runCatching { SpatialMode.valueOf(it) }.getOrNull()
            } ?: SpatialMode.OFF,
            roomSimulation = prefs[Keys.ROOM_SIM]?.let {
                runCatching { RoomSimulation.valueOf(it) }.getOrNull()
            } ?: RoomSimulation.NONE,
            activeMovieProfile = prefs[Keys.MOVIE_PROFILE]?.let {
                runCatching { MovieSoundProfile.valueOf(it) }.getOrNull()
            },
            cinemaModeEnabled = prefs[Keys.CINEMA_MODE] ?: false,
            virtualSubwoofer = prefs[Keys.VIRTUAL_SUB] ?: 0.35f,
            deepBass = prefs[Keys.DEEP_BASS] ?: 0.30f,
            dialogueEnhancement = prefs[Keys.DIALOGUE_ENHANCE] ?: 0.25f,
            stereoWidening = prefs[Keys.STEREO_WIDTH] ?: 0.40f,
            theatreBassDb = prefs[Keys.THEATRE_BASS_DB] ?: 0f,
            theatreTrebleDb = prefs[Keys.THEATRE_TREBLE_DB] ?: 0f,
        )
    }

    suspend fun saveAppSettings(settings: AppSettingsState) {
        context.hardikDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themePreference.name
            prefs[Keys.HIGH_REFRESH] = settings.highRefreshMotion
            prefs[Keys.AUTO_POPUP] = settings.autoShowConnectionPopup
            prefs[Keys.GITHUB_OWNER] = settings.githubOwner.trim().ifBlank { "hardik-buds" }
            prefs[Keys.GITHUB_REPO] = settings.githubRepo.trim().ifBlank { "hardik-buds-android" }
        }
    }

    suspend fun saveSoundStudio(state: SoundStudioState) {
        context.hardikDataStore.edit { prefs ->
            prefs[Keys.EQ_BANDS] = state.eqBandsDb.joinToString(",")
            prefs[Keys.ACTIVE_PRESET_ID] = state.activePresetId
            prefs[Keys.BASS_BOOST] = state.bassBoost
            prefs[Keys.TREBLE_BOOST] = state.trebleBoost
            prefs[Keys.VOCAL_ENHANCER] = state.vocalEnhancer
            prefs[Keys.CLARITY_ENHANCER] = state.clarityEnhancer
            prefs[Keys.BALANCE] = state.balance
            prefs[Keys.MASTER_BYPASS] = state.masterBypass
        }
    }

    suspend fun saveHomeTheatre(state: HomeTheatreState) {
        context.hardikDataStore.edit { prefs ->
            prefs[Keys.SPATIAL_MODE] = state.spatialMode.name
            prefs[Keys.ROOM_SIM] = state.roomSimulation.name
            if (state.activeMovieProfile != null) {
                prefs[Keys.MOVIE_PROFILE] = state.activeMovieProfile.name
            } else {
                prefs.remove(Keys.MOVIE_PROFILE)
            }
            prefs[Keys.CINEMA_MODE] = state.cinemaModeEnabled
            prefs[Keys.VIRTUAL_SUB] = state.virtualSubwoofer
            prefs[Keys.DEEP_BASS] = state.deepBass
            prefs[Keys.DIALOGUE_ENHANCE] = state.dialogueEnhancement
            prefs[Keys.STEREO_WIDTH] = state.stereoWidening
            prefs[Keys.THEATRE_BASS_DB] = state.theatreBassDb
            prefs[Keys.THEATRE_TREBLE_DB] = state.theatreTrebleDb
        }
    }

    suspend fun resetAll() {
        context.hardikDataStore.edit { it.clear() }
    }
}
