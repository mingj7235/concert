package com.hhplus.concert.business.domain.manager.concert

import com.hhplus.concert.common.config.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component

@Component
class ConcertCacheManager {
    @CacheEvict(
        cacheNames = [CacheConfig.ONE_MIN_CACHE],
        key = "'available-concert'",
    )
    fun evictConcertCache() {}

    @CacheEvict(
        cacheNames = [CacheConfig.FIVE_MIN_CACHE],
        key = "'concert-' + #concertId",
    )
    fun evictConcertScheduleCache(concertId: Long) {}
}
