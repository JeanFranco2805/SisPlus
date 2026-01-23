package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUser(User user);

    @Query("SELECT a FROM Attendance a WHERE a.entryTime >= :startDate AND a.entryTime < :endDate ORDER BY a.entryTime DESC")
    List<Attendance> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.entryTime >= :startDate AND a.entryTime < :endDate ORDER BY a.entryTime DESC")
    List<Attendance> findByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Attendance a WHERE DATE(a.entryTime) = CURRENT_DATE ORDER BY a.entryTime DESC")
    List<Attendance> findTodayAttendances();

    Optional<Attendance> findById(Long id);
}