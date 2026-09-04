package com.uit.scirs.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * In-memory read-through cache (Caffeine, via Spring's cache abstraction) —
 * a deliberate substitute for Redis. Everything here is per-instance state:
 * fine for this project's single-node deployment, but it means an entry
 * evicted on one node stays stale on another if this ever runs behind more
 * than one instance.
 *
 * SAFETY RULES — see the callers of these caches for the enforcement:
 *   1. Never cache a method whose result depends on the calling user (report
 *      ownership, notifications, "my" endpoints, role-scoped views). A
 *      cached response served across users is a data leak, not a bug you
 *      can catch in QA — it will look correct for the user who populated
 *      the entry and wrong for everyone else who hits it after them.
 *   2. Never cache GET /api/reports/{id} (or any single-report read).
 *      Citizens track their own report's status in near-real-time; a stale
 *      cached copy directly contradicts that feature.
 *   3. Every cache below holds only public, role-agnostic, aggregate, or
 *      reference data — categories, departments, the leaderboard, the
 *      citizen-visible map pins, and cross-department stats.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIES = "categories";
    public static final String DEPARTMENTS = "departments";
    public static final String LEADERBOARD = "leaderboard";
    public static final String PUBLIC_MAP = "publicMap";
    public static final String DEPT_STATS = "deptStats";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache(CATEGORIES, Duration.ofHours(1), 200),
                buildCache(DEPARTMENTS, Duration.ofHours(1), 200),
                buildCache(LEADERBOARD, Duration.ofSeconds(60), 10),
                buildCache(PUBLIC_MAP, Duration.ofSeconds(60), 50),
                buildCache(DEPT_STATS, Duration.ofMinutes(5), 100)
        ));
        return manager;
    }

    /**
     * recordStats() is enabled on every cache so hit/miss ratios are
     * observable via each Caffeine cache's native stats() — wire an actuator
     * metrics endpoint or a debug controller to CacheManager.getCache(name)
     * to surface them for the demo.
     */
    private CaffeineCache buildCache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
