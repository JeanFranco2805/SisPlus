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
                .dayOvertimeMultiplier(getConfigDouble("DAY_OVERTIME_MULTIPLIER", 1.25))
                .nightSurchargeMultiplier(getConfigDouble("NIGHT_SURCHARGE_MULTIPLIER", 0.35))
                .nightOvertimeMultiplier(getConfigDouble("NIGHT_OVERTIME_MULTIPLIER", 1.75))
                .workHoursPerMonth(getConfigInt("WORK_HOURS_PER_MONTH", 240))
                .nightStartHour(getConfigInt("NIGHT_START_HOUR", 21))
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