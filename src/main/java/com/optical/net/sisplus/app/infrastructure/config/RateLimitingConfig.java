package com.optical.net.sisplus.app.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class RateLimitingConfig {

    private static final int MAX_FAILURES = 5;

    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final Duration BUCKET_IDLE_TIMEOUT = Duration.ofMinutes(10);

    private final Map<String, TimedBucket> buckets = new ConcurrentHashMap<>();

    private final Map<String, FailureRecord> loginFailures = new ConcurrentHashMap<>();

    public enum RateLimit {
        LOGIN(5, Duration.ofMinutes(5)),

        API_DEFAULT(500, Duration.ofMinutes(1)),

        PAYROLL(10, Duration.ofMinutes(1)),

        USERS(200, Duration.ofMinutes(1));

        private final long capacity;
        private final Duration refillDuration;

        RateLimit(long capacity, Duration refillDuration) {
            this.capacity = capacity;
            this.refillDuration = refillDuration;
        }

        public Bucket createBucket() {
            Bandwidth limit = Bandwidth.classic(
                    capacity,
                    Refill.intervally(capacity, refillDuration)
            );
            return Bucket.builder().addLimit(limit).build();
        }
    }

    /**
     * Obtiene o crea el bucket de rate limit para una clave.
     * Actualiza la marca de último acceso (para el cleanup inteligente).
     */
    public Bucket resolveBucket(String key, RateLimit rateLimit) {
        TimedBucket timedBucket = buckets.compute(key, (k, existing) -> {
            if (existing == null) {
                return new TimedBucket(rateLimit.createBucket());
            }
            existing.touch();
            return existing;
        });
        return timedBucket.bucket;
    }

    /**
     * Verifica si una IP está actualmente bloqueada por demasiados fallos.
     */
    public boolean isIpBlocked(String ip) {
        FailureRecord record = loginFailures.get(ip);
        if (record == null) return false;

        if (record.blockedUntil != null && Instant.now().isBefore(record.blockedUntil)) {
            log.warn("[BRUTE-FORCE] IP bloqueada intenta acceder: {}", ip);
            return true;
        }

        if (record.blockedUntil != null) {
            loginFailures.remove(ip);
        }
        return false;
    }

    /**
     * Registra un intento de login FALLIDO.
     * Si supera MAX_FAILURES, bloquea la IP por LOCK_DURATION.
     */
    public void recordLoginFailure(String ip) {
        loginFailures.merge(ip, new FailureRecord(), (existing, newRecord) -> {
            existing.failures++;
            existing.lastAttempt = Instant.now();

            if (existing.failures >= MAX_FAILURES) {
                existing.blockedUntil = Instant.now().plus(LOCK_DURATION);
                log.warn("[BRUTE-FORCE] IP bloqueada por {} min tras {} fallos: {}",
                        LOCK_DURATION.toMinutes(), existing.failures, ip);
            }
            return existing;
        });
    }

    /**
     * Limpia el contador de fallos de una IP (llamar tras login exitoso).
     */
    public void clearLoginFailures(String ip) {
        loginFailures.remove(ip);
    }

    /**
     * Devuelve cuántos segundos quedan de bloqueo para una IP.
     */
    public long getBlockedSecondsRemaining(String ip) {
        FailureRecord record = loginFailures.get(ip);
        if (record == null || record.blockedUntil == null) return 0;
        long remaining = Instant.now().until(record.blockedUntil,
                java.time.temporal.ChronoUnit.SECONDS);
        return Math.max(0, remaining);
    }

    // =========================================================
    //  Cleanup inteligente — solo elimina buckets inactivos
    // =========================================================

    /**
     * Limpia solo los buckets que llevan más de BUCKET_IDLE_TIMEOUT sin usarse.
     * Ejecutado cada 5 minutos (antes era cada 1 min y borraba TODO).
     *
     * ANTES: cache.clear() → atacante esperaba 61s para resetear su bucket
     * AHORA: los buckets activos persisten, solo se borran los abandonados
     */
    @Scheduled(fixedDelay = 300_000) // cada 5 minutos
    public void cleanupInactiveBuckets() {
        Instant cutoff = Instant.now().minus(BUCKET_IDLE_TIMEOUT);
        int removed = 0;

        for (Map.Entry<String, TimedBucket> entry : buckets.entrySet()) {
            if (entry.getValue().lastAccess.isBefore(cutoff)) {
                buckets.remove(entry.getKey());
                removed++;
            }
        }

        // Limpiar bloqueos de login expirados
        loginFailures.entrySet().removeIf(e ->
                e.getValue().blockedUntil != null &&
                        Instant.now().isAfter(e.getValue().blockedUntil));

        if (removed > 0) {
            log.debug("[RATE-LIMIT] Cleanup: {} buckets inactivos eliminados. Activos: {}",
                    removed, buckets.size());
        }
    }

    // =========================================================
    //  Clases internas
    // =========================================================

    private static class TimedBucket {
        final Bucket bucket;
        volatile Instant lastAccess;

        TimedBucket(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        void touch() {
            this.lastAccess = Instant.now();
        }
    }

    private static class FailureRecord {
        int failures = 1;
        Instant lastAttempt = Instant.now();
        Instant blockedUntil = null;
    }
}