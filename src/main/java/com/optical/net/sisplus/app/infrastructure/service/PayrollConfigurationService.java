package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.domain.PayrollConfiguration;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Servicio mejorado de configuración con caché
 * Ahora retorna PayrollConfiguration del dominio en lugar de su propio DTO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollConfigurationService {

    private final PortCaseAdapter portCaseAdapter;

    /**
     * Obtiene la configuración de nómina (con caché)
     * @return Configuración inmutable del dominio
     */
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
                .regularWorkHours(8) // Constante por ahora
                .build();
    }

    /**
     * Actualiza configuración y limpia el caché
     */
    @CacheEvict(value = "payrollConfig", allEntries = true)
    public void updateConfig(String key, String value) {
        log.info("Actualizando configuración: {} = {}", key, value);
        portCaseAdapter.updateConfig(key, value);
    }

    /**
     * Invalida el caché de configuración
     */
    @CacheEvict(value = "payrollConfig", allEntries = true)
    public void clearCache() {
        log.info("Limpiando caché de configuración");
    }

    private double getConfigDouble(String key, double defaultValue) {
        try {
            ConfigurationDomain config = portCaseAdapter.getConfig(key);
            return Double.parseDouble(config.getValue());
        } catch (Exception e) {
            log.warn("No se pudo cargar configuración {}, usando valor por defecto: {}",
                    key, defaultValue);
            return defaultValue;
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            ConfigurationDomain config = portCaseAdapter.getConfig(key);
            return Integer.parseInt(config.getValue());
        } catch (Exception e) {
            log.warn("No se pudo cargar configuración {}, usando valor por defecto: {}",
                    key, defaultValue);
            return defaultValue;
        }
    }
}