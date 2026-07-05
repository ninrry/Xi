package luzzr.xi

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import luzzr.xi.data.repository.MediaProcessor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.InputStream

/**
 * Tests MediaProcessor bitmap and PDF processing.
 * Mocks Context / ContentResolver / Uri / BitmapFactory / Bitmap / PdfRenderer
 * to exercise the success, scaling, null, and exception branches without
 * touching the real Android framework.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaProcessorTest {

    @get:Rule
    val testDispatcherRule = TestDispatcherRule()

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()

    private lateinit var processor: MediaProcessor

    @Before
    fun setUp() {
        every { context.contentResolver } returns contentResolver

        // MediaProcessor logs on error paths; stub Log so the JVM stubs
        // do not throw "Method e in android.util.Log not mocked".
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        processor = MediaProcessor(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // -----------------------------------------------------------------
    // loadBitmapFromUri
    // -----------------------------------------------------------------

    @Test
    fun `loadBitmapFromUri returns null when stream is null`() = runTest {
        // Given: ContentResolver returns a null InputStream
        val uri = mockk<Uri>()
        every { contentResolver.openInputStream(uri) } returns null

        // When: loadBitmapFromUri is called
        val result = processor.loadBitmapFromUri(uri)

        // Then: result is null
        assertNull(result)
    }

    @Test
    fun `loadBitmapFromUri returns null when BitmapFactory returns null`() = runTest {
        // Given: stream opens, but BitmapFactory.decodeStream cannot decode
        val uri = mockk<Uri>()
        val stream = mockk<InputStream>(relaxed = true)
        every { contentResolver.openInputStream(uri) } returns stream
        every { stream.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(stream) } returns null

        // When: loadBitmapFromUri is called
        val result = processor.loadBitmapFromUri(uri)

        // Then: result is null
        assertNull(result)
    }

    @Test
    fun `loadBitmapFromUri returns original bitmap when within maxSize`() = runTest {
        // Given: decoded bitmap 500x500, maxSize 800 (no scaling needed)
        val uri = mockk<Uri>()
        val stream = mockk<InputStream>(relaxed = true)
        val original = mockk<Bitmap>(relaxed = true)
        every { original.width } returns 500
        every { original.height } returns 500

        every { contentResolver.openInputStream(uri) } returns stream
        every { stream.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(stream) } returns original

        // When: loadBitmapFromUri is called with maxSize=800
        val result = processor.loadBitmapFromUri(uri, maxSize = 800)

        // Then: same bitmap instance returned, never recycled
        assertSame(original, result)
        verify(exactly = 0) { original.recycle() }
    }

    @Test
    fun `loadBitmapFromUri returns scaled bitmap when exceeding maxSize`() = runTest {
        // Given: decoded bitmap 1600x1200, maxSize 800 → 800x600
        val uri = mockk<Uri>()
        val stream = mockk<InputStream>(relaxed = true)
        val original = mockk<Bitmap>(relaxed = true)
        val scaled = mockk<Bitmap>(relaxed = true)

        every { original.width } returns 1600
        every { original.height } returns 1200
        every { contentResolver.openInputStream(uri) } returns stream
        every { stream.close() } returns Unit

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(stream) } returns original
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(original, 800, 600, true) } returns scaled

        // When: loadBitmapFromUri is called with maxSize=800
        val result = processor.loadBitmapFromUri(uri, maxSize = 800)

        // Then: scaled bitmap returned and original is recycled
        assertSame(scaled, result)
        verify { original.recycle() }
    }

    @Test
    fun `loadBitmapFromUri returns null on exception`() = runTest {
        // Given: opening the stream throws
        val uri = mockk<Uri>()
        every { contentResolver.openInputStream(uri) } throws RuntimeException("boom")

        // When: loadBitmapFromUri is called
        val result = processor.loadBitmapFromUri(uri)

        // Then: result is null
        assertNull(result)
    }

    // -----------------------------------------------------------------
    // renderPdfPages
    // -----------------------------------------------------------------

    @Test
    fun `renderPdfPages returns empty list when pfd is null`() = runTest {
        // Given: ContentResolver returns a null ParcelFileDescriptor
        val uri = mockk<Uri>()
        every { contentResolver.openFileDescriptor(uri, "r") } returns null

        // When: renderPdfPages is called
        val result = processor.renderPdfPages(uri)

        // Then: empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `renderPdfPages returns empty list on exception`() = runTest {
        // Given: opening the file descriptor throws
        val uri = mockk<Uri>()
        every { contentResolver.openFileDescriptor(uri, "r") } throws RuntimeException("boom")

        // When: renderPdfPages is called
        val result = processor.renderPdfPages(uri)

        // Then: empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `renderPdfPages respects maxPages limit`() = runTest {
        // Given: a PDF with 5 pages, maxPages=2
        val uri = mockk<Uri>()
        val pfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { contentResolver.openFileDescriptor(uri, "r") } returns pfd

        val page0 = mockk<PdfRenderer.Page>(relaxed = true)
        val page1 = mockk<PdfRenderer.Page>(relaxed = true)
        every { page0.width } returns 100
        every { page0.height } returns 100
        every { page1.width } returns 100
        every { page1.height } returns 100

        val bitmap0 = mockk<Bitmap>(relaxed = true)
        val bitmap1 = mockk<Bitmap>(relaxed = true)

        mockkConstructor(PdfRenderer::class)
        every { anyConstructed<PdfRenderer>().pageCount } returns 5
        every { anyConstructed<PdfRenderer>().openPage(0) } returns page0
        every { anyConstructed<PdfRenderer>().openPage(1) } returns page1

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(any(), any(), any()) } returnsMany listOf(bitmap0, bitmap1)

        // When: renderPdfPages is called with maxPages=2
        val result = processor.renderPdfPages(uri, maxPages = 2)

        // Then: exactly 2 bitmaps returned, page 2 is never opened
        assertEquals(2, result.size)
        verify(exactly = 0) { anyConstructed<PdfRenderer>().openPage(2) }
    }

    @Test
    fun `renderPdfPages applies scale factor`() = runTest {
        // Given: a 1-page PDF, page 100x200, scale=2.0 → bitmap 200x400
        val uri = mockk<Uri>()
        val pfd = mockk<ParcelFileDescriptor>(relaxed = true)
        every { contentResolver.openFileDescriptor(uri, "r") } returns pfd

        val page = mockk<PdfRenderer.Page>(relaxed = true)
        every { page.width } returns 100
        every { page.height } returns 200

        val bitmap = mockk<Bitmap>(relaxed = true)

        mockkConstructor(PdfRenderer::class)
        every { anyConstructed<PdfRenderer>().pageCount } returns 1
        every { anyConstructed<PdfRenderer>().openPage(0) } returns page

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888) } returns bitmap

        // When: renderPdfPages is called with scale=2.0f
        val result = processor.renderPdfPages(uri, scale = 2.0f)

        // Then: bitmap created with scaled dimensions and returned
        assertEquals(1, result.size)
        assertSame(bitmap, result[0])
        verify { Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888) }
    }
}
