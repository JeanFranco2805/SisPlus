package com.optical.net.sisplus.app.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para procesamiento asíncrono
 * Permite ejecutar tareas pesadas sin bloquear el hilo principal
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Executor para cálculos de nómina
     * Pool de 5 threads para procesar múltiples cálculos en paralelo
     */
    @Bean(name = "payrollExecutor")
    public Executor payrollExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configuración del pool
        executor.setCorePoolSize(5);           // Mínimo de threads
        executor.setMaxPoolSize(10);           // Máximo de threads
        executor.setQueueCapacity(25);         // Cola de espera
        executor.setThreadNamePrefix("Payroll-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Política de rechazo: Callee-Runs (ejecutar en el thread que llama)
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();
        log.info("Payroll Executor inicializado: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }

    /**
     * Executor por defecto para otras tareas asíncronas
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }

    /**
     * Manejador de excepciones no capturadas en tareas asíncronas
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Excepción no capturada en método asíncrono: {}",
                    method.getName(), throwable);
            log.error("Parámetros: {}", (Object[]) params);
        };
    }
}