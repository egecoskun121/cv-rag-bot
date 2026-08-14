package com.ege.cvrag.medium;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Declarative client for a Medium profile's public RSS feed. */
@HttpExchange
public interface MediumApi {

    /** Raw RSS/XML for a profile, e.g. handle {@code @egecoskun}. */
    @GetExchange("/{handle}")
    String fetchFeed(@PathVariable String handle);
}
