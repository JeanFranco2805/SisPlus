package com.optical.net.sisplus.app.infrastructure.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * DEPENDENCIAS REQUERIDAS (pom.xml):
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-data-redis</artifactId>
 * </dependency>
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-cache</artifactId>
 * </dependency>
 *
 * CONFIGURACIÓN (application.properties):
 * spring.redis.host=localhost
 * spring.redis.port=6379
 * spring.cache.type=redis
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache Manager con Redis (para entornos distribuidos)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        ;
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(RedisSerializer.json())
                )
                .disableCachingNullValues();


        // Configuraciones específicas por caché
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Usuarios: 15 minutos
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("userById", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Configuración de nómina: 1 hora (cambia poco)
        cacheConfigurations.put("payrollConfig", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Asistencias: 5 minutos (cambia frecuentemente)
        cacheConfigurations.put("attendances", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("todayAttendances", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Cálculos de nómina: 30 minutos
        cacheConfigurations.put("payrollCalculations", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Cache Manager con Caffeine (para desarrollo/single instance)
     * Se activa cuando Redis no está disponible
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        com.github.benmanes.caffeine.cache.Caffeine<Object, Object> caffeine =
                com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                        .initialCapacity(100)
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats();

        org.springframework.cache.caffeine.CaffeineCacheManager cacheManager =
                new org.springframework.cache.caffeine.CaffeineCacheManager(
                        "payrollConfig",
                        "users",
                        "userById",
                        "attendances",
                        "todayAttendances",
                        "payrollCalculations"
                );

        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}