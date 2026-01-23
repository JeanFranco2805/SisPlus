package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.response.AttendanceResponseMapper;
import com.optical.net.sisplus.app.infrastructure.web.AttendanceRequest;
import com.optical.net.sisplus.app.infrastructure.web.AttendanceResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    private final PortAdapter portAdapter;

    public AttendanceService(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    public List<AttendanceResponse> getAttendances(
            String filter,
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate date
    ) {
        List<AttendanceDomain> attendances;

        if (date != null) {
            attendances = portAdapter.obtenerAsistenciasDelDia(date);
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);

            attendances = (userId != null)
                    ? portAdapter.obtenerAsistenciasPorUsuarioYRango(userId, start, end)
                    : portAdapter.obtenerAsistenciasPorRangoFechas(start, end);
        } else if (filter != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start;
            LocalDateTime end = now;

            switch (filter.toLowerCase()) {
                case "week" -> start = now.minusDays(7).toLocalDate().atStartOfDay();
                case "month" -> start = now.minusDays(30).toLocalDate().atStartOfDay();
                case "all" -> {
                    attendances = portAdapter.obtenerTodasLasAsistencias();
                    return AttendanceResponseMapper.toResponseList(attendances);
                }
                default -> {
                    start = now.toLocalDate().atStartOfDay();
                    end = now.toLocalDate().atTime(LocalTime.MAX);
                }
            }

            attendances = (userId != null)
                    ? portAdapter.obtenerAsistenciasPorUsuarioYRango(userId, start, end)
                    : portAdapter.obtenerAsistenciasPorRangoFechas(start, end);
        } else {
            attendances = portAdapter.obtenerAsistenciasDelDia(LocalDate.now());
        }

        return AttendanceResponseMapper.toResponseList(attendances);
    }

    public AttendanceResponse getAttendanceById(Long id) {
        AttendanceDomain att = portAdapter.obtenerAsistenciaPorId(id);
        return AttendanceResponseMapper.toResponse(att);
    }

    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        AttendanceDomain updated = portAdapter.actualizarAsistencia(
                id,
                request.getEntryTime(),
                request.getDepartureTime()
        );
        return AttendanceResponseMapper.toResponse(updated);
    }

    public void deleteAttendance(Long id) {
        portAdapter.eliminarAsistencia(id);
    }
}

