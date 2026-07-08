package luzzr.xi.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import luzzr.xi.domain.model.TranslationResult
import java.security.MessageDigest

@Singleton
class TranslationCache @Inject constructor() {

    private val maxEntries = 50
    private val ttlMs = 3600_000L
    private val maxTextLengthForHash = 200

    private data class CacheEntry(val result: TranslationResult, val lastAccess: Long)

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
                return size > maxEntries
            }
        }
    )

    private val inflight = Collections.synchronizedSet(mutableSetOf<String>())
    private val cacheMutex = Mutex()

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
        if (!cacheMutex.tryLock()) return
        try {
            synchronized(cache) {
                cache[key] = CacheEntry(translation, System.currentTimeMillis())
            }
            inflight.remove(key)
        } finally {
            cacheMutex.unlock()
        }
    }

    fun markInFlight(text: String, sourceLang: String, targetLang: String, model: String, providerId: String): Boolean {
        val key = key(text, sourceLang, targetLang, model, providerId)
        return inflight.add(key)
    }

    suspend fun removeInFlight(text: String, sourceLang: String, targetLang: String, model: String, providerId: String) {
        val key = key(text, sourceLang, targetLang, model, providerId)
        cacheMutex.withLock {
            inflight.remove(key)
        }
    }

    fun clear() {
        if (!cacheMutex.tryLock()) return
        try {
            synchronized(cache) {
                cache.clear()
            }
            inflight.clear()
        } finally {
            cacheMutex.unlock()
        }
    }

    val size: Int get() = synchronized(cache) { cache.size }

    private fun key(text: String, sourceLang: String, targetLang: String, model: String, providerId: String): String {
        val textKey = if (text.length > maxTextLengthForHash) {
            hashText(text)
        } else {
            text
        }
        return "$providerId:$model:$sourceLang:$targetLang:$textKey"
    }

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}