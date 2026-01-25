package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuración inmutable para el cálculo de nómina.
 * Reemplaza las variables estáticas en UserDomain.
 */
@Getter
@Builder
public class PayrollConfiguration {

    // Tarifas de pago
    private final double regularHourRate;
    private final double dayOvertimeRate;
    private final double nightSurchargeRate;
    private final double nightOvertimeRate;

    // Horarios
    private final int nightStartHour;
    private final int nightEndHour;
    private final int regularWorkHours;

    // Valores por defecto (Colombia 2026)
    private static final double DEFAULT_REGULAR_HOUR_RATE = 7959.0;
    private static final double DEFAULT_DAY_OVERTIME_RATE = 9948.0;
    private static final double DEFAULT_NIGHT_SURCHARGE_RATE = 2786.0;
    private static final double DEFAULT_NIGHT_OVERTIME_RATE = 13928.25;
    private static final int DEFAULT_NIGHT_START_HOUR = 19;
    private static final int DEFAULT_NIGHT_END_HOUR = 6;
    private static final int DEFAULT_REGULAR_WORK_HOURS = 8;

    /**
     * Crea una configuración con valores por defecto
     */
    public static PayrollConfiguration defaults() {
        return PayrollConfiguration.builder()
                .regularHourRate(DEFAULT_REGULAR_HOUR_RATE)
                .dayOvertimeRate(DEFAULT_DAY_OVERTIME_RATE)
                .nightSurchargeRate(DEFAULT_NIGHT_SURCHARGE_RATE)
                .nightOvertimeRate(DEFAULT_NIGHT_OVERTIME_RATE)
                .nightStartHour(DEFAULT_NIGHT_START_HOUR)
                .nightEndHour(DEFAULT_NIGHT_END_HOUR)
                .regularWorkHours(DEFAULT_REGULAR_WORK_HOURS)
                .build();
    }
}