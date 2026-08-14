package com.ege.cvrag.cache;

import com.ege.cvrag.constant.RagBotConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the {@link RedisCacheManager} is built with the right cache names —
 * building it doesn't touch Redis (connections are lazy), so a mocked
 * {@link RedisConnectionFactory} is enough. CI-safe, no Docker.
 */
class CacheConfigTest {

    @Test
    void registersOneCachePerConfiguredName() {
        RedisCacheManager cacheManager = new CacheConfig()
                .cacheManager(mock(RedisConnectionFactory.class), 60, 60, 10);
        cacheManager.afterPropertiesSet(); // eager-initializes from the pre-configured names (no connection made)

        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                RagBotConstants.CACHE_GITHUB_REPOS,
                RagBotConstants.CACHE_GITHUB_LANGUAGES,
                RagBotConstants.CACHE_MEDIUM_FEED,
                RagBotConstants.CACHE_ASK_ANSWERS);
    }
}
