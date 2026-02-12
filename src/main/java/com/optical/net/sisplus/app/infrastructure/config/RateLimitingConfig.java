package com.optical.net.sisplus.app.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    private static final long BUCKET_EXPIRY_SECONDS = 3600;

    private final Map<String, BucketEntry> cache = new ConcurrentHashMap<>();

    public enum RateLimit {
        LOGIN(5, Duration.ofMinutes(15)),
        API_DEFAULT(100, Duration.ofMinutes(1)),
        PAYROLL(10, Duration.ofMinutes(1));

        private final long capacity;
        private final Duration refillDuration;

        RateLimit(long capacity, Duration refillDuration) {
            this.capacity = capacity;
            this.refillDuration = refillDuration;
        }

        public Bucket createBucket() {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }
    }

    public Bucket resolveBucket(String key, RateLimit rateLimit) {
        BucketEntry entry = cache.computeIfAbsent(key, k -> new BucketEntry(rateLimit.createBucket()));
        entry.updateLastAccess();
        return entry.getBucket();
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupExpiredBuckets() {
        Instant threshold = Instant.now().minusSeconds(BUCKET_EXPIRY_SECONDS);
        cache.entrySet().removeIf(e -> e.getValue().getLastAccess().isBefore(threshold));
    }

    private static class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccess;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        Bucket getBucket() {
            return bucket;
        }

        Instant getLastAccess() {
            return lastAccess;
        }

        void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
    }
}