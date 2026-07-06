package luzzr.xi

import luzzr.xi.domain.model.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedLanguageTest {

    @Test
    fun getByCode_returnsCorrectLanguage_forValidCode() {
        assertEquals(SupportedLanguage.CHINESE, SupportedLanguage.getByCode("zh"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("en"))
        assertEquals(SupportedLanguage.JAPANESE, SupportedLanguage.getByCode("ja"))
        assertEquals(SupportedLanguage.KOREAN, SupportedLanguage.getByCode("ko"))
        assertEquals(SupportedLanguage.SPANISH, SupportedLanguage.getByCode("es"))
        assertEquals(SupportedLanguage.FRENCH, SupportedLanguage.getByCode("fr"))
        assertEquals(SupportedLanguage.GERMAN, SupportedLanguage.getByCode("de"))
        assertEquals(SupportedLanguage.ITALIAN, SupportedLanguage.getByCode("it"))
        assertEquals(SupportedLanguage.PORTUGUESE, SupportedLanguage.getByCode("pt"))
        assertEquals(SupportedLanguage.RUSSIAN, SupportedLanguage.getByCode("ru"))
        assertEquals(SupportedLanguage.ARABIC, SupportedLanguage.getByCode("ar"))
        assertEquals(SupportedLanguage.HINDI, SupportedLanguage.getByCode("hi"))
        assertEquals(SupportedLanguage.THAI, SupportedLanguage.getByCode("th"))
        assertEquals(SupportedLanguage.VIETNAMESE, SupportedLanguage.getByCode("vi"))
        assertEquals(SupportedLanguage.INDONESIAN, SupportedLanguage.getByCode("id"))
        assertEquals(SupportedLanguage.TURKISH, SupportedLanguage.getByCode("tr"))
        assertEquals(SupportedLanguage.DUTCH, SupportedLanguage.getByCode("nl"))
        assertEquals(SupportedLanguage.POLISH, SupportedLanguage.getByCode("pl"))
        assertEquals(SupportedLanguage.SWEDISH, SupportedLanguage.getByCode("sv"))
        assertEquals(SupportedLanguage.UKRAINIAN, SupportedLanguage.getByCode("uk"))
    }

    @Test
    fun getByCode_returnsEnglish_forInvalidCode() {
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("xx"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode(""))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("zz"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.getByCode("unknown"))
    }

    @Test
    fun allEntries_haveUniqueCodes() {
        val codes = SupportedLanguage.entries.map { it.code }
        assertEquals(
            "Expected ${SupportedLanguage.entries.size} unique codes but found ${codes.toSet().size}",
            SupportedLanguage.entries.size,
            codes.toSet().size
        )
        assertTrue("All codes should be 2-letter strings", codes.all { it.length == 2 })
    }
}
