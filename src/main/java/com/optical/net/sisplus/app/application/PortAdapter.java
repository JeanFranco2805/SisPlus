package com.optical.net.sisplus.app.application;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.UserDomain;

import java.time.LocalDate;
import java.util.Optional;

public interface PortAdapter {
    UserDomain guardarUsuario(UserDomain userDomain);
    UserDomain buscarUsuarioPorId(Long usuarioId);
    void guardarHuella(FootPrintsDomain footPrintsDomain);
    UserDomain identificarUsuarioPorHuella(byte[] templateHuella);
    AttendanceDomain obtenerAsistenciaDelDia(Long usuarioId, LocalDate fecha);
    void registrarEntrada(Long usuarioId);
    void registrarSalida(Long usuarioId);
}
