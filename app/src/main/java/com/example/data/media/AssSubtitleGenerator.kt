package com.example.data.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.BlurMaskRect
import com.example.data.model.CaptionStyle
import com.example.data.model.CaptionWordCue
import com.example.data.model.EditingConfig
import com.example.data.model.SolidShapeRect
import com.example.data.model.StudioFont
import java.io.File
import java.util.Locale

object AssSubtitleGenerator {

    /**
     * Generates a complete Advanced SubStation Alpha (.ass) file with karaoke \kf timing tags
     * matching the configured style, font, colors, and vertical position.
     */
    fun generateAssFile(
        config: EditingConfig,
        title: String,
        outputFile: File
    ): File {
        outputFile.parentFile?.mkdirs()
        val primaryAss = hexToAssBgr(config.primaryColorHex)
        val highlightAss = hexToAssBgr(config.highlightColorHex)
        val borderStyle = if (config.captionStyle == CaptionStyle.BOX) 3 else 1
        val outline = when (config.captionStyle) {
            CaptionStyle.BOLD -> 4
            CaptionStyle.NEON -> 3
            CaptionStyle.BOX -> 2
            CaptionStyle.CLEAN -> 2
        }
        val shadow = if (config.captionStyle == CaptionStyle.NEON) 3 else 1
        val marginV = ((1f - config.captionPositionY.coerceIn(0.15f, 0.92f)) * 1920f).toInt()
        val fontSizePx = (config.fontSizeSp * 2.4f).toInt().coerceIn(36, 120)

        val sb = StringBuilder()
        sb.appendLine("[Script Info]")
        sb.appendLine("Title: $title")
        sb.appendLine("ScriptType: v4.00+")
        sb.appendLine("PlayResX: 1080")
        sb.appendLine("PlayResY: 1920")
        sb.appendLine("ScaledBorderAndShadow: yes")
        sb.appendLine()
        sb.appendLine("[V4+ Styles]")
        sb.appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")
        sb.appendLine(
            "Style: Default,${config.fontFamily.assFontName},$fontSizePx,$highlightAss,$primaryAss,&H00000000,&HB012100C,-1,0,0,0,100,100,0,0,$borderStyle,$outline,$shadow,2,60,60,$marginV,1"
        )
        sb.appendLine()
        sb.appendLine("[Events]")
        sb.appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")

        val chunks = config.wordCues.chunked(4)
        for (chunk in chunks) {
            if (chunk.isEmpty()) continue
            val startMs = chunk.first().startMs
            val endMs = chunk.last().endMs
            val karaokeLine = chunk.joinToString(" ") { cue ->
                val centis = ((cue.endMs - cue.startMs) / 10L).coerceAtLeast(10L)
                "{\\kf$centis}${cue.word}"
            }
            sb.appendLine(
                "Dialogue: 0,${formatAssTime(startMs)},${formatAssTime(endMs)},Default,,0,0,0,,$karaokeLine"
            )
        }

        outputFile.writeText(sb.toString(), Charsets.UTF_8)
        return outputFile
    }

    /**
     * Renders karaoke captions, blur mask rectangles, and solid shape rectangles onto a
     * transparent 1080x1920 overlay bitmap for a given frame timestamp (ms).
     */
    fun renderOverlayFrame(
        bitmap: Bitmap,
        timeMs: Long,
        config: EditingConfig,
        typeface: Typeface?
    ) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        // 1. Solid Shape Rectangles
        val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        config.solidShapes.forEach { shape: SolidShapeRect ->
            shapePaint.style = Paint.Style.FILL
            shapePaint.color = parseColorSafe(shape.colorHex, Color.parseColor("#12100C"))
            val rect = RectF(
                shape.xFraction * w,
                shape.yFraction * h,
                (shape.xFraction + shape.widthFraction) * w,
                (shape.yFraction + shape.heightFraction) * h
            )
            canvas.drawRoundRect(rect, 18f, 18f, shapePaint)
        }

        // 2. Blur / Privacy Mask Rectangles (frosted dark mosaic look burned cleanly into MP4)
        val maskFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(228, 22, 18, 14)
        }
        val maskStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(140, 255, 200, 31)
        }
        config.blurMasks.forEach { mask: BlurMaskRect ->
            val rect = RectF(
                mask.xFraction * w,
                mask.yFraction * h,
                (mask.xFraction + mask.widthFraction) * w,
                (mask.yFraction + mask.heightFraction) * h
            )
            canvas.drawRoundRect(rect, 20f, 20f, maskFill)
            canvas.drawRoundRect(rect, 20f, 20f, maskStroke)
        }

        // 3. Active Karaoke Caption Chunk
        val chunks = config.wordCues.chunked(4)
        val activeChunk = chunks.firstOrNull { chunk ->
            chunk.isNotEmpty() && timeMs in chunk.first().startMs..chunk.last().endMs
        } ?: return

        drawKaraokeChunk(canvas, w, h, timeMs, activeChunk, config, typeface)
    }

    private fun drawKaraokeChunk(
        canvas: Canvas,
        width: Float,
        height: Float,
        timeMs: Long,
        chunk: List<CaptionWordCue>,
        config: EditingConfig,
        typeface: Typeface?
    ) {
        val textSizePx = config.fontSizeSp * 2.5f
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
            this.typeface = typeface ?: Typeface.DEFAULT_BOLD
            isFakeBoldText = true
        }

        val spaceW = basePaint.measureText(" ")
        val wordWidths = chunk.map { basePaint.measureText(it.word) }
        val totalWidth = wordWidths.sum() + spaceW * (chunk.size - 1).coerceAtLeast(0)
        val centerY = height * config.captionPositionY.coerceIn(0.18f, 0.88f)
        var cursorX = (width - totalWidth) / 2f

        if (config.captionStyle == CaptionStyle.BOX) {
            val padX = 34f
            val padY = 24f
            val boxRect = RectF(
                cursorX - padX,
                centerY - textSizePx - padY * 0.4f,
                cursorX + totalWidth + padX,
                centerY + padY
            )
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(215, 18, 16, 12)
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = parseColorSafe(config.highlightColorHex, Color.parseColor("#FFC81F"))
            }
            canvas.drawRoundRect(boxRect, 22f, 22f, boxPaint)
            canvas.drawRoundRect(boxRect, 22f, 22f, borderPaint)
        }

        val primaryColor = parseColorSafe(config.primaryColorHex, Color.WHITE)
        val highlightColor = parseColorSafe(config.highlightColorHex, Color.parseColor("#FFC81F"))

        chunk.forEachIndexed { idx, cue ->
            val isActiveWord = timeMs in cue.startMs..cue.endMs
            val isSpoken = timeMs >= cue.startMs
            val scale = if (isActiveWord) 1.12f else 1.0f
            val wordW = wordWidths[idx]

            canvas.save()
            val pivotX = cursorX + wordW / 2f
            val pivotY = centerY - textSizePx * 0.35f
            canvas.scale(scale, scale, pivotX, pivotY)

            if (config.captionStyle == CaptionStyle.BOLD || config.captionStyle == CaptionStyle.NEON) {
                val strokePaint = Paint(basePaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = if (config.captionStyle == CaptionStyle.NEON) 10f else 12f
                    color = if (config.captionStyle == CaptionStyle.NEON && isActiveWord) {
                        Color.argb(180, Color.red(highlightColor), Color.green(highlightColor), Color.blue(highlightColor))
                    } else {
                        Color.BLACK
                    }
                }
                canvas.drawText(cue.word, cursorX, centerY, strokePaint)
            }

            val fillPaint = Paint(basePaint).apply {
                style = Paint.Style.FILL
                color = if (isSpoken) highlightColor else primaryColor
            }
            canvas.drawText(cue.word, cursorX, centerY, fillPaint)
            canvas.restore()

            cursorX += wordW + spaceW
        }
    }

    private fun formatAssTime(ms: Long): String {
        val totalCs = (ms.coerceAtLeast(0L) / 10L)
        val cs = totalCs % 100
        val totalSec = totalCs / 100
        val sec = totalSec % 60
        val min = (totalSec / 60) % 60
        val hr = totalSec / 3600
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hr, min, sec, cs)
    }

    private fun hexToAssBgr(hex: String): String {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return "&H00FFFFFF"
        val r = cleaned.substring(0, 2)
        val g = cleaned.substring(2, 4)
        val b = cleaned.substring(4, 6)
        return "&H00$b$g$r".uppercase(Locale.US)
    }

    fun parseColorSafe(hex: String, fallback: Int): Int {
        return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
    }
}
