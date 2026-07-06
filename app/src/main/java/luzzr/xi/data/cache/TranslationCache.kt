package luzzr.xi.data.cache

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationCache @Inject constructor() {

    private val maxEntries = 50

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean {
                return size > maxEntries
            }
        }
    )
    
    private val lastAccessTime = ConcurrentHashMap<String, Long>()

    fun get(text: String, sourceLang: String, targetLang: String, model: String): String? {
        val key = key(text, sourceLang, targetLang, model)
        val lastAccess = lastAccessTime[key] ?: return null
        if (System.currentTimeMillis() - lastAccess > 3600_000L) {
            cache.remove(key)
            lastAccessTime.remove(key)
            return null
        }
        lastAccessTime[key] = System.currentTimeMillis()
        return cache[key]
    }

    fun put(text: String, sourceLang: String, targetLang: String, model: String, translation: String) {
        val key = key(text, sourceLang, targetLang, model)
        cache[key] = translation
        lastAccessTime[key] = System.currentTimeMillis()
    }

    fun clear() {
        cache.clear()
        lastAccessTime.clear()
    }

    val size: Int get() = cache.size

    private fun key(text: String, sourceLang: String, targetLang: String, model: String): String =
        "$model:$sourceLang:$targetLang:$text"
}
