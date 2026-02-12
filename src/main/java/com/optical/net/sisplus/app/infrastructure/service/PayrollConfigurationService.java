package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.domain.PayrollConfiguration;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollConfigurationService {

    private final PortCaseAdapter portCaseAdapter;

    @Cacheable(value = "payrollConfig", key = "'default'")
    public PayrollConfiguration getPayrollConfig() {
        log.debug("Cargando configuración de nómina desde base de datos");

        return PayrollConfiguration.builder()
                .regularHourRate(getConfigDouble("REGULAR_HOUR_RATE", 7959.0))
                .dayOvertimeRate(getConfigDouble("DAY_OVERTIME_RATE", 9948.0))
                .nightSurchargeRate(getConfigDouble("NIGHT_SURCHARGE_RATE", 2786.0))
                .nightOvertimeRate(getConfigDouble("NIGHT_OVERTIME_RATE", 13928.25))
                .nightStartHour(getConfigInt("NIGHT_START_HOUR", 19))
                .nightEndHour(getConfigInt("NIGHT_END_HOUR", 6))
                .regularWorkHours(8)
                .build();
    }

    @CacheEvict(value = "payrollConfig", allEntries = true)
    public void updateConfig(String key, String value) {
        log.info("Actualizando configuración: {} = {}", key, value);
        portCaseAdapter.updateConfig(key, value);
    }

    @CacheEvict(value = "payrollConfig", allEntries = true)
    public void clearCache() {
        log.info("Caché de configuración limpiado manualmente");
    }

    private double getConfigDouble(String key, double defaultValue) {
        try {
            ConfigurationDomain config = portCaseAdapter.getConfig(key);
            return Double.parseDouble(config.getValue());
        } catch (Exception e) {
            log.warn("No se pudo cargar {}, usando default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            ConfigurationDomain config = portCaseAdapter.getConfig(key);
            return Integer.parseInt(config.getValue());
        } catch (Exception e) {
            log.warn("No se pudo cargar {}, usando default: {}", key, defaultValue);
            return defaultValue;
        }
    }
}