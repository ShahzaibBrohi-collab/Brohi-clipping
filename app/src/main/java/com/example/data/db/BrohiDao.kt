package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrohiDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM short_records ORDER BY updatedTimestamp DESC")
    fun observeAllShorts(): Flow<List<ShortRecordEntity>>

    @Query("SELECT * FROM short_records WHERE id = :id")
    suspend fun getShortById(id: String): ShortRecordEntity?

    @Query("SELECT * FROM short_records WHERE id = :id")
    fun observeShortById(id: String): Flow<ShortRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShort(record: ShortRecordEntity)

    @Update
    suspend fun updateShort(record: ShortRecordEntity)

    @Query("DELETE FROM short_records WHERE id = :id")
    suspend fun deleteShortById(id: String)
}
