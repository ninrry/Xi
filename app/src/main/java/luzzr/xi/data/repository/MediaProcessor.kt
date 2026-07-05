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
                val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val original = try {
                    BitmapFactory.decodeStream(stream)
                } finally {
                    try { stream.close() } catch (_: Exception) { Log.w("MediaProc", "stream close failed") }
                }
                if (original == null) return@withContext null

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

    suspend fun renderPdfPages(uri: Uri, maxPages: Int = 5, scale: Float = 1.5f): List<Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                val pfd: ParcelFileDescriptor =
                    context.contentResolver.openFileDescriptor(uri, "r")
                        ?: return@withContext emptyList()
                try {
                    val renderer = PdfRenderer(pfd)
                    try {
                        val pages = mutableListOf<Bitmap>()
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
                                pages.add(bitmap)
                            } finally {
                                try { page.close() } catch (_: Exception) {}
                            }
                        }
                        pages
                    } finally {
                        try { renderer.close() } catch (_: Exception) {}
                    }
                } finally {
                    try { pfd.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("MediaProc", "renderPdfPages failed", e)
                emptyList()
            }
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
