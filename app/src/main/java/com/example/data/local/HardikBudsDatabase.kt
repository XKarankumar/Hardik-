package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.ConnectionHistoryItem
import com.example.domain.model.EarbudVisualStyle
import com.example.domain.model.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "custom_eq_profiles")
data class CustomEqProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandsCsv: String,
    val bassBoost: Float,
    val trebleBoost: Float,
    val vocalEnhance: Float,
    val clarityEnhance: Float,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey val address: String,
    val systemName: String,
    val customAlias: String? = null,
    val visualStyleName: String = EarbudVisualStyle.NOTHING_TRANSPARENT.name,
    val lastBatteryPercent: Int? = null,
    val lastConnectedMs: Long = 0L,
)

@Entity(tableName = "connection_history")
data class ConnectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val deviceAddress: String,
    val deviceName: String,
    val eventType: String,
    val batteryAtEvent: Int? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

@Dao
interface HardikBudsDao {
    @Query("SELECT * FROM custom_eq_profiles ORDER BY createdAtMs DESC")
    fun observeCustomProfiles(): Flow<List<CustomEqProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomProfile(profile: CustomEqProfileEntity)

    @Query("DELETE FROM custom_eq_profiles WHERE id = :id")
    suspend fun deleteCustomProfile(id: String)

    @Query("DELETE FROM custom_eq_profiles")
    suspend fun clearCustomProfiles()

    @Query("SELECT * FROM saved_devices ORDER BY lastConnectedMs DESC")
    fun observeSavedDevices(): Flow<List<SavedDeviceEntity>>

    @Query("SELECT * FROM saved_devices WHERE address = :address LIMIT 1")
    suspend fun getSavedDevice(address: String): SavedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedDevice(device: SavedDeviceEntity)

    @Query("DELETE FROM saved_devices")
    suspend fun clearSavedDevices()

    @Query("SELECT * FROM connection_history ORDER BY timestampMs DESC LIMIT 50")
    fun observeConnectionHistory(): Flow<List<ConnectionHistoryEntity>>

    @Insert
    suspend fun insertHistoryEvent(event: ConnectionHistoryEntity)

    @Query("DELETE FROM connection_history")
    suspend fun clearHistory()
}

@Database(
    entities = [
        CustomEqProfileEntity::class,
        SavedDeviceEntity::class,
        ConnectionHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class HardikBudsDatabase : RoomDatabase() {
    abstract fun dao(): HardikBudsDao

    companion object {
        @Volatile
        private var instance: HardikBudsDatabase? = null

        fun getInstance(context: Context): HardikBudsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HardikBudsDatabase::class.java,
                    "hardik_buds.db",
                ).build().also { instance = it }
            }
        }
    }
}

class LocalBudsRepository(private val dao: HardikBudsDao) {
    val customProfiles: Flow<List<EqPreset>> = dao.observeCustomProfiles().map { list ->
        list.map { entity ->
            val bands = entity.bandsCsv.split(",")
                .mapNotNull { it.toFloatOrNull() }
                .let { parsed ->
                    if (parsed.size == 10) parsed else List(10) { i -> parsed.getOrElse(i) { 0f } }
                }
            EqPreset(
                id = entity.id,
                name = entity.name,
                bandsDb = bands,
                bassBoost = entity.bassBoost,
                trebleBoost = entity.trebleBoost,
                vocalEnhance = entity.vocalEnhance,
                clarityEnhance = entity.clarityEnhance,
                isCustom = true,
            )
        }
    }

    val savedDevices: Flow<List<SavedDeviceEntity>> = dao.observeSavedDevices()

    val connectionHistory: Flow<List<ConnectionHistoryItem>> = dao.observeConnectionHistory().map { list ->
        list.map {
            ConnectionHistoryItem(
                id = it.id,
                deviceAddress = it.deviceAddress,
                deviceName = it.deviceName,
                eventType = it.eventType,
                batteryAtEvent = it.batteryAtEvent,
                timestampMs = it.timestampMs,
            )
        }
    }

    suspend fun saveCustomProfile(preset: EqPreset) {
        dao.upsertCustomProfile(
            CustomEqProfileEntity(
                id = preset.id,
                name = preset.name,
                bandsCsv = preset.bandsDb.joinToString(","),
                bassBoost = preset.bassBoost,
                trebleBoost = preset.trebleBoost,
                vocalEnhance = preset.vocalEnhance,
                clarityEnhance = preset.clarityEnhance,
            )
        )
    }

    suspend fun deleteCustomProfile(id: String) = dao.deleteCustomProfile(id)

    suspend fun updateDeviceMetadata(
        address: String,
        systemName: String,
        customAlias: String? = null,
        visualStyle: EarbudVisualStyle? = null,
        batteryPercent: Int? = null,
        touchTimestamp: Boolean = false,
    ) {
        val existing = dao.getSavedDevice(address)
        val merged = SavedDeviceEntity(
            address = address,
            systemName = systemName.ifBlank { existing?.systemName ?: address },
            customAlias = customAlias ?: existing?.customAlias,
            visualStyleName = visualStyle?.name ?: existing?.visualStyleName
                ?: EarbudVisualStyle.NOTHING_TRANSPARENT.name,
            lastBatteryPercent = batteryPercent ?: existing?.lastBatteryPercent,
            lastConnectedMs = if (touchTimestamp) System.currentTimeMillis()
            else (existing?.lastConnectedMs ?: 0L),
        )
        dao.upsertSavedDevice(merged)
    }

    suspend fun setDeviceAlias(address: String, systemName: String, alias: String) {
        val existing = dao.getSavedDevice(address)
        dao.upsertSavedDevice(
            SavedDeviceEntity(
                address = address,
                systemName = systemName.ifBlank { existing?.systemName ?: address },
                customAlias = alias.trim().ifBlank { null },
                visualStyleName = existing?.visualStyleName
                    ?: EarbudVisualStyle.NOTHING_TRANSPARENT.name,
                lastBatteryPercent = existing?.lastBatteryPercent,
                lastConnectedMs = existing?.lastConnectedMs ?: System.currentTimeMillis(),
            )
        )
    }

    suspend fun logConnectionEvent(
        address: String,
        deviceName: String,
        eventType: String,
        batteryPercent: Int?,
    ) {
        dao.insertHistoryEvent(
            ConnectionHistoryEntity(
                deviceAddress = address,
                deviceName = deviceName,
                eventType = eventType,
                batteryAtEvent = batteryPercent,
            )
        )
    }

    suspend fun clearAll() {
        dao.clearCustomProfiles()
        dao.clearSavedDevices()
        dao.clearHistory()
    }
}
