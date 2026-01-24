package com.optical.net.sisplus.app.infrastructure.adapter;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.Configuration;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.AttendanceMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.UserMapper;
import com.optical.net.sisplus.app.infrastructure.repository.AttendanceRepository;
import com.optical.net.sisplus.app.infrastructure.repository.ConfigurationRepository;
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

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");
    private final ConfigurationRepository configurationRepository;

    public PortCaseAdapter(UserRepository userRepository, UserMapper userMapper,
                           AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper, ConfigurationRepository configurationRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
        this.configurationRepository = configurationRepository;
    }

    @Override
    public UserDomain saveUser(UserDomain userDomain) {
        var savedUser = userRepository.save(userMapper.toEntity(userDomain));
        return userMapper.toDomain(savedUser);
    }

    @Override
    public UserDomain findUserById(Long usuarioId) {
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
    public List<UserDomain> getAllUsers() {
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
    public void deleteUser(Long id) {
        userRepository.removeById(id);
    }

    @Override
    public void registerAttendance(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow(
                () -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId)
        );

        LocalDateTime ahora = LocalDateTime.now(COLOMBIA_ZONE);

        var attendances = attendanceRepository.findByUser(user);
        boolean yaRegistroHoy = attendances.stream()
                .anyMatch(att -> att.getEntryTime() != null &&
                        att.getEntryTime().toLocalDate().equals(ahora.toLocalDate()));

        if (yaRegistroHoy) {
            registerDeparture(usuarioId);
            return;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .entryTime(ahora)
                .build();

        attendanceRepository.save(attendance);
    }

    @Override
    public void registerDeparture(Long usuarioId) {
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
    public AttendanceDomain getAttendanceForDay(Long usuarioId, LocalDate fecha) {
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
    public AttendanceDomain getAttendanceById(Long attendanceId) {
        var attendance = attendanceRepository.findById(attendanceId).orElseThrow(
                () -> new RuntimeException("Asistencia no encontrada con ID: " + attendanceId)
        );
        return attendanceMapper.toDomain(attendance);
    }

    @Override
    public List<AttendanceDomain> getAttendancesForDay(LocalDate fecha) {
        List<Attendance> allAttendances = attendanceRepository.findAll();

        return allAttendances.stream()
                .filter(att -> att.getEntryTime() != null &&
                        att.getEntryTime().toLocalDate().equals(fecha))
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> getAllAttendances() {
        return attendanceRepository.findAll()
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> getAttendancesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDomain> getAttendancesByUserAndDateRange(Long usuarioId, LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByUserAndDateRange(usuarioId, startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceDomain updateAttendance(Long attendanceId, LocalDateTime entryTime, LocalDateTime departureTime) {
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
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new RuntimeException("Asistencia no encontrada con ID: " + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }

    @Override
    public ConfigurationDomain saveConfig(ConfigurationDomain config) {
        var configuration = configurationRepository.save(Configuration.builder()
                .value(config.getValue())
                .key(config.getKey())
                .build());
        return ConfigurationDomain.builder()
                .key(configuration.getKey())
                .id(configuration.getId())
                .value(configuration.getValue())
                .build();
    }

    @Override
    public ConfigurationDomain getConfig(String key) {
        var config = configurationRepository.findByKey(key).orElseThrow();
        return ConfigurationDomain.builder()
                .value(config.getValue())
                .id(config.getId())
                .key(config.getKey())
                .build();
    }

    @Override
    public List<ConfigurationDomain> getAllConfig() {
        var entities = configurationRepository.findAll();
        return entities.stream().map(e -> ConfigurationDomain.builder()
                .id(e.getId())
                .value(e.getValue())
                .key(e.getKey())
                .build()).toList();
    }

    @Override
    public ConfigurationDomain updateConfig(String key, String value) {
        var config = getConfig(key);
        config.setValue(value);
        configurationRepository.save(Configuration.builder()
                .id(config.getId())
                .key(config.getKey())
                .value(config.getValue())
                .build());
        return config;
    }
}