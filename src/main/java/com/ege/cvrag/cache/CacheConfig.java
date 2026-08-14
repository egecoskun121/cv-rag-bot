package com.ege.cvrag.cache;

import com.ege.cvrag.constant.RagBotConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Wires Redis-backed caching, only when {@code app.cache.enabled=true}. With
 * {@code @EnableCaching} living here (not at application root), every
 * {@code @Cacheable} annotation elsewhere is simply inert when this config is
 * absent — no aspect is registered, so nothing tries to reach Redis.
 *
 * Each cache gets its own TTL (GitHub/Medium responses change rarely; answers are
 * kept shorter in case the CV content is re-indexed).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          @Value("${app.cache.github-ttl-minutes:60}") long githubTtl,
                                          @Value("${app.cache.medium-ttl-minutes:60}") long mediumTtl,
                                          @Value("${app.cache.ask-ttl-minutes:10}") long askTtl) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                RagBotConstants.CACHE_GITHUB_REPOS, defaults.entryTtl(Duration.ofMinutes(githubTtl)),
                RagBotConstants.CACHE_GITHUB_LANGUAGES, defaults.entryTtl(Duration.ofMinutes(githubTtl)),
                RagBotConstants.CACHE_MEDIUM_FEED, defaults.entryTtl(Duration.ofMinutes(mediumTtl)),
                RagBotConstants.CACHE_ASK_ANSWERS, defaults.entryTtl(Duration.ofMinutes(askTtl)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
