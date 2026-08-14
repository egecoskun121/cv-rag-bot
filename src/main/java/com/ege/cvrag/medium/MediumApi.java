package com.ege.cvrag.medium;

import com.ege.cvrag.constant.RagBotConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative client for a Medium profile's public RSS feed.
 * {@code @Cacheable} is inert unless {@code app.cache.enabled=true}.
 */
@HttpExchange
public interface MediumApi {

    /**
     * Raw RSS/XML for a profile, e.g. handle {@code @egecoskun}. {@code unless}
     * skips caching a blank response so a transient empty reply doesn't lock in
     * "no posts" for a full TTL.
     */
    @Cacheable(value = RagBotConstants.CACHE_MEDIUM_FEED, key = "#handle",
            unless = "#result == null || #result.isBlank()")
    @GetExchange("/{handle}")
    String fetchFeed(@PathVariable String handle);
}
