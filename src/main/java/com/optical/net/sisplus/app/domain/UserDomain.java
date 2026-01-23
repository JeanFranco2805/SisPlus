package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

@Builder
@Getter
@Setter
public class UserDomain {
    private Long id;
    private String name;
    private String lastName;
    private String cc;
    private List<AttendanceDomain> attendance;


    public double calcularHorasExtrasDiarias(LocalDate date) {
        var today = attendance.stream().filter(e -> e.getDate().equals(date))
                .reduce((first, second) -> second)
                .orElseThrow();
        var entryTime = today.getEntryTime();
        var departureTime = today.getDepartureTime();

        return Duration.between(entryTime, departureTime).toHours() - 8;
    }

    public double calcularHorasExtrasSemanales(LocalDate date) {
        var attedance = attendance.stream().sorted().dropWhile(e -> e.getDate().isBefore(date.minusDays(7)))
                .toList();
        double extrHours = 0;
        for (AttendanceDomain attendanceDomain : attedance) {
            extrHours += calcularHorasExtrasDiarias(attendanceDomain.getDate());
        }
        return extrHours;
    }

    public double calcularHorasExtrasMensuales(int month, int year) {
        LocalDate date = LocalDate.now().withMonth(month).withYear(year);
        Month mes = date.getMonth();
        int dias = switch (mes){
            case JANUARY, MARCH, MAY, JULY, AUGUST, OCTOBER, DECEMBER-> 31;
            case APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30;
            case FEBRUARY -> date.isLeapYear() ? 29 : 28;
        };

        var attedance = attendance.stream().sorted().dropWhile(e -> e.getDate().isBefore(date.minusDays(dias)))
                .toList();
        double extrHours = 0;
        for (AttendanceDomain attendanceDomain : attedance) {
            extrHours += calcularHorasExtrasDiarias(attendanceDomain.getDate());
        }
        return extrHours;
    }
    public double calcularPagoOrdinarioDiurnoDiario(LocalDate date) {
        double HORA_ORDINARIA_DIURNA = 9948;
        return 8 * HORA_ORDINARIA_DIURNA;
    }
    public double calcularPagoOrdinarioDiurnoSemanal(LocalDate date) {
        double HORA_ORDINARIA_DIURNA = 9948;

        var asistenciaSemana = attendance.stream()
                .filter(a -> !a.getDate().isBefore(date.minusDays(6)) &&
                        !a.getDate().isAfter(date))
                .toList();

        return asistenciaSemana.size() * 8 * HORA_ORDINARIA_DIURNA;
    }
    public double calcularPagoOrdinarioDiurnoMensual(int month, int year) {
        double HORA_ORDINARIA_DIURNA = 9948;

        var asistenciaMes = attendance.stream()
                .filter(a -> a.getDate().getMonthValue() == month &&
                        a.getDate().getYear() == year)
                .toList();

        return asistenciaMes.size() * 8 * HORA_ORDINARIA_DIURNA;
    }

    public long calcularHorasNocturnas(LocalDate date) {
        var today = attendance.stream()
                .filter(e -> e.getDate().equals(date))
                .findFirst()
                .orElseThrow();

        LocalDateTime entry = today.getEntryTime();
        LocalDateTime exit = today.getDepartureTime();

        long horasNocturnas = 0;

        for (LocalDateTime t = entry; t.isBefore(exit); t = t.plusHours(1)) {
            int hour = t.getHour();
            if (hour >= 19 || hour < 6) {
                horasNocturnas++;
            }
        }

        return horasNocturnas;
    }

    public double calcularPagoRecargoNocturno(LocalDate date) {
        double RECARGO_NOCTURNO = 2786;

        long horasNocturnas = calcularHorasNocturnas(date);
        return horasNocturnas * RECARGO_NOCTURNO;
    }

    public double calcularRecargoNocturno(LocalDate date) {
        return 0.0;
    }
}
