package com.fittrack.app.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/**
 * Renders a 1080x1350 (4:5, Instagram-friendly) summary card for a finished
 * workout and hands it to the system share sheet. Drawn with android.graphics
 * so it needs no Compose host and looks identical on every device.
 */
object WorkoutShareCard {
    private const val W = 1080
    private const val H = 1350

    private val bg = Color.parseColor("#0B0C10")
    private val tile = Color.parseColor("#15161C")
    private val border = Color.parseColor("#262833")
    private val volt = Color.parseColor("#C6FF4A")
    private val voltDeep = Color.parseColor("#9BE22E")
    private val ink = Color.parseColor("#0B1A00")
    private val white = Color.parseColor("#F2F3F7")
    private val muted = Color.parseColor("#A2A6B5")

    fun render(summary: ShareSummary): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bg)

        val text = Paint(Paint.ANTI_ALIAS_FLAG)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // Wordmark + date
        text.typeface = Typeface.DEFAULT_BOLD; text.textSize = 34f; text.color = volt; text.letterSpacing = 0.18f
        c.drawText("FITTRACK", 72f, 110f, text)
        text.letterSpacing = 0f; text.typeface = Typeface.DEFAULT; text.textSize = 34f; text.color = muted
        c.drawText(summary.dateText, 72f, 164f, text)

        // Headline
        text.typeface = Typeface.DEFAULT_BOLD; text.textSize = 84f; text.color = white
        c.drawText(summary.headline, 72f, 276f, text)

        // Hero stats tile (lime gradient)
        val hero = RectF(72f, 330f, W - 72f, 600f)
        fill.shader = LinearGradient(hero.left, hero.top, hero.right, hero.bottom, volt, voltDeep, Shader.TileMode.CLAMP)
        c.drawRoundRect(hero, 40f, 40f, fill)
        fill.shader = null
        val cols = listOf(
            "${summary.durationMinutes}" to "MIN",
            summary.volumeText to "VOLUME",
            "${summary.setCount}" to "SETS",
        )
        val colW = hero.width() / 3
        cols.forEachIndexed { i, (v, l) ->
            val cx = hero.left + colW * i + colW / 2
            text.textAlign = Paint.Align.CENTER
            text.typeface = Typeface.DEFAULT_BOLD; text.textSize = 96f; text.color = ink
            c.drawText(v, cx, hero.top + 150f, text)
            text.typeface = Typeface.DEFAULT; text.textSize = 30f; text.letterSpacing = 0.12f
            c.drawText(l, cx, hero.top + 210f, text)
            text.letterSpacing = 0f
        }
        text.textAlign = Paint.Align.LEFT

        // Exercise list tile
        var y = 660f
        val listTop = y
        val lineH = 62f
        val lines = summary.exerciseLines.take(6)
        val listH = 40f + lines.size * lineH + 24f
        val list = RectF(72f, listTop, W - 72f, listTop + listH)
        fill.color = tile; c.drawRoundRect(list, 40f, 40f, fill)
        fill.style = Paint.Style.STROKE; fill.strokeWidth = 2f; fill.color = border
        c.drawRoundRect(list, 40f, 40f, fill)
        fill.style = Paint.Style.FILL
        text.typeface = Typeface.DEFAULT; text.textSize = 36f; text.color = white
        y = listTop + 40f + 42f
        for (line in lines) {
            c.drawText(ellipsize(line, text, list.width() - 96f), list.left + 48f, y, text)
            y += lineH
        }
        if (summary.exerciseCount > lines.size) {
            text.color = muted; text.textSize = 30f
            c.drawText("+${summary.exerciseCount - lines.size} more", list.left + 48f, y - 10f, text)
        }
        y = list.bottom + 36f

        // PR pills
        if (summary.prLines.isNotEmpty()) {
            text.typeface = Typeface.DEFAULT_BOLD; text.textSize = 32f
            for (pr in summary.prLines.take(3)) {
                val label = "PR   $pr"
                val w = text.measureText(label) + 56f
                val pill = RectF(72f, y, (72f + w).coerceAtMost(W - 72f), y + 68f)
                fill.color = volt; c.drawRoundRect(pill, 34f, 34f, fill)
                text.color = ink
                c.drawText(ellipsize(label, text, pill.width() - 56f), pill.left + 28f, y + 46f, text)
                y += 84f
            }
        }

        // Footer: streak + attribution
        text.typeface = Typeface.DEFAULT_BOLD; text.textSize = 40f; text.color = volt
        if (summary.streakWeeks > 0) {
            val weeks = if (summary.streakWeeks == 1) "1-week streak" else "${summary.streakWeeks}-week streak"
            c.drawText("🔥 $weeks", 72f, H - 96f, text)
        }
        text.typeface = Typeface.DEFAULT; text.textSize = 30f; text.color = muted; text.textAlign = Paint.Align.RIGHT
        c.drawText("Tracked with FitTrack", W - 72f, H - 96f, text)
        return bmp
    }

    /** Write the card to cache and open the system share sheet. */
    fun share(context: Context, summary: ShareSummary) {
        val bmp = render(summary)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "fittrack_workout_${System.currentTimeMillis()}.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${summary.headline} — ${summary.durationMinutes} min, ${summary.setCount} sets, ${summary.volumeText}. Tracked with FitTrack.")
            // ClipData is what the system chooser reads to grant itself (and
            // the picked app) access for the image preview.
            clipData = ClipData.newUri(context.contentResolver, "FitTrack workout", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share workout").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun ellipsize(s: String, p: Paint, maxWidth: Float): String {
        if (p.measureText(s) <= maxWidth) return s
        var end = s.length
        while (end > 1 && p.measureText(s.substring(0, end) + "…") > maxWidth) end--
        return s.substring(0, end) + "…"
    }
}
