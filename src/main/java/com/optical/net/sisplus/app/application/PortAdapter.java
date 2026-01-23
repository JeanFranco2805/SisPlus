package com.optical.net.sisplus.app.application;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.UserDomain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PortAdapter {
    // Usuarios
    UserDomain guardarUsuario(UserDomain userDomain);
    UserDomain buscarUsuarioPorId(Long usuarioId);
    List<UserDomain> obtenerTodosUsuarios();

    // Huellas
    void guardarHuella(FootPrintsDomain footPrintsDomain);
    UserDomain identificarUsuarioPorHuella(byte[] templateHuella);

    // Asistencias - CRUD
    void registrarEntrada(Long usuarioId);
    void registrarSalida(Long usuarioId);
    AttendanceDomain obtenerAsistenciaDelDia(Long usuarioId, LocalDate fecha);
    AttendanceDomain obtenerAsistenciaPorId(Long attendanceId);
    List<AttendanceDomain> obtenerAsistenciasDelDia(LocalDate fecha);
    List<AttendanceDomain> obtenerTodasLasAsistencias();
    List<AttendanceDomain> obtenerAsistenciasPorRangoFechas(LocalDateTime startDate, LocalDateTime endDate);
    List<AttendanceDomain> obtenerAsistenciasPorUsuarioYRango(Long usuarioId, LocalDateTime startDate, LocalDateTime endDate);
    AttendanceDomain actualizarAsistencia(Long attendanceId, LocalDateTime entryTime, LocalDateTime departureTime);
    void eliminarAsistencia(Long attendanceId);
}