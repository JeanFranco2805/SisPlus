package com.optical.net.sisplus.app.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché usando Caffeine
 *
 * BENEFICIOS:
 * - Mejora performance de consultas repetidas
 * - Reduce carga en base de datos
 * - Configuración centralizada de TTL
 *
 * DEPENDENCIA REQUERIDA (pom.xml):
 * <dependency>
 *     <groupId>com.github.ben-manes.caffeine</groupId>
 *     <artifactId>caffeine</artifactId>
 * </dependency>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configuración del cache manager con Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "payrollConfig",      // Configuración de nómina
                "users",              // Lista de usuarios
                "userById",           // Usuarios individuales
                "attendances"         // Asistencias
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Configuración de Caffeine
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)                    // Capacidad inicial
                .maximumSize(1000)                       // Máximo de entradas
                .expireAfterWrite(5, TimeUnit.MINUTES)   // Expira después de 5 minutos
                .recordStats();                          // Habilita estadísticas
    }
}