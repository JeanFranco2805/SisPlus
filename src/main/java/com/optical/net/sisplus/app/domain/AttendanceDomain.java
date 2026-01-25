package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class AttendanceDomain implements Comparable<AttendanceDomain> {

    private Long id;
    private UserDomain user;
    private LocalDateTime entryTime;
    private LocalDateTime departureTime;
    private int hoursWorked;

    /**
     * Calcula las horas trabajadas
     * @return Horas trabajadas en formato decimal
     */
    public double getWorkedHours() {
        if (entryTime == null || departureTime == null) return 0.0;
        return Duration.between(entryTime, departureTime).toHours();
    }

    /**
     * Obtiene la fecha de la asistencia
     * @return Fecha de entrada
     */
    public LocalDate getDate() {
        return entryTime != null ? entryTime.toLocalDate() : null;
    }

    /**
     * Calcula las horas nocturnas con configuración específica
     * @param config Configuración de horarios nocturnos
     * @return Horas nocturnas trabajadas
     */
    public double getNightHours(PayrollConfiguration config) {
        if (entryTime == null || departureTime == null) return 0.0;
        if (entryTime.isAfter(departureTime)) return 0.0;

        LocalDateTime startNight = entryTime.withHour(config.getNightStartHour()).withMinute(0).withSecond(0);
        LocalDateTime endNight = entryTime.plusDays(1).withHour(config.getNightEndHour()).withMinute(0).withSecond(0);

        LocalDateTime nightStart = entryTime.isAfter(startNight) ? entryTime : startNight;
        LocalDateTime nightEnd = departureTime.isBefore(endNight) ? departureTime : endNight;

        if (nightStart.isAfter(nightEnd)) return 0.0;

        long minutes = Duration.between(nightStart, nightEnd).toMinutes();
        return minutes / 60.0;
    }

    /**
     * @deprecated Usar {@link #getNightHours(PayrollConfiguration)}
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public double getNightHours() {
        return getNightHours(PayrollConfiguration.defaults());
    }

    /**
     * Verifica si la asistencia está completa (tiene entrada y salida)
     * @return true si está completa
     */
    public boolean isComplete() {
        return entryTime != null && departureTime != null;
    }

    @Override
    public int compareTo(AttendanceDomain o) {
        if (this.getDate() == null || o.getDate() == null) return 0;
        return this.getDate().compareTo(o.getDate());
    }
}