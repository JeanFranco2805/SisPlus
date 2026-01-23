package com.optical.net.sisplus.app.infrastructure.adapter;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.AttendanceMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.FootPrintsMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.UserMapper;
import com.optical.net.sisplus.app.infrastructure.repository.AttendanceRepository;
import com.optical.net.sisplus.app.infrastructure.repository.FootPrintsRepository;
import com.optical.net.sisplus.app.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PortCaseAdapter implements PortAdapter {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final FootPrintsRepository footPrintsRepository;
    private final FootPrintsMapper footPrintsMapper;

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    public PortCaseAdapter(UserRepository userRepository, UserMapper userMapper,
                           AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper,
                           FootPrintsRepository footPrintsRepository, FootPrintsMapper footPrintsMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
        this.footPrintsRepository = footPrintsRepository;
        this.footPrintsMapper = footPrintsMapper;
    }

    @Override
    public UserDomain guardarUsuario(UserDomain userDomain) {
        var savedUser = userRepository.save(userMapper.toEntity(userDomain));
        return userMapper.toDomain(savedUser);
    }

    @Override
    public UserDomain buscarUsuarioPorId(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId)
        );

        var userDomain = userMapper.toDomain(user);

        var attendanceEntities = attendanceRepository.findByUser(user);
        List<AttendanceDomain> attendanceDomains = attendanceEntities.stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());

        userDomain.setAttendance(attendanceDomains != null ? attendanceDomains : new ArrayList<>());

        return userDomain;
    }

    @Override
    public List<UserDomain> obtenerTodosUsuarios() {
        return userRepository.findAll()
                .stream()
                .map(user -> {
                    var userDomain = userMapper.toDomain(user);
                    userDomain.setAttendance(new ArrayList<>());
                    return userDomain;
                })
                .toList();
    }

    @Override
    public void eliminarUsuario(Long id) {
        userRepository.removeById(id);
    }

    @Override
    public void guardarHuella(FootPrintsDomain footPrintsDomain) {
        footPrintsRepository.save(footPrintsMapper.toEntity(footPrintsDomain));
    }

    @Override
    public UserDomain identificarUsuarioPorHuella(byte[] templateHuella) {
        var fps = footPrintsRepository.findByTemplate(templateHuella).getLast();
        var userDomain = userMapper.toDomain(fps.getUser());
        userDomain.setAttendance(new ArrayList<>());
        return userDomain;
    }

    @Override
    public void registrarAsistencia(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId)
        );

        LocalDateTime ahora = LocalDateTime.now(COLOMBIA_ZONE);

        var attendances = attendanceRepository.findByUser(user);
        boolean yaRegistroHoy = attendances.stream()
                .anyMatch(att -> att.getEntryTime() != null &&
                        att.getEntryTime().toLocalDate().equals(ahora.toLocalDate()));

        if (yaRegistroHoy) {
            registrarSalida(usuarioId);
            return;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .entryTime(ahora)
                .build();

        attendanceRepository.save(attendance);
    }

    @Override
    public void registrarSalida(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId)
        );

        LocalDateTime ahora = LocalDateTime.now(COLOMBIA_ZONE);

        var attendances = attendanceRepository.findByUser(user);

        var attendance = attendances.stream()
                .filter(e -> e.getEntryTime() != null &&
                        e.getEntryTime().toLocalDate().equals(ahora.toLocalDate()) &&
                        e.getDepartureTime() == null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró un registro de entrada sin salida para hoy"
                ));

        attendance.setDepartureTime(ahora);
        attendanceRepository.save(attendance);
    }

    @Override
    public AttendanceDomain obtenerAsistenciaDelDia(Long usuarioId, LocalDate fecha) {
        var user = userRepository.findById(usuarioId).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado")
        );

        var attendances = attendanceRepository.findByUser(user);

        return attendances.stream()
                .filter(att -> att.getEntryTime() != null &&
                        att.getEntryTime().toLocalDate().equals(fecha))
                .findFirst()
                .map(attendanceMapper::toDomain)
                .orElse(null);
    }

    @Override
    public AttendanceDomain obtenerAsistenciaPorId(Long attendanceId) {
        var attendance = attendanceRepository.findById(attendanceId).orElseThrow(
                () -> new RuntimeException("Asistencia no encontrada con ID: " + attendanceId)
        );
        return attendanceMapper.toDomain(attendance);
    }

    @Override
    public List<AttendanceDomain> obtenerAsistenciasDelDia(LocalDate fecha) {
        List<Attendance> allAttendances = attendanceRepository.findAll();

        return allAttendances.stream()
                .filter(att -> att.getEntryTime() != null &&
                        att.getEntryTime().toLocalDate().equals(fecha))
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> obtenerTodasLasAsistencias() {
        return attendanceRepository.findAll()
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> obtenerAsistenciasPorRangoFechas(LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> obtenerAsistenciasPorUsuarioYRango(Long usuarioId, LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByUserAndDateRange(usuarioId, startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceDomain actualizarAsistencia(Long attendanceId, LocalDateTime entryTime, LocalDateTime departureTime) {
        var attendance = attendanceRepository.findById(attendanceId).orElseThrow(
                () -> new RuntimeException("Asistencia no encontrada con ID: " + attendanceId)
        );

        if (entryTime != null) {
            attendance.setEntryTime(entryTime);
        }

        if (departureTime != null) {
            if (attendance.getEntryTime() != null && departureTime.isBefore(attendance.getEntryTime())) {
                throw new RuntimeException("La hora de salida no puede ser anterior a la hora de entrada");
            }
            attendance.setDepartureTime(departureTime);
        }

        var updatedAttendance = attendanceRepository.save(attendance);
        return attendanceMapper.toDomain(updatedAttendance);
    }

    @Override
    public void eliminarAsistencia(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new RuntimeException("Asistencia no encontrada con ID: " + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }
}