package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AiVoiceOption
import com.example.data.model.CaptionStyle
import com.example.data.model.GeminiModelOption
import com.example.data.model.StudioFont
import com.example.data.model.TargetLanguage
import com.example.data.model.VoiceMode

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val geminiModelId: String = GeminiModelOption.FLASH_38.modelId,
    val defaultLanguage: String = TargetLanguage.ENGLISH.name,
    val defaultVoice: String = AiVoiceOption.PUCK.name,
    val defaultVoiceMode: String = VoiceMode.INTRO.name,
    val defaultClipDurationSec: Int = 30,
    val defaultClipCount: Int = 3,
    val defaultCaptionStyle: String = CaptionStyle.BOLD.name,
    val defaultFont: String = StudioFont.BEBAS_NEUE.name,
    val preferHardwareEncoder: Boolean = true,
    val onboardingCompleted: Boolean = false
)

@Entity(tableName = "short_records")
data class ShortRecordEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val sourceUri: String?,
    val streamer: String,
    val title: String,
    val caption: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val rawVideoPath: String?,
    val finalMp4Path: String?,
    val voicePath: String?,
    val assSubtitlePath: String?,
    val thumbnailPath: String?,
    val viralScore: Int,
    val languageName: String,
    val voiceName: String,
    val voiceModeName: String,
    val createdTimestamp: Long,
    val updatedTimestamp: Long,
    val editingConfigJson: String
)
