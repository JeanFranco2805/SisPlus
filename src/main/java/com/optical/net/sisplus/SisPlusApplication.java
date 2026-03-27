package com.optical.net.sisplus;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Configuration;
import com.optical.net.sisplus.app.infrastructure.repository.ConfigurationRepository;
import com.optical.net.sisplus.app.infrastructure.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class SisPlusApplication implements CommandLineRunner {

    private final ConfigurationRepository configurationRepository;
    private final AdminService adminService;

    public SisPlusApplication(ConfigurationRepository configurationRepository, AdminService adminService) {
        this.configurationRepository = configurationRepository;
        this.adminService = adminService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SisPlusApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Inicializando configuraciones del sistema...");

        // Crear configuraciones por defecto si no existen
        getOrCreate("TIME_ZONE", "America/Bogota");
        getOrCreate("REGULAR_HOUR_RATE", "7959");
        getOrCreate("DAY_OVERTIME_RATE", "9948");
        getOrCreate("NIGHT_SURCHARGE_RATE", "2786");
        getOrCreate("NIGHT_OVERTIME_RATE", "13928.25");
        getOrCreate("NIGHT_START_HOUR", "19");
        getOrCreate("NIGHT_END_HOUR", "6");

        String timeZone = getOrCreate("TIME_ZONE", "America/Bogota");
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }

    private String getOrCreate(String key, String defaultValue) {
        return configurationRepository.findByKey(key)
                .map(Configuration::getValue)
                .orElseGet(() -> {
                    Configuration config = Configuration.builder()
                            .key(key)
                            .value(defaultValue)
                            .build();
                    configurationRepository.save(config);
                    log.debug("Configuración creada: {} = {}", key, defaultValue);
                    return defaultValue;
                });
    }
}