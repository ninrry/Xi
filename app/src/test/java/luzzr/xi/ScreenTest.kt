package luzzr.xi

import luzzr.xi.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScreenTest {

    @Test
    fun allScreens_haveCorrectRoutes() {
        assertEquals("translate", Screen.Translate.route)
        assertEquals("essay", Screen.Essay.route)
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun allScreens_haveNonZeroTitleResId() {
        assertNotEquals(0, Screen.Translate.titleResId)
        assertNotEquals(0, Screen.Essay.titleResId)
        assertNotEquals(0, Screen.Settings.titleResId)
    }
}
