package com.optical.net.sisplus.repository;

import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.User;
import com.optical.net.sisplus.app.infrastructure.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AttendanceRepository - Tests de Queries")
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Juan")
                .lastName("Pérez")
                .cc("1234567890")
                .status(true)
                .build();
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Debe guardar asistencia con entrada y salida")
    void shouldSaveAttendanceWithEntryAndExit() {
        // Given
        Attendance attendance = Attendance.builder()
                .user(testUser)
                .entryTime(LocalDateTime.of(2026, 1, 25, 8, 0))
                .departureTime(LocalDateTime.of(2026, 1, 25, 16, 0))
                .build();

        // When
        Attendance saved = attendanceRepository.save(attendance);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEntryTime()).isNotNull();
        assertThat(saved.getDepartureTime()).isNotNull();
    }

    @Test
    @DisplayName("Debe permitir guardar solo entrada sin salida")
    void shouldAllowSavingOnlyEntry() {
        // Given
        Attendance attendance = Attendance.builder()
                .user(testUser)
                .entryTime(LocalDateTime.of(2026, 1, 25, 8, 0))
                .departureTime(null)
                .build();

        // When
        Attendance saved = attendanceRepository.save(attendance);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEntryTime()).isNotNull();
        assertThat(saved.getDepartureTime()).isNull();
    }

    @Test
    @DisplayName("Debe encontrar asistencias por usuario")
    void shouldFindAttendancesByUser() {
        // Given
        createAttendance(testUser, LocalDateTime.of(2026, 1, 25, 8, 0), LocalDateTime.of(2026, 1, 25, 16, 0));
        createAttendance(testUser, LocalDateTime.of(2026, 1, 26, 8, 0), LocalDateTime.of(2026, 1, 26, 16, 0));

        User anotherUser = User.builder()
                .name("María")
                .lastName("García")
                .cc("9876543210")
                .status(true)
                .build();
        entityManager.persist(anotherUser);
        createAttendance(anotherUser, LocalDateTime.of(2026, 1, 25, 9, 0), LocalDateTime.of(2026, 1, 25, 17, 0));

        // When
        List<Attendance> attendances = attendanceRepository.findByUser(testUser);

        // Then
        assertThat(attendances).hasSize(2);
        assertThat(attendances).allMatch(a -> a.getUser().getId().equals(testUser.getId()));
    }

    @Test
    @DisplayName("Debe encontrar asistencias por rango de fechas")
    void shouldFindAttendancesByDateRange() {
        // Given
        LocalDateTime start = LocalDateTime.of(2026, 1, 20, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 27, 0, 0);

        // Asistencias dentro del rango
        createAttendance(testUser, LocalDateTime.of(2026, 1, 25, 8, 0), LocalDateTime.of(2026, 1, 25, 16, 0));
        createAttendance(testUser, LocalDateTime.of(2026, 1, 26, 8, 0), LocalDateTime.of(2026, 1, 26, 16, 0));

        // Asistencia fuera del rango
        createAttendance(testUser, LocalDateTime.of(2026, 1, 15, 8, 0), LocalDateTime.of(2026, 1, 15, 16, 0));
        createAttendance(testUser, LocalDateTime.of(2026, 1, 28, 8, 0), LocalDateTime.of(2026, 1, 28, 16, 0));

        // When
        List<Attendance> attendances = attendanceRepository.findByDateRange(start, end);

        // Then
        assertThat(attendances).hasSize(2);
        assertThat(attendances).allMatch(a ->
                !a.getEntryTime().isBefore(start) && a.getEntryTime().isBefore(end)
        );
    }

    @Test
    @DisplayName("Debe encontrar asistencias por usuario y rango de fechas")
    void shouldFindAttendancesByUserAndDateRange() {
        // Given
        User user1 = testUser;
        User user2 = User.builder()
                .name("Carlos")
                .lastName("Rodríguez")
                .cc("5555555555")
                .status(true)
                .build();
        entityManager.persist(user2);

        LocalDateTime start = LocalDateTime.of(2026, 1, 20, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 27, 0, 0);

        // Asistencias de user1 en rango
        createAttendance(user1, LocalDateTime.of(2026, 1, 25, 8, 0), LocalDateTime.of(2026, 1, 25, 16, 0));
        createAttendance(user1, LocalDateTime.of(2026, 1, 26, 8, 0), LocalDateTime.of(2026, 1, 26, 16, 0));

        // Asistencias de user2 en rango (no deberían aparecer)
        createAttendance(user2, LocalDateTime.of(2026, 1, 25, 8, 0), LocalDateTime.of(2026, 1, 25, 16, 0));

        // Asistencia de user1 fuera de rango
        createAttendance(user1, LocalDateTime.of(2026, 1, 15, 8, 0), LocalDateTime.of(2026, 1, 15, 16, 0));

        // When
        List<Attendance> attendances = attendanceRepository.findByUserAndDateRange(
                user1.getId(), start, end
        );

        // Then
        assertThat(attendances).hasSize(2);
        assertThat(attendances).allMatch(a -> a.getUser().getId().equals(user1.getId()));
        assertThat(attendances).allMatch(a ->
                !a.getEntryTime().isBefore(start) && a.getEntryTime().isBefore(end)
        );
    }

    @Test
    @DisplayName("Debe ordenar resultados por fecha de entrada descendente")
    void shouldOrderByEntryTimeDescending() {
        // Given
        createAttendance(testUser, LocalDateTime.of(2026, 1, 23, 8, 0), LocalDateTime.of(2026, 1, 23, 16, 0));
        createAttendance(testUser, LocalDateTime.of(2026, 1, 25, 8, 0), LocalDateTime.of(2026, 1, 25, 16, 0));
        createAttendance(testUser, LocalDateTime.of(2026, 1, 24, 8, 0), LocalDateTime.of(2026, 1, 24, 16, 0));

        LocalDateTime start = LocalDateTime.of(2026, 1, 20, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 27, 0, 0);

        // When
        List<Attendance> attendances = attendanceRepository.findByDateRange(start, end);

        // Then
        assertThat(attendances).hasSize(3);
        assertThat(attendances.get(0).getEntryTime()).isAfter(attendances.get(1).getEntryTime());
        assertThat(attendances.get(1).getEntryTime()).isAfter(attendances.get(2).getEntryTime());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si no hay asistencias en el rango")
    void shouldReturnEmptyListWhenNoAttendancesInRange() {
        // Given
        createAttendance(testUser, LocalDateTime.of(2026, 1, 15, 8, 0), LocalDateTime.of(2026, 1, 15, 16, 0));

        LocalDateTime start = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 28, 0, 0);

        // When
        List<Attendance> attendances = attendanceRepository.findByDateRange(start, end);

        // Then
        assertThat(attendances).isEmpty();
    }

    @Test
    @DisplayName("Debe actualizar hora de salida de asistencia existente")
    void shouldUpdateDepartureTime() {
        // Given
        Attendance attendance = createAttendance(
                testUser,
                LocalDateTime.of(2026, 1, 25, 8, 0),
                null
        );
        Long attendanceId = attendance.getId();

        // When
        attendance.setDepartureTime(LocalDateTime.of(2026, 1, 25, 16, 30));
        attendanceRepository.save(attendance);
        entityManager.flush();
        entityManager.clear();

        // Then
        Attendance updated = attendanceRepository.findById(attendanceId).orElseThrow();
        assertThat(updated.getDepartureTime()).isEqualTo(LocalDateTime.of(2026, 1, 25, 16, 30));
    }

    @Test
    @DisplayName("Debe eliminar asistencia")
    void shouldDeleteAttendance() {
        // Given
        Attendance attendance = createAttendance(
                testUser,
                LocalDateTime.of(2026, 1, 25, 8, 0),
                LocalDateTime.of(2026, 1, 25, 16, 0)
        );
        Long attendanceId = attendance.getId();

        // When
        attendanceRepository.deleteById(attendanceId);
        entityManager.flush();

        // Then
        assertThat(attendanceRepository.findById(attendanceId)).isEmpty();
    }

    // Helper method
    private Attendance createAttendance(User user, LocalDateTime entry, LocalDateTime departure) {
        Attendance attendance = Attendance.builder()
                .user(user)
                .entryTime(entry)
                .departureTime(departure)
                .build();
        entityManager.persist(attendance);
        entityManager.flush();
        return attendance;
    }
}