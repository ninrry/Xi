package luzzr.xi

import luzzr.xi.data.cache.TranslationCache
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
        assertNull(cache.get("hello", "English", "Chinese", "model"))
    }

    @Test
    fun `put and get returns cached value`() {
        cache.put("hello", "English", "Chinese", "model", "你好")
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model"))
    }

    @Test
    fun `different source languages are cached separately`() {
        cache.put("hello", "English", "Chinese", "model", "你好")
        cache.put("hello", "Japanese", "Chinese", "model", "こんにちは")
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model"))
        assertEquals("こんにちは", cache.get("hello", "Japanese", "Chinese", "model"))
    }

    @Test
    fun `different target languages are cached separately`() {
        cache.put("hello", "English", "Chinese", "model", "你好")
        cache.put("hello", "English", "Japanese", "model", "こんにちは")
        assertEquals("你好", cache.get("hello", "English", "Chinese", "model"))
        assertEquals("こんにちは", cache.get("hello", "English", "Japanese", "model"))
    }

    @Test
    fun `different models are cached separately`() {
        cache.put("hello", "English", "Chinese", "model1", "你好1")
        cache.put("hello", "English", "Chinese", "model2", "你好2")
        assertEquals("你好1", cache.get("hello", "English", "Chinese", "model1"))
        assertEquals("你好2", cache.get("hello", "English", "Chinese", "model2"))
    }

    @Test
    fun `clear removes all entries`() {
        cache.put("hello", "English", "Chinese", "model", "你好")
        cache.put("world", "English", "Chinese", "model", "世界")
        assertEquals(2, cache.size)

        cache.clear()
        assertEquals(0, cache.size)
        assertNull(cache.get("hello", "English", "Chinese", "model"))
    }

    @Test
    fun `LRU eviction removes oldest entry when max exceeded`() {
        for (i in 0 until 50) {
            cache.put("text$i", "English", "Chinese", "model", "翻译$i")
        }
        assertEquals(50, cache.size)

        cache.put("new-text", "English", "Chinese", "model", "新翻译")
        assertEquals(50, cache.size)
        assertNull(cache.get("text0", "English", "Chinese", "model"))
        assertEquals("新翻译", cache.get("new-text", "English", "Chinese", "model"))
    }

    @Test
    fun `size returns correct count`() {
        assertEquals(0, cache.size)
        cache.put("a", "en", "zh", "m", "A")
        assertEquals(1, cache.size)
        cache.put("b", "en", "zh", "m", "B")
        assertEquals(2, cache.size)
    }
}
