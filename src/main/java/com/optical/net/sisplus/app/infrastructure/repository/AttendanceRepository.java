package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import com.optical.net.sisplus.app.infrastructure.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Attendance a WHERE a.user = :user " +
            "AND FUNCTION('DATE', a.entryTime) = :date")
    boolean existsByUserAndEntryTimeDate(@Param("user") User user, @Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Attendance a WHERE a.user = :user " +
            "AND FUNCTION('DATE', a.entryTime) = :date " +
            "AND a.departureTime IS NULL " +
            "ORDER BY a.entryTime DESC")
    Optional<Attendance> findPendingDepartureByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.user = :user " +
            "AND FUNCTION('DATE', a.entryTime) = :date")
    Optional<Attendance> findByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE FUNCTION('DATE', a.entryTime) = :date")
    List<Attendance> findByDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.entryTime BETWEEN :startDate AND :endDate")
    List<Attendance> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId " +
            "AND a.entryTime BETWEEN :startDate AND :endDate")
    List<Attendance> findByUserAndDateRange(@Param("userId") Long userId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}