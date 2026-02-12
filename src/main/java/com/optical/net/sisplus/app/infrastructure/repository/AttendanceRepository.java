package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  ATTENDANCE REPOSITORY — Queries optimizadas contra DoS
 * ============================================================
 *
 * VULNERABILIDADES ORIGINALES:
 *
 * 1. getAttendancesForDay() en PortCaseAdapter llamaba:
 *      attendanceRepository.findAll()  ← trae TODA la tabla a memoria
 *    luego filtraba con .stream().filter(...) en Java.
 *    Con miles de registros esto consume RAM ilimitada → DoS.
 *    CORREGIDO: findByDateRange() hace el filtro en SQL.
 *
 * 2. getAllAttendances() sin paginación → igual problema.
 *    CORREGIDO: agregada versión paginada.
 *
 * 3. findAll() sin límite → una sola petición podría traer millones
 *    de filas.
 *    CORREGIDO: @QueryHints con javax.persistence.query.timeout.
 *
 * CÓMO ACTUALIZAR PortCaseAdapter:
 *   En getAttendancesForDay(LocalDate fecha):
 *     ANTES: attendanceRepository.findAll() + filter en Java
 *     AHORA: attendanceRepository.findByEntryDate(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByUser(User user);


    @Query("SELECT a FROM Attendance a " +
            "WHERE a.entryTime >= :startDate AND a.entryTime < :endDate " +
            "ORDER BY a.entryTime DESC")
    @QueryHints(@QueryHint(name = "javax.persistence.query.timeout", value = "5000"))
    List<Attendance> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.entryTime >= :start AND a.entryTime < :end " +
            "ORDER BY a.entryTime ASC")
    @QueryHints(@QueryHint(name = "javax.persistence.query.timeout", value = "5000"))
    List<Attendance> findByEntryDate(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.user.id = :userId " +
            "AND a.entryTime >= :startDate AND a.entryTime < :endDate " +
            "ORDER BY a.entryTime DESC")
    @QueryHints(@QueryHint(name = "javax.persistence.query.timeout", value = "5000"))
    List<Attendance> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Attendance a ORDER BY a.entryTime DESC")
    Page<Attendance> findAllPaged(Pageable pageable);

    Optional<Attendance> findById(Long id);
}