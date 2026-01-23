package com.optical.net.sisplus.app.application;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.UserDomain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PortAdapter {
    UserDomain saveUser(UserDomain userDomain);
    UserDomain findUserById(Long usuarioId);
    List<UserDomain> getAllUsers();
    void deleteUser(Long id);
    void saveFingerprint(FootPrintsDomain footPrintsDomain);
    UserDomain identifyUserByFingerprint(byte[] templateHuella);

    void registerAttendance(Long usuarioId);
    void registerDeparture(Long usuarioId);
    AttendanceDomain getAttendanceForDay(Long usuarioId, LocalDate fecha);
    AttendanceDomain getAttendanceById(Long attendanceId);
    List<AttendanceDomain> getAttendancesForDay(LocalDate fecha);
    List<AttendanceDomain> getAllAttendances();
    List<AttendanceDomain> getAttendancesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<AttendanceDomain> getAttendancesByUserAndDateRange(Long usuarioId, LocalDateTime startDate, LocalDateTime endDate);
    AttendanceDomain updateAttendance(Long attendanceId, LocalDateTime entryTime, LocalDateTime departureTime);
    void deleteAttendance(Long attendanceId);
}