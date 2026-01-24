package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class UserDomain {
    private static final double MINIMUM_SALARY = 1750905.0;
    private static final int WEEKLY_HOURS = 44;
    private static final double DAILY_HOURS = 7.33333333;
    private static final int MONTHLY_HOURS = 220;

    private static final double REGULAR_HOUR_RATE = 7959.0;
    private static final double DAY_OVERTIME_RATE = 9948.0;
    private static final double NIGHT_SURCHARGE_RATE = 2786.0;
    private static final double NIGHT_OVERTIME_RATE = 13928.25;

    private static final int REGULAR_WORK_HOURS = 8;
    private static final int NIGHT_START_HOUR = 19;
    private static final int NIGHT_END_HOUR = 6;

    private Long id;
    private String name;
    private String lastName;
    private String cc;
    private List<AttendanceDomain> attendance;

    public PayrollCalculation calculateDailyPayroll(LocalDate date) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        AttendanceDomain todayAttendance = getAttendanceByDate(date);
        if (todayAttendance == null || !todayAttendance.isComplete()) {
            return PayrollCalculation.builder().build();
        }

        double workedHours = todayAttendance.getWorkedHours();
        double regularHours = Math.min(workedHours, REGULAR_WORK_HOURS);
        double totalOvertimeHours = Math.max(0, workedHours - REGULAR_WORK_HOURS);

        double dayOvertimeHours = calculateDayOvertimeHoursFromAttendance(todayAttendance);
        double nightOvertimeHours = calculateNightOvertimeHoursFromAttendance(todayAttendance);
        double nightHours = todayAttendance.getNightHours();

        double regularPay = regularHours * REGULAR_HOUR_RATE;
        double nightSurchargePay = nightHours * NIGHT_SURCHARGE_RATE;
        double dayOvertimePay = dayOvertimeHours * DAY_OVERTIME_RATE;
        double nightOvertimePay = nightOvertimeHours * NIGHT_OVERTIME_RATE;
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

    public PayrollCalculation calculateWeeklyPayroll(LocalDate date) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        LocalDate weekStart = date.minusDays(6);
        List<AttendanceDomain> weeklyAttendances = attendance.stream()
                .filter(a -> !a.getDate().isBefore(weekStart) && !a.getDate().isAfter(date))
                .filter(AttendanceDomain::isComplete)
                .toList();

        return calculatePayrollForPeriod(weeklyAttendances);
    }

    public PayrollCalculation calculateMonthlyPayroll(int month, int year) {
        if (attendance == null || attendance.isEmpty()) {
            return PayrollCalculation.builder().build();
        }

        List<AttendanceDomain> monthlyAttendances = attendance.stream()
                .filter(a -> a.getDate() != null)
                .filter(a -> a.getDate().getMonthValue() == month && a.getDate().getYear() == year)
                .filter(AttendanceDomain::isComplete)
                .toList();

        return calculatePayrollForPeriod(monthlyAttendances);
    }

    private PayrollCalculation calculatePayrollForPeriod(List<AttendanceDomain> attendances) {
        double totalRegularHours = 0;
        double totalDayOvertimeHours = 0;
        double totalNightOvertimeHours = 0;
        double totalNightHours = 0;
        double totalRegularPay = 0;
        double totalNightSurchargePay = 0;
        double totalDayOvertimePay = 0;
        double totalNightOvertimePay = 0;

        // Calcular directamente desde las asistencias pasadas como parámetro
        // sin volver a buscar en la lista global para evitar duplicaciones
        for (AttendanceDomain att : attendances) {
            if (att == null || !att.isComplete()) {
                continue;
            }

            double workedHours = att.getWorkedHours();
            double regularHours = Math.min(workedHours, REGULAR_WORK_HOURS);
            double nightHours = att.getNightHours();

            // Calcular horas extras directamente de esta asistencia
            double dayOvertimeHours = calculateDayOvertimeHoursFromAttendance(att);
            double nightOvertimeHours = calculateNightOvertimeHoursFromAttendance(att);

            // Acumular horas
            totalRegularHours += regularHours;
            totalDayOvertimeHours += dayOvertimeHours;
            totalNightOvertimeHours += nightOvertimeHours;
            totalNightHours += nightHours;

            // Calcular pagos
            totalRegularPay += regularHours * REGULAR_HOUR_RATE;
            totalNightSurchargePay += nightHours * NIGHT_SURCHARGE_RATE;
            totalDayOvertimePay += dayOvertimeHours * DAY_OVERTIME_RATE;
            totalNightOvertimePay += nightOvertimeHours * NIGHT_OVERTIME_RATE;
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
     * MÉTODO RENOMBRADO Y MODIFICADO: Calcula horas extras diurnas desde una asistencia específica
     * Antes se llamaba calculateDayOvertimeHours y buscaba la asistencia por fecha
     */
    private double calculateDayOvertimeHoursFromAttendance(AttendanceDomain attendance) {
        if (attendance == null || !attendance.isComplete()) {
            return 0.0;
        }

        double workedHours = attendance.getWorkedHours();
        double overtimeHours = Math.max(0, workedHours - REGULAR_WORK_HOURS);

        if (overtimeHours == 0) {
            return 0.0;
        }

        LocalDateTime entryTime = attendance.getEntryTime();
        LocalDateTime departureTime = attendance.getDepartureTime();
        LocalDateTime regularWorkEnd = entryTime.plusHours(REGULAR_WORK_HOURS);

        if (regularWorkEnd.isAfter(departureTime)) {
            return 0.0;
        }

        long dayMinutes = 0;
        LocalDateTime current = regularWorkEnd;

        while (current.isBefore(departureTime)) {
            int hour = current.getHour();
            if (hour >= NIGHT_END_HOUR && hour < NIGHT_START_HOUR) {
                dayMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return dayMinutes / 60.0;
    }

    /**
     * MÉTODO RENOMBRADO Y MODIFICADO: Calcula horas extras nocturnas desde una asistencia específica
     * Antes se llamaba calculateNightOvertimeHours y buscaba la asistencia por fecha
     */
    private double calculateNightOvertimeHoursFromAttendance(AttendanceDomain attendance) {
        if (attendance == null || !attendance.isComplete()) {
            return 0.0;
        }

        double workedHours = attendance.getWorkedHours();
        double overtimeHours = Math.max(0, workedHours - REGULAR_WORK_HOURS);

        if (overtimeHours == 0) {
            return 0.0;
        }

        LocalDateTime entryTime = attendance.getEntryTime();
        LocalDateTime departureTime = attendance.getDepartureTime();
        LocalDateTime regularWorkEnd = entryTime.plusHours(REGULAR_WORK_HOURS);

        if (regularWorkEnd.isAfter(departureTime)) {
            return 0.0;
        }

        long nightMinutes = 0;
        LocalDateTime current = regularWorkEnd;

        while (current.isBefore(departureTime)) {
            int hour = current.getHour();
            if (hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR) {
                nightMinutes++;
            }
            current = current.plusMinutes(1);
        }

        return nightMinutes / 60.0;
    }

    /**
     * MÉTODOS DEPRECADOS MANTENIDOS PARA COMPATIBILIDAD
     * Estos métodos ya no se usan en calculatePayrollForPeriod pero se mantienen
     * para uso en calculateDailyPayroll
     */
    private double calculateDayOvertimeHours(LocalDate date) {
        AttendanceDomain attendance = getAttendanceByDate(date);
        return calculateDayOvertimeHoursFromAttendance(attendance);
    }

    private double calculateNightOvertimeHours(LocalDate date) {
        AttendanceDomain attendance = getAttendanceByDate(date);
        return calculateNightOvertimeHoursFromAttendance(attendance);
    }

    private AttendanceDomain getAttendanceByDate(LocalDate date) {
        if (attendance == null || attendance.isEmpty()) {
            return null;
        }
        return attendance.stream()
                .filter(a -> date.equals(a.getDate()))
                .findFirst()
                .orElse(null);
    }
}