package luzzr.xi.data.cache

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import luzzr.xi.domain.model.TranslationResult

@Singleton
class TranslationCache @Inject constructor() {

    private val maxEntries = 50
    private val ttlMs = 3600_000L

    private data class CacheEntry(val result: TranslationResult, val lastAccess: Long)

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
                return size > maxEntries
            }
        }
    )

    fun get(text: String, sourceLang: String, targetLang: String, model: String, providerId: String): TranslationResult? {
        val key = key(text, sourceLang, targetLang, model, providerId)
        synchronized(cache) {
            val entry = cache[key] ?: return null
            if (System.currentTimeMillis() - entry.lastAccess > ttlMs) {
                cache.remove(key)
                return null
            }
            cache[key] = entry.copy(lastAccess = System.currentTimeMillis())
            return entry.result
        }
    }

    fun put(text: String, sourceLang: String, targetLang: String, model: String, providerId: String, translation: TranslationResult) {
        val key = key(text, sourceLang, targetLang, model, providerId)
        synchronized(cache) {
            cache[key] = CacheEntry(translation, System.currentTimeMillis())
        }
    }

    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }

    val size: Int get() = synchronized(cache) { cache.size }

    private fun key(text: String, sourceLang: String, targetLang: String, model: String, providerId: String): String =
        "$providerId:$model:$sourceLang:$targetLang:$text"
}
