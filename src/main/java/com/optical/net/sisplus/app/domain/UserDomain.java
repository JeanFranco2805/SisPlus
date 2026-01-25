package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dominio de Usuario con cálculo de nómina.
 */
@Builder
@Getter
@Setter
public class UserDomain {
    private Long id;
    private String name;
    private String lastName;
    private String cc;
    private List<AttendanceDomain> attendance;

    /**
     * Calcula la nómina diaria con configuración específica
     * @param date Fecha para calcular
     * @param config Configuración de tarifas y horarios
     * @return Cálculo de nómina
     */
    public PayrollCalculation calculateDailyPayroll(LocalDate date, PayrollConfiguration config) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        AttendanceDomain todayAttendance = getAttendanceByDate(date);
        if (todayAttendance == null || !todayAttendance.isComplete()) {
            return PayrollCalculation.builder().build();
        }

        double workedHours = todayAttendance.getWorkedHours();
        double regularHours = Math.min(workedHours, config.getRegularWorkHours());
        double totalOvertimeHours = Math.max(0, workedHours - config.getRegularWorkHours());

        double dayOvertimeHours = calculateDayOvertimeHoursFromAttendance(todayAttendance, config);
        double nightOvertimeHours = calculateNightOvertimeHoursFromAttendance(todayAttendance, config);
        double nightHours = todayAttendance.getNightHours(config);

        double regularPay = regularHours * config.getRegularHourRate();
        double nightSurchargePay = nightHours * config.getNightSurchargeRate();
        double dayOvertimePay = dayOvertimeHours * config.getDayOvertimeRate();
        double nightOvertimePay = nightOvertimeHours * config.getNightOvertimeRate();
        double totalOvertimePay = dayOvertimePay + nightOvertimePay;
        double totalPay = regularPay + nightSurchargePay + totalOvertimePay;

        return PayrollCalculation.builder()
                .regularHours(regularHours)
                .dayOvertimeHours(dayOvertimeHours)
                .nightOvertimeHours(nightOvertimeHours)
                .nightHours(nightHours)
                .regularPay(regularPay)
                .nightSurchargePay(nightSurchargePay)
                .dayOvertimePay(dayOvertimePay)
                .nightOvertimePay(nightOvertimePay)
                .totalOvertimePay(totalOvertimePay)
                .totalPay(totalPay)
                .build();
    }

    /**
     * Calcula la nómina semanal con configuración específica
     * @param date Fecha final de la semana
     * @param config Configuración de tarifas y horarios
     * @return Cálculo de nómina
     */
    public PayrollCalculation calculateWeeklyPayroll(LocalDate date, PayrollConfiguration config) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        LocalDate weekStart = date.minusDays(6);
        List<AttendanceDomain> weeklyAttendances = attendance.stream()
                .filter(a -> !a.getDate().isBefore(weekStart) && !a.getDate().isAfter(date))
                .filter(AttendanceDomain::isComplete)
                .toList();

        return calculatePayrollForPeriod(weeklyAttendances, config);
    }

    /**
     * Calcula la nómina mensual con configuración específica
     * @param month Mes a calcular
     * @param year Año a calcular
     * @param config Configuración de tarifas y horarios
     * @return Cálculo de nómina
     */
    public PayrollCalculation calculateMonthlyPayroll(int month, int year, PayrollConfiguration config) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        List<AttendanceDomain> monthlyAttendances = attendance.stream()
                .filter(a -> a.getDate() != null)
                .filter(a -> a.getDate().getMonthValue() == month && a.getDate().getYear() == year)
                .filter(AttendanceDomain::isComplete)
                .toList();

        return calculatePayrollForPeriod(monthlyAttendances, config);
    }

    /**
     * Calcula la nómina para un período de asistencias
     * @param attendances Lista de asistencias
     * @param config Configuración de tarifas y horarios
     * @return Cálculo de nómina acumulado
     */
    private PayrollCalculation calculatePayrollForPeriod(List<AttendanceDomain> attendances, PayrollConfiguration config) {
        double totalRegularHours = 0;
        double totalDayOvertimeHours = 0;
        double totalNightOvertimeHours = 0;
        double totalNightHours = 0;
        double totalRegularPay = 0;
        double totalNightSurchargePay = 0;
        double totalDayOvertimePay = 0;
        double totalNightOvertimePay = 0;

        for (AttendanceDomain att : attendances) {
            if (att == null || !att.isComplete()) {
                continue;
            }

            double workedHours = att.getWorkedHours();
            double regularHours = Math.min(workedHours, config.getRegularWorkHours());
            double nightHours = att.getNightHours(config);

            double dayOvertimeHours = calculateDayOvertimeHoursFromAttendance(att, config);
            double nightOvertimeHours = calculateNightOvertimeHoursFromAttendance(att, config);

            totalRegularHours += regularHours;
            totalDayOvertimeHours += dayOvertimeHours;
            totalNightOvertimeHours += nightOvertimeHours;
            totalNightHours += nightHours;

            totalRegularPay += regularHours * config.getRegularHourRate();
            totalNightSurchargePay += nightHours * config.getNightSurchargeRate();
            totalDayOvertimePay += dayOvertimeHours * config.getDayOvertimeRate();
            totalNightOvertimePay += nightOvertimeHours * config.getNightOvertimeRate();
        }

        double totalOvertimePay = totalDayOvertimePay + totalNightOvertimePay;
        double totalPay = totalRegularPay + totalNightSurchargePay + totalOvertimePay;

        return PayrollCalculation.builder()
                .regularHours(totalRegularHours)
                .dayOvertimeHours(totalDayOvertimeHours)
                .nightOvertimeHours(totalNightOvertimeHours)
                .nightHours(totalNightHours)
                .regularPay(totalRegularPay)
                .nightSurchargePay(totalNightSurchargePay)
                .dayOvertimePay(totalDayOvertimePay)
                .nightOvertimePay(totalNightOvertimePay)
                .totalOvertimePay(totalOvertimePay)
                .totalPay(totalPay)
                .build();
    }

    /**
     * Calcula horas extras diurnas desde una asistencia específica
     * @param attendance Asistencia a analizar
     * @param config Configuración de horarios
     * @return Horas extras diurnas
     */
    private double calculateDayOvertimeHoursFromAttendance(AttendanceDomain attendance, PayrollConfiguration config) {
        if (attendance == null || !attendance.isComplete()) {
            return 0.0;
        }

        double workedHours = attendance.getWorkedHours();
        double overtimeHours = Math.max(0, workedHours - config.getRegularWorkHours());

        if (overtimeHours == 0) {
            return 0.0;
        }

        LocalDateTime entryTime = attendance.getEntryTime();
        LocalDateTime departureTime = attendance.getDepartureTime();
        LocalDateTime regularWorkEnd = entryTime.plusHours(config.getRegularWorkHours());

        if (regularWorkEnd.isAfter(departureTime)) {
            return 0.0;
        }

        long dayMinutes = 0;
        LocalDateTime current = regularWorkEnd;

        while (current.isBefore(departureTime)) {
            int hour = current.getHour();
            if (hour >= config.getNightEndHour() && hour < config.getNightStartHour()) {
                dayMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return dayMinutes / 60.0;
    }

    /**
     * Calcula horas extras nocturnas desde una asistencia específica
     * @param attendance Asistencia a analizar
     * @param config Configuración de horarios
     * @return Horas extras nocturnas
     */
    private double calculateNightOvertimeHoursFromAttendance(AttendanceDomain attendance, PayrollConfiguration config) {
        if (attendance == null || !attendance.isComplete()) {
            return 0.0;
        }

        double workedHours = attendance.getWorkedHours();
        double overtimeHours = Math.max(0, workedHours - config.getRegularWorkHours());

        if (overtimeHours == 0) {
            return 0.0;
        }

        LocalDateTime entryTime = attendance.getEntryTime();
        LocalDateTime departureTime = attendance.getDepartureTime();
        LocalDateTime regularWorkEnd = entryTime.plusHours(config.getRegularWorkHours());

        if (regularWorkEnd.isAfter(departureTime)) {
            return 0.0;
        }

        long nightMinutes = 0;
        LocalDateTime current = regularWorkEnd;

        while (current.isBefore(departureTime)) {
            int hour = current.getHour();
            if (hour >= config.getNightStartHour() || hour < config.getNightEndHour()) {
                nightMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return nightMinutes / 60.0;
    }

    /**
     * Obtiene la asistencia de una fecha específica
     * @param date Fecha a buscar
     * @return Asistencia encontrada o null
     */
    private AttendanceDomain getAttendanceByDate(LocalDate date) {
        if (attendance == null || attendance.isEmpty()) {
            return null;
        }
        return attendance.stream()
                .filter(a -> date.equals(a.getDate()))
                .findFirst()
                .orElse(null);
    }

    // ========================================
    // MÉTODOS DEPRECATED - Mantener por compatibilidad temporalmente
    // Se eliminarán en versión 2.0
    // ========================================

    /**
     * @deprecated Usar {@link #calculateDailyPayroll(LocalDate, PayrollConfiguration)}
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateDailyPayroll(LocalDate date) {
        return calculateDailyPayroll(date, PayrollConfiguration.defaults());
    }

    /**
     * @deprecated Usar {@link #calculateWeeklyPayroll(LocalDate, PayrollConfiguration)}
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateWeeklyPayroll(LocalDate date) {
        return calculateWeeklyPayroll(date, PayrollConfiguration.defaults());
    }

    /**
     * @deprecated Usar {@link #calculateMonthlyPayroll(int, int, PayrollConfiguration)}
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateMonthlyPayroll(int month, int year) {
        return calculateMonthlyPayroll(month, year, PayrollConfiguration.defaults());
    }
}