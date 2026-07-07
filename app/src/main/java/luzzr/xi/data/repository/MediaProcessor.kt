package luzzr.xi.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Bitmap and PDF processing off the main thread.
 * ViewModels should not hold Context or Bitmaps — delegate to this service.
 */
@Singleton
class MediaProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun loadBitmapFromUri(uri: Uri, maxSize: Int = 800): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null

                options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)
                options.inJustDecodeBounds = false

                val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@withContext null

                if (original.width > maxSize || original.height > maxSize) {
                    val ratio = maxSize.toFloat() / maxOf(original.width, original.height)
                    val scaled = Bitmap.createScaledBitmap(
                        original,
                        (original.width * ratio).toInt(),
                        (original.height * ratio).toInt(),
                        true
                    )
                    if (scaled !== original) original.recycle()
                    scaled
                } else {
                    original
                }
            } catch (e: Exception) {
                Log.e("MediaProc", "loadBitmapFromUri failed", e)
                null
            }
        }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun imageUriToBase64(uri: Uri, maxSize: Int = 1024): String? =
        withContext(Dispatchers.IO) {
            try {
                val bitmap = loadBitmapFromUri(uri, maxSize) ?: return@withContext null
                val baos = java.io.ByteArrayOutputStream()
                val quality = getOptimalQuality(bitmap.byteCount)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val result = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                bitmap.recycle()
                result
            } catch (e: Exception) {
                Log.e("MediaProc", "Failed to convert URI to base64", e)
                null
            }
        }

    private fun getOptimalQuality(byteCount: Int): Int = when {
        byteCount < 500_000 -> 95
        byteCount < 1_500_000 -> 85
        else -> 75
    }

    suspend fun renderPdfPagesAsBase64(uri: Uri, maxPages: Int = 5, scale: Float = 1.5f): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val pfd: ParcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: return@withContext emptyList()
                try {
                    val renderer = PdfRenderer(pfd)
                    try {
                        val result = mutableListOf<String>()
                        val pageCount = minOf(renderer.pageCount, maxPages)

                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            try {
                                val bitmap = Bitmap.createBitmap(
                                    (page.width * scale).toInt(),
                                    (page.height * scale).toInt(),
                                    Bitmap.Config.ARGB_8888
                                )
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val baos = java.io.ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                                result.add(base64)
                                bitmap.recycle()
                            } finally {
                                try { page.close() } catch (_: Exception) {}
                            }
                        }
                        result
                    } finally {
                        try { renderer.close() } catch (_: Exception) {}
                    }
                } finally {
                    try { pfd.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("MediaProc", "renderPdfPagesAsBase64 failed", e)
                emptyList()
            }
        }
}
