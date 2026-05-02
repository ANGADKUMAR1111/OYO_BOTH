package com.oyo.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-tier rate limiter:
 *
 * • PUBLIC tier  — read-only hotel/search endpoints: 1 000 req/min per IP
 *   (accommodates burst load tests and real traffic spikes)
 * • STRICT tier  — auth, write, payment endpoints:   30 req/min per IP
 * • BYPASS       — Swagger, actuator, health checks: unlimited
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // ── Bucket stores keyed by "<tier>:<ip>" ─────────────────────────────
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // ── Tier capacities ──────────────────────────────────────────────────
    /** 1 000 tokens, refilled at 1 000/min (smooth: ~16/s). */
    private static final int  PUBLIC_CAPACITY = 1_000;
    private static final long PUBLIC_REFILL_PER_MIN = 1_000;

    /** 30 tokens, refilled at 30/min (~1 every 2 s). */
    private static final int  STRICT_CAPACITY = 30;
    private static final long STRICT_REFILL_PER_MIN = 30;

    // ── Path classification ───────────────────────────────────────────────

    /** Paths that bypass rate limiting entirely. */
    private static final String[] BYPASS_PREFIXES = {
        "/swagger-ui",
        "/v3/api-docs",
        "/actuator",
        "/api/health"
    };

    /** Public read-only prefixes that get the generous PUBLIC bucket. */
    private static final String[] PUBLIC_PREFIXES = {
        "/api/hotels",
        "/api/search",
        "/api/rooms",
        "/api/reviews"
    };

    // ── Bucket factories ─────────────────────────────────────────────────

    private Bucket createPublicBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(PUBLIC_CAPACITY)
                .refillGreedy(PUBLIC_REFILL_PER_MIN, Duration.ofMinutes(1))
                .initialTokens(PUBLIC_CAPACITY)   // start full — Bucket4j 8.x defaults to 0
                .build())
            .build();
    }

    private Bucket createStrictBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(STRICT_CAPACITY)
                .refillGreedy(STRICT_REFILL_PER_MIN, Duration.ofMinutes(1))
                .initialTokens(STRICT_CAPACITY)   // start full — Bucket4j 8.x defaults to 0
                .build())
            .build();
    }

    // ── Filter logic ──────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // getRequestURI() is always reliable; getServletPath() can be empty inside Spring Security filters
        String uri = request.getRequestURI();
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;

        // 1. Bypass — no rate limiting for swagger, health checks, or localhost (for local load testing)
        String ip = getClientIp(request);
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            chain.doFilter(request, response);
            return;
        }

        for (String prefix : BYPASS_PREFIXES) {
            if (path.startsWith(prefix)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 2. Determine tier
        boolean isPublic = false;
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                // Only GET requests get the generous limit
                if ("GET".equalsIgnoreCase(request.getMethod())) {
                    isPublic = true;
                }
                break;
            }
        }

        String bucketKey = (isPublic ? "pub:" : "str:") + ip;
        final boolean isPublicFinal = isPublic;   // effectively-final copy for the lambda

        Bucket bucket = buckets.computeIfAbsent(bucketKey,
            k -> isPublicFinal ? createPublicBucket() : createStrictBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests\"}");
        }
    }

    /**
     * Honour X-Forwarded-For so that requests through a reverse proxy
     * (nginx, Render load balancer, etc.) use the real client IP.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
