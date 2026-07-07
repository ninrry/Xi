package luzzr.xi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import android.util.Log
import luzzr.xi.core.network.NetworkCheck
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkCheckTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var capabilities: NetworkCapabilities
    private lateinit var networkCheck: NetworkCheck

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        context = mockk()
        connectivityManager = mockk()
        network = mockk()
        capabilities = mockk()

        every {
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns connectivityManager

        networkCheck = NetworkCheck(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `isNetworkAvailable returns true when network has internet and validated capability`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        val result = networkCheck.isNetworkAvailable()

        assertTrue(result)
    }

    @Test
    fun `isNetworkAvailable returns false when activeNetwork is null`() {
        every { connectivityManager.activeNetwork } returns null

        val result = networkCheck.isNetworkAvailable()

        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false when capabilities is null`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        val result = networkCheck.isNetworkAvailable()

        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false when no internet capability`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val result = networkCheck.isNetworkAvailable()

        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false on SecurityException`() {
        every { connectivityManager.activeNetwork } throws SecurityException("Permission denied")

        val result = networkCheck.isNetworkAvailable()

        assertFalse(result)
    }

    @Test
    fun `isNetworkAvailable returns false on generic Exception`() {
        every { connectivityManager.activeNetwork } throws RuntimeException("Unexpected error")

        val result = networkCheck.isNetworkAvailable()

        assertFalse(result)
    }
}
