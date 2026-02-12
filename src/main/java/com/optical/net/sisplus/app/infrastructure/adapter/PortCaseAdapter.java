package com.optical.net.sisplus.app.infrastructure.adapter;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.domain.exception.DomainExceptions;
import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.Configuration;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.AdminMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.AttendanceMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.domains.UserMapper;
import com.optical.net.sisplus.app.infrastructure.repository.AdminRepository;
import com.optical.net.sisplus.app.infrastructure.repository.AttendanceRepository;
import com.optical.net.sisplus.app.infrastructure.repository.ConfigurationRepository;
import com.optical.net.sisplus.app.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PortCaseAdapter implements PortAdapter {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final ConfigurationRepository configurationRepository;
    private final AdminRepository adminRepository;
    private final AdminMapper adminMapper;

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    public PortCaseAdapter(UserRepository userRepository,
                           UserMapper userMapper,
                           AttendanceRepository attendanceRepository,
                           AttendanceMapper attendanceMapper,
                           ConfigurationRepository configurationRepository,
                           AdminRepository adminRepository,
                           AdminMapper adminMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
        this.configurationRepository = configurationRepository;
        this.adminRepository = adminRepository;
        this.adminMapper = adminMapper;
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDomain saveUser(UserDomain userDomain) {
        var savedUser = userRepository.save(userMapper.toEntity(userDomain));
        return userMapper.toDomain(savedUser);
    }

    @Override
    @Cacheable(value = "userById", key = "#usuarioId")
    @Transactional(readOnly = true)
    public UserDomain findUserById(Long usuarioId) {
        var user = userRepository.findByIdWithAttendances(usuarioId)
                .orElseThrow(() -> new DomainExceptions(usuarioId));

        var userDomain = userMapper.toDomain(user);
        List<AttendanceDomain> attendanceDomains = user.getAttendances() != null
                ? user.getAttendances().stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList())
                : new ArrayList<>();
        userDomain.setAttendance(attendanceDomains);
        return userDomain;
    }

    @Override
    @Cacheable(value = "users")
    @Transactional(readOnly = true)
    public List<UserDomain> getAllUsers() {
        return userRepository.findAllWithoutAttendances()
                .stream()
                .map(user -> {
                    var userDomain = userMapper.toDomain(user);
                    userDomain.setAttendance(new ArrayList<>());
                    return userDomain;
                })
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userById", key = "#id")
    })
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new DomainExceptions(id);
        }
        userRepository.removeById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "todayAttendances", allEntries = true)
    public void registerAttendance(Long usuarioId) {
        var user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new DomainExceptions(usuarioId));

        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);
        LocalDate today = now.toLocalDate();

        boolean alreadyCheckedInToday = attendanceRepository
                .existsByUserAndEntryTimeDate(user, today);

        if (alreadyCheckedInToday) {
            registerDeparture(usuarioId);
            return;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .entryTime(now)
                .build();

        attendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    @CacheEvict(value = "todayAttendances", allEntries = true)
    public void registerDeparture(Long usuarioId) {
        var user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new DomainExceptions(usuarioId));

        LocalDateTime now = LocalDateTime.now(COLOMBIA_ZONE);

        var attendance = attendanceRepository
                .findPendingDepartureByUserAndDate(user, now.toLocalDate())
                .orElseThrow(() -> new RuntimeException(
                        "No pending check-in found for today"
                ));

        attendance.setDepartureTime(now);
        attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDomain getAttendanceForDay(Long usuarioId, LocalDate fecha) {
        var user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new DomainExceptions(usuarioId));

        return attendanceRepository.findByUserAndDate(user, fecha)
                .map(attendanceMapper::toDomain)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDomain getAttendanceById(Long attendanceId) {
        return attendanceRepository.findById(attendanceId)
                .map(attendanceMapper::toDomain)
                .orElseThrow(() -> new RuntimeException(
                        "Attendance not found with id: " + attendanceId
                ));
    }

    @Override
    @Cacheable(value = "todayAttendances", key = "#fecha")
    @Transactional(readOnly = true)
    public List<AttendanceDomain> getAttendancesForDay(LocalDate fecha) {
        return attendanceRepository.findByDate(fecha)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDomain> getAllAttendances() {
        return attendanceRepository.findAll()
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDomain> getAttendancesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDomain> getAttendancesByUserAndDateRange(Long usuarioId, LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceRepository.findByUserAndDateRange(usuarioId, startDate, endDate)
                .stream()
                .map(attendanceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "todayAttendances", allEntries = true)
    public AttendanceDomain updateAttendance(Long attendanceId, LocalDateTime entryTime, LocalDateTime departureTime) {
        var attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException(
                        "Attendance not found with id: " + attendanceId
                ));

        if (entryTime != null) {
            attendance.setEntryTime(entryTime);
        }
        if (departureTime != null) {
            if (attendance.getEntryTime() != null && departureTime.isBefore(attendance.getEntryTime())) {
                throw new RuntimeException("Departure time cannot be before entry time");
            }
            attendance.setDepartureTime(departureTime);
        }

        return attendanceMapper.toDomain(attendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    @CacheEvict(value = "todayAttendances", allEntries = true)
    public void deleteAttendance(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new RuntimeException("Attendance not found with id: " + attendanceId);
        }
        attendanceRepository.deleteById(attendanceId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "payrollConfig", key = "#config.key")
    public ConfigurationDomain saveConfig(ConfigurationDomain config) {
        var existing = configurationRepository.findByKey(config.getKey());
        Configuration entity;
        if (existing.isPresent()) {
            existing.get().setValue(config.getValue());
            entity = configurationRepository.save(existing.get());
        } else {
            entity = configurationRepository.save(
                    Configuration.builder()
                            .key(config.getKey())
                            .value(config.getValue())
                            .build()
            );
        }
        return ConfigurationDomain.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .value(entity.getValue())
                .build();
    }

    @Override
    @Cacheable(value = "payrollConfig", key = "#key")
    @Transactional(readOnly = true)
    public ConfigurationDomain getConfig(String key) {
        var config = configurationRepository.findByKey(key)
                .orElseThrow(() -> new RuntimeException(
                        "Configuration not found for key: " + key
                ));
        return ConfigurationDomain.builder()
                .id(config.getId())
                .key(config.getKey())
                .value(config.getValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfigurationDomain> getAllConfig() {
        return configurationRepository.findAll().stream()
                .map(e -> ConfigurationDomain.builder()
                        .id(e.getId())
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "payrollConfig", key = "#key")
    public ConfigurationDomain updateConfig(String key, String value) {
        var config = getConfig(key);
        config.setValue(value);
        configurationRepository.save(
                Configuration.builder()
                        .id(config.getId())
                        .key(config.getKey())
                        .value(config.getValue())
                        .build()
        );
        return config;
    }

    @Override
    @Transactional
    public AdminDomain save(AdminDomain adminDomain) {
        return adminMapper.toDomain(adminRepository.save(adminMapper.toEntity(adminDomain)));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDomain findByUsername(String username) {
        return adminMapper.toDomain(adminRepository.findByUsername(username));
    }

    @Override
    @Transactional
    public boolean removeAdmin(String username) {
        if (!adminRepository.existsByUsername(username)) {
            return false;
        }
        adminRepository.deleteByUsername(username);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDomain> findAllAdmins() {
        return adminMapper.toDomainsList(adminRepository.findAll());
    }
}