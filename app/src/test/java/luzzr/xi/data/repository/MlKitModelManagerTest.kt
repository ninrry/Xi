package luzzr.xi.data.repository

import luzzr.xi.domain.model.ModelDownloadState
import org.junit.Assert.assertEquals
import org.junit.Test

class MlKitModelManagerTest {

    @Test
    fun `initial status is IDLE`() {
        val manager = MlKitModelManager()
        val status = manager.getStatus("en", "zh")
        assertEquals(ModelDownloadState.IDLE, status)
    }

    @Test
    fun `getDownloadProgress returns empty progress when idle`() {
        val manager = MlKitModelManager()
        val progress = manager.getDownloadProgress("en->zh")
        assertEquals(0f, progress.progress, 0.01f)
        assertEquals(ModelDownloadState.IDLE, progress.state)
    }

    @Test
    fun `cancelDownload resets DOWNLOADING to FAILED`() {
        val manager = MlKitModelManager()
        manager.cancelDownload()
        assertEquals(ModelDownloadState.IDLE, manager.getStatus("en", "zh"))
    }
}
