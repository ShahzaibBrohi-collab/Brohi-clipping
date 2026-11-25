package com.example.data.db

import com.example.data.model.EditingConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class BrohiRepository(private val dao: BrohiDao) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val configAdapter = moshi.adapter(EditingConfig::class.java)

    val settingsFlow: Flow<AppSettingsEntity> = dao.observeSettings().map {
        it ?: AppSettingsEntity()
    }

    val shortsFlow: Flow<List<ShortRecordEntity>> = dao.observeAllShorts()

    suspend fun getSettings(): AppSettingsEntity {
        return dao.getSettingsOnce() ?: AppSettingsEntity().also {
            dao.upsertSettings(it)
        }
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.upsertSettings(settings)
    }

    suspend fun getShortById(id: String): ShortRecordEntity? = dao.getShortById(id)

    fun observeShortById(id: String): Flow<ShortRecordEntity?> = dao.observeShortById(id)

    suspend fun saveShort(record: ShortRecordEntity) {
        dao.upsertShort(record)
    }

    suspend fun updateEditingConfig(shortId: String, config: EditingConfig, newTitle: String? = null, newCaption: String? = null) {
        val existing = dao.getShortById(shortId) ?: return
        val updated = existing.copy(
            title = newTitle ?: existing.title,
            caption = newCaption ?: existing.caption,
            startTimeMs = config.startMs,
            endTimeMs = config.endMs,
            updatedTimestamp = System.currentTimeMillis(),
            editingConfigJson = encodeConfig(config)
        )
        dao.updateShort(updated)
    }

    suspend fun deleteShortAndMedia(id: String) = withContext(Dispatchers.IO) {
        val record = dao.getShortById(id)
        if (record != null) {
            listOfNotNull(
                record.rawVideoPath,
                record.finalMp4Path,
                record.voicePath,
                record.assSubtitlePath,
                record.thumbnailPath
            ).forEach { path ->
                runCatching { File(path).delete() }
            }
        }
        dao.deleteShortById(id)
    }

    fun encodeConfig(config: EditingConfig): String {
        return configAdapter.toJson(config)
    }

    fun decodeConfig(json: String): EditingConfig {
        return runCatching { configAdapter.fromJson(json) }.getOrNull() ?: EditingConfig()
    }
}
