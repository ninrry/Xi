package luzzr.xi

import luzzr.xi.data.cache.TranslationCache
import luzzr.xi.domain.model.TranslationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TranslationCacheTest {

    private lateinit var cache: TranslationCache

    @Before
    fun setUp() {
        cache = TranslationCache()
    }

    @Test
    fun `get returns null for missing key`() {
        assertNull(cache.get("hello", "English", "Chinese", "model", "opencode"))
    }

    @Test
    fun `put and get returns cached value`() {
        cache.put("hello", "English", "Chinese", "model", "opencode", TranslationResult("你好"))
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model", "opencode")?.translation)
    }

    @Test
    fun `different source languages are cached separately`() {
        cache.put("hello", "English", "Chinese", "model", "opencode", TranslationResult("你好"))
        cache.put("hello", "Japanese", "Chinese", "model", "opencode", TranslationResult("こんにちは"))
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model", "opencode")?.translation)
        assertEquals("こんにちは", cache.get("hello", "Japanese", "Chinese", "model", "opencode")?.translation)
    }

    @Test
    fun `different target languages are cached separately`() {
        cache.put("hello", "English", "Chinese", "model", "opencode", TranslationResult("你好"))
        cache.put("hello", "English", "Japanese", "model", "opencode", TranslationResult("こんにちは"))
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model", "opencode")?.translation)
        assertEquals("こんにちは", cache.get("hello", "English", "Japanese", "model", "opencode")?.translation)
    }

    @Test
    fun `different models are cached separately`() {
        cache.put("hello", "English", "Chinese", "model1", "opencode", TranslationResult("你好1"))
        cache.put("hello", "English", "Chinese", "model2", "opencode", TranslationResult("你好2"))
        assertEquals("你好1", cache.get("hello", "English", "Chinese", "model1", "opencode")?.translation)
        assertEquals("你好2", cache.get("hello", "English", "Chinese", "model2", "opencode")?.translation)
    }

    @Test
    fun `clear removes all entries`() {
        cache.put("hello", "English", "Chinese", "model", "opencode", TranslationResult("你好"))
        cache.put("world", "English", "Chinese", "model", "opencode", TranslationResult("世界"))
        assertEquals(2, cache.size)

        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache.get("hello", "English", "Chinese", "model", "opencode"))
    }

    @Test
    fun `LRU eviction removes oldest entry when max exceeded`() {
        for (i in 0 until 50) {
            cache.put("text$i", "English", "Chinese", "model", "opencode", TranslationResult("翻译$i"))
        }
        assertEquals(50, cache.size)

        cache.put("new-text", "English", "Chinese", "model", "opencode", TranslationResult("新翻译"))
        assertEquals(50, cache.size)
        assertNull(cache.get("text0", "English", "Chinese", "model", "opencode"))
        assertEquals("新翻译", cache.get("new-text", "English", "Chinese", "model", "opencode")?.translation)
    }

    @Test
    fun `size returns correct count`() {
        assertEquals(0, cache.size)
        cache.put("a", "en", "zh", "m", "opencode", TranslationResult("A"))
        assertEquals(1, cache.size)
        cache.put("b", "en", "zh", "m", "opencode", TranslationResult("B"))
        assertEquals(2, cache.size)
    }

    @Test
    fun `different providers are cached separately`() {
        cache.put("hello", "English", "Chinese", "model", "opencode", TranslationResult("你好"))
        cache.put("hello", "English", "Chinese", "model", "xiaomi_mimo", TranslationResult("你好呀"))
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model", "opencode")?.translation)
        assertEquals("你好呀", cache.get("hello", "English", "Chinese", "model", "xiaomi_mimo")?.translation)
    }
}
