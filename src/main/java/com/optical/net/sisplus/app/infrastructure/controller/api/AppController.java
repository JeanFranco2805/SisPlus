package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.UserResponseMapper;
import com.optical.net.sisplus.app.infrastructure.web.AttendanceRequest;
import com.optical.net.sisplus.app.infrastructure.web.AttendanceResponse;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AppController {

    private final PortAdapter portAdapter;

    public AppController(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    // ========== USUARIOS ==========

    @GetMapping("/users")
    public List<UserResponse> readAll() {
        var usuarios = portAdapter.obtenerTodosUsuarios();
        return usuarios.stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .lastName(user.getLastName())
                        .cc(user.getCc())
                        .build())
                .toList();
    }

    @PostMapping("/users")
    public UserResponse registrarUsuario(@RequestBody UserRequest request) {
        var userDomain = UserDomain.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
                .build();

        var savedUser = portAdapter.guardarUsuario(userDomain);

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .lastName(savedUser.getLastName())
                .cc(savedUser.getCc())
                .build();
    }

    // ========== NÓMINA ==========

    @PostMapping("/users/payroll")
    public UserResponse calcularNomina(@RequestBody UserRequest request) {
        var user = portAdapter.buscarUsuarioPorId(request.getId());

        LocalDate date = request.getDate() != null
                ? request.getDate()
                : LocalDate.now();

        int month = request.getMonth() != null
                ? request.getMonth()
                : date.getMonthValue();

        int year = request.getYear() != null
                ? request.getYear()
                : date.getYear();

        return UserResponseMapper.fromDomain(user, date, month, year);
    }

    @GetMapping("/users/{id}/extra-hours/daily")
    public double obtenerHorasExtrasDiarias(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularHorasExtrasDiarias(targetDate);
    }

    @GetMapping("/users/{id}/extra-hours/weekly")
    public double obtenerHorasExtrasSemanales(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularHorasExtrasSemanales(targetDate);
    }

    @GetMapping("/users/{id}/extra-hours/monthly")
    public double obtenerHorasExtrasMensuales(
            @PathVariable Long id,
            @RequestParam int month,
            @RequestParam int year
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        return user.calcularHorasExtrasMensuales(month, year);
    }

    @GetMapping("/users/{id}/night-surcharge")
    public double obtenerRecargoNocturno(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularPagoRecargoNocturno(targetDate);
    }

    // ========== ASISTENCIAS - CREATE ==========

    @PostMapping("/users/{id}/entry")
    public ResponseEntity<Map<String, String>> registrarEntrada(@PathVariable Long id) {
        try {
            portAdapter.registrarEntrada(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Entrada registrada exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/users/{id}/exit")
    public ResponseEntity<Map<String, String>> registrarSalida(@PathVariable Long id) {
        try {
            portAdapter.registrarSalida(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Salida registrada exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========== ASISTENCIAS - READ ==========

    /**
     * Obtener asistencias con filtros
     * @param filter: "today" | "week" | "month" | "all"
     * @param userId: ID del usuario (opcional)
     * @param startDate: Fecha inicio para rango personalizado (opcional)
     * @param endDate: Fecha fin para rango personalizado (opcional)
     */
    @GetMapping("/attendances")
    public List<AttendanceResponse> readAttendances(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) LocalDate date
    ) {
        List<AttendanceDomain> attendances;

        // Si se proporciona una fecha específica (para compatibilidad)
        if (date != null) {
            attendances = portAdapter.obtenerAsistenciasDelDia(date);
        }
        // Si se proporciona un rango de fechas personalizado
        else if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);

            if (userId != null) {
                attendances = portAdapter.obtenerAsistenciasPorUsuarioYRango(userId, start, end);
            } else {
                attendances = portAdapter.obtenerAsistenciasPorRangoFechas(start, end);
            }
        }
        // Si se proporciona un filtro predefinido
        else if (filter != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start;
            LocalDateTime end = now;

            switch (filter.toLowerCase()) {
                case "today":
                    start = now.toLocalDate().atStartOfDay();
                    end = now.toLocalDate().atTime(LocalTime.MAX);
                    break;
                case "week":
                    start = now.minusDays(7).toLocalDate().atStartOfDay();
                    break;
                case "month":
                    start = now.minusDays(30).toLocalDate().atStartOfDay();
                    break;
                case "all":
                    attendances = portAdapter.obtenerTodasLasAsistencias();
                    return mapToAttendanceResponse(attendances);
                default:
                    start = now.toLocalDate().atStartOfDay();
                    end = now.toLocalDate().atTime(LocalTime.MAX);
            }

            if (userId != null) {
                attendances = portAdapter.obtenerAsistenciasPorUsuarioYRango(userId, start, end);
            } else {
                attendances = portAdapter.obtenerAsistenciasPorRangoFechas(start, end);
            }
        }
        // Por defecto, asistencias de hoy
        else {
            LocalDate today = LocalDate.now();
            attendances = portAdapter.obtenerAsistenciasDelDia(today);
        }

        return mapToAttendanceResponse(attendances);
    }

    @GetMapping("/attendances/{id}")
    public AttendanceResponse obtenerAsistenciaPorId(@PathVariable Long id) {
        AttendanceDomain attendance = portAdapter.obtenerAsistenciaPorId(id);
        return mapSingleAttendance(attendance);
    }

    // ========== ASISTENCIAS - UPDATE ==========

    @PutMapping("/attendances/{id}")
    public ResponseEntity<AttendanceResponse> actualizarAsistencia(
            @PathVariable Long id,
            @RequestBody AttendanceRequest request
    ) {
        try {
            AttendanceDomain updated = portAdapter.actualizarAsistencia(
                    id,
                    request.getEntryTime(),
                    request.getDepartureTime()
            );

            return ResponseEntity.ok(mapSingleAttendance(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== ASISTENCIAS - DELETE ==========

    @DeleteMapping("/attendances/{id}")
    public ResponseEntity<Map<String, String>> eliminarAsistencia(@PathVariable Long id) {
        try {
            portAdapter.eliminarAsistencia(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Asistencia eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // ========== HELPER METHODS ==========

    private List<AttendanceResponse> mapToAttendanceResponse(List<AttendanceDomain> attendances) {
        return attendances.stream()
                .map(this::mapSingleAttendance)
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapSingleAttendance(AttendanceDomain att) {
        double horasExtras = 0;
        if (att.getDepartureTime() != null && att.getEntryTime() != null) {
            double horasTrabajadas = att.getWorkedHours();
            horasExtras = Math.max(0, horasTrabajadas - 8);
        }

        return AttendanceResponse.builder()
                .id(att.getUser().getId())
                .user(UserResponse.builder()
                        .id(att.getUser().getId())
                        .name(att.getUser().getName())
                        .lastName(att.getUser().getLastName())
                        .cc(att.getUser().getCc())
                        .build())
                .entryTime(att.getEntryTime())
                .departureTime(att.getDepartureTime())
                .workedHours(att.getDepartureTime() != null ? att.getWorkedHours() : 0)
                .nightHours(att.getDepartureTime() != null ? att.getNightHours() : 0)
                .extraHours(horasExtras)
                .build();
    }
}