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
    private Long id;
    private String name;
    private String lastName;
    private String cc;
    private double salary;
    private List<AttendanceDomain> attendance;

    private double effectiveRegularRate(PayrollConfiguration config) {
        if (salary > 0) return salary / 240.0;
        return config.getRegularHourRate();
    }

    private double effectiveDayOvertimeRate(PayrollConfiguration config) {
        if (salary > 0) return (salary / 240.0) * 1.25;
        return config.getDayOvertimeRate();
    }

    private double effectiveNightSurchargeRate(PayrollConfiguration config) {
        if (salary > 0) return (salary / 240.0) * 0.35;
        return config.getNightSurchargeRate();
    }

    private double effectiveNightOvertimeRate(PayrollConfiguration config) {
        if (salary > 0) return (salary / 240.0) * 1.75;
        return config.getNightOvertimeRate();
    }

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

        double dayOvertimeHours = calculateDayOvertimeHoursFromAttendance(todayAttendance, config);
        double nightOvertimeHours = calculateNightOvertimeHoursFromAttendance(todayAttendance, config);
        double nightHours = todayAttendance.getNightHours(config);

        double regularPay = regularHours * effectiveRegularRate(config);
        double nightSurchargePay = nightHours * effectiveNightSurchargeRate(config);
        double dayOvertimePay = dayOvertimeHours * effectiveDayOvertimeRate(config);
        double nightOvertimePay = nightOvertimeHours * effectiveNightOvertimeRate(config);
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

            totalRegularPay += regularHours * effectiveRegularRate(config);
            totalNightSurchargePay += nightHours * effectiveNightSurchargeRate(config);
            totalDayOvertimePay += dayOvertimeHours * effectiveDayOvertimeRate(config);
            totalNightOvertimePay += nightOvertimeHours * effectiveNightOvertimeRate(config);
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

    private AttendanceDomain getAttendanceByDate(LocalDate date) {
        if (attendance == null || attendance.isEmpty()) {
            return null;
        }
        return attendance.stream()
                .filter(a -> date.equals(a.getDate()))
                .findFirst()
                .orElse(null);
    }

    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateDailyPayroll(LocalDate date) {
        return calculateDailyPayroll(date, PayrollConfiguration.defaults());
    }

    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateWeeklyPayroll(LocalDate date) {
        return calculateWeeklyPayroll(date, PayrollConfiguration.defaults());
    }

    @Deprecated(since = "1.1", forRemoval = true)
    public PayrollCalculation calculateMonthlyPayroll(int month, int year) {
        return calculateMonthlyPayroll(month, year, PayrollConfiguration.defaults());
    }
}