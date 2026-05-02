package com.oyo.backend.config;

import com.oyo.backend.service.AdminService;
import com.oyo.backend.service.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupRunner implements ApplicationRunner {

    private final AdminService adminService;
    private final HotelService hotelService;
    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Warming up caches on startup...");

        // Evict stale entries first — Redis may hold LinkedHashMap values from a
        // previous deployment that cause ClassCastException when used as Page/List.
        evictStaleEntries();

        try {
            adminService.getStats();
            log.info("✅ adminStats cache warmed up");
        } catch (Exception e) {
            log.warn("⚠️ Could not warm adminStats cache: {}", e.getMessage());
        }

        try {
            // size=10 matches the controller's @RequestParam(defaultValue = "10")
            hotelService.getFeaturedHotelsInternal(0, 10);
            hotelService.getFeaturedHotelsInternal(1, 10);
            log.info("✅ featuredHotels cache warmed up (pages 0 & 1, size=10)");
        } catch (Exception e) {
            log.warn("⚠️ Could not warm featuredHotels cache: {}", e.getMessage());
        }

        try {
            hotelService.getCities();
            log.info("✅ cities cache warmed up");
        } catch (Exception e) {
            log.warn("⚠️ Could not warm cities cache: {}", e.getMessage());
        }

        try {
            hotelService.searchHotelsInternal(null, null, null, null, null, null, 0, 20);
            log.info("✅ defaultHotels cache warmed up");
        } catch (Exception e) {
            log.warn("⚠️ Could not warm defaultHotels cache: {}", e.getMessage());
        }

        log.info("Cache warm-up complete.");
    }

    /**
     * Clears caches that are prone to deserialization poisoning between deployments.
     *
     * Problem: Redis stores values as plain JSON (no type metadata). After a code
     * change, Jackson deserializes them as LinkedHashMap instead of Page/List, which
     * causes ClassCastExceptions that bypass ResilientCacheConfig entirely — the
     * exception happens in user code AFTER Spring successfully retrieves the value.
     *
     * Solution: clear on every startup so warmup always writes fresh, correctly-typed
     * values into Redis.
     */
    private void evictStaleEntries() {
        String[] cacheNames = {"featuredHotels", "defaultHotels", "cities", "adminStats"};
        for (String name : cacheNames) {
            try {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                    log.info("[CacheWarmup] 🗑️  Evicted stale entries from '{}'", name);
                }
            } catch (Exception e) {
                log.warn("[CacheWarmup] Could not evict '{}': {}", name, e.getMessage());
            }
        }
    }
}

