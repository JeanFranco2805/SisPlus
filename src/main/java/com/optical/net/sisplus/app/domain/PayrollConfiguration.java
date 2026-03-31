package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollConfiguration {

    private final double dayOvertimeMultiplier;
    private final double nightSurchargeMultiplier;
    private final double nightOvertimeMultiplier;
    private final int workHoursPerMonth;
    private final int nightStartHour;
    private final int nightEndHour;
    private final int regularWorkHours;

    private static final double DEFAULT_DAY_OVERTIME_MULTIPLIER = 1.25;
    private static final double DEFAULT_NIGHT_SURCHARGE_MULTIPLIER = 0.35;
    private static final double DEFAULT_NIGHT_OVERTIME_MULTIPLIER = 1.75;
    private static final int DEFAULT_WORK_HOURS_PER_MONTH = 240;
    private static final int DEFAULT_NIGHT_START_HOUR = 21;
    private static final int DEFAULT_NIGHT_END_HOUR = 6;
    private static final int DEFAULT_REGULAR_WORK_HOURS = 8;

    public static PayrollConfiguration defaults() {
        return PayrollConfiguration.builder()
                .dayOvertimeMultiplier(DEFAULT_DAY_OVERTIME_MULTIPLIER)
                .nightSurchargeMultiplier(DEFAULT_NIGHT_SURCHARGE_MULTIPLIER)
                .nightOvertimeMultiplier(DEFAULT_NIGHT_OVERTIME_MULTIPLIER)
                .workHoursPerMonth(DEFAULT_WORK_HOURS_PER_MONTH)
                .nightStartHour(DEFAULT_NIGHT_START_HOUR)
                .nightEndHour(DEFAULT_NIGHT_END_HOUR)
                .regularWorkHours(DEFAULT_REGULAR_WORK_HOURS)
                .build();
    }
}