package com.fittrack.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.fittrack.app.data.db.dao.ProgressPhotoDao
import com.fittrack.app.data.entity.PhotoPose
import com.fittrack.app.data.entity.ProgressPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Progress photos: rows in Room, JPEGs in `files/progress_photos/`. Images
 * are re-encoded from the decoded bitmap (long edge ≤ 1600 px, quality 85),
 * which also drops EXIF metadata such as location. Nothing is uploaded.
 */
class ProgressPhotoRepository(
    private val context: Context,
    private val dao: ProgressPhotoDao,
) {
    private val appContext = context.applicationContext
    val directory: File get() = File(appContext.filesDir, DIR).apply { mkdirs() }

    fun getAll(): Flow<List<ProgressPhoto>> = dao.getAll()
    fun getCount(): Flow<Int> = dao.getCount()

    fun fileFor(photo: ProgressPhoto): File = File(directory, photo.fileName)

    /** Copy [source] into the photo store (downscaled, EXIF stripped) and insert a row. */
    suspend fun import(source: Uri, takenAt: Long, pose: PhotoPose, notes: String?): Long? =
        withContext(Dispatchers.IO) {
            val bitmap = decode(source) ?: return@withContext null
            val name = "${takenAt}_${UUID.randomUUID().toString().take(8)}.jpg"
            val target = File(directory, name)
            runCatching {
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                }
            }.onFailure {
                target.delete()
                return@withContext null
            }
            bitmap.recycle()
            dao.insert(ProgressPhoto(takenAt = takenAt, fileName = name, pose = pose, notes = notes?.trim()?.takeIf { it.isNotEmpty() }))
        }

    suspend fun update(photo: ProgressPhoto) = dao.update(photo)

    suspend fun delete(photo: ProgressPhoto) = withContext(Dispatchers.IO) {
        dao.delete(photo)
        fileFor(photo).delete()
    }

    private fun decode(source: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder honours EXIF orientation and lets us sample down.
            val src = ImageDecoder.createSource(appContext.contentResolver, source)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val longEdge = max(info.size.width, info.size.height)
                if (longEdge > MAX_EDGE) {
                    val scale = MAX_EDGE.toFloat() / longEdge
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            appContext.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
    }.getOrNull()

    companion object {
        const val DIR = "progress_photos"
        private const val MAX_EDGE = 1600
        private const val QUALITY = 85
    }
}
