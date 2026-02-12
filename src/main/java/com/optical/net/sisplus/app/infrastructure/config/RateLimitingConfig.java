package com.optical.net.sisplus.app.infrastructure.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuración de Rate Limiting usando Bucket4j
 * Protege contra ataques de fuerza bruta y abuso de API
 *
 * DEPENDENCIA REQUERIDA (pom.xml):
 * <dependency>
 *     <groupId>com.github.vladimir-bukhtoyarov</groupId>
 *     <artifactId>bucket4j-core</artifactId>
 *     <version>8.1.0</version>
 * </dependency>
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Cache de buckets por IP
     * En producción, usar Redis para distribuido
     */
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Configuración de límites por endpoint
     */
    public enum RateLimit {
        LOGIN(5, Duration.ofMinutes(1)),        // 5 intentos por minuto
        API_DEFAULT(100, Duration.ofMinutes(1)), // 100 requests por minuto
        PAYROLL(10, Duration.ofMinutes(1));      // 10 cálculos por minuto

        private final long capacity;
        private final Duration refillDuration;

        RateLimit(long capacity, Duration refillDuration) {
            this.capacity = capacity;
            this.refillDuration = refillDuration;
        }

        public Bucket createBucket() {
            Bandwidth limit = Bandwidth.classic(capacity,
                    Refill.intervally(capacity, refillDuration));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }
    }

    /**
     * Obtiene o crea un bucket para una clave específica
     */
    public Bucket resolveBucket(String key, RateLimit rateLimit) {
        return cache.computeIfAbsent(key, k -> rateLimit.createBucket());
    }

    @Scheduled(fixedDelay = 60_000) // cada 1 minuto
    public void cleanupOldBuckets() {
        cache.clear();
    }
}