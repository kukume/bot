package me.kuku.common.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
object CacheManager {

    val cacheMap: ConcurrentMap<String, Cache<*, *>> = ConcurrentHashMap()

    fun <K : Any, V : Any> getCache(key: String): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: buildCache<K, V>().also { cacheMap[key] = it }
    }

    fun <K : Any, V : Any> getCache(key: String, duration: kotlin.time.Duration): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: buildCache<K, V>(duration.inWholeMilliseconds).also { cacheMap[key] = it }
    }

    fun <K : Any, V : Any> getCache(key: String, duration: Duration): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: buildCache<K, V>(duration.toMillis()).also { cacheMap[key] = it }
    }

    fun <K : Any, V : Any> getCache(key: String, expire: Long): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: buildCache<K, V>(expire).also { cacheMap[key] = it }
    }

    fun <K : Any, V : Any> getCache(key: String, expire: Long, timeUnit: TimeUnit): Cache<K, V> {
        return cacheMap[key] as? Cache<K, V> ?: buildCache<K, V>(timeUnit.toMillis(expire)).also { cacheMap[key] = it }
    }

    fun <K : Any, V : Any> buildCache(expireAfterAccessMillis: Long? = null): Cache<K, V> {
        val builder = Caffeine.newBuilder()
            .removalListener<K, V> { _, value, cause ->
                if (cause == RemovalCause.REPLACED) return@removalListener
                kotlin.runCatching {
                    when (value) {
                        is java.io.Closeable -> value.close()
                        is AutoCloseable -> value.close()
                    }
                }
            }
        if (expireAfterAccessMillis != null && expireAfterAccessMillis > 0) {
            builder.expireAfterAccess(expireAfterAccessMillis, TimeUnit.MILLISECONDS)
        }
        return builder.build()
    }
}
