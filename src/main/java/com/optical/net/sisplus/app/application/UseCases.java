package com.optical.net.sisplus.app.application;

import java.time.LocalDate;

public interface UseCases {
    void registrarUsuario(String nombre, String apellido, String cc);
    void registrarHuella(Long usuarioId, byte[] templateHuella);
    void marcarAsistencia(Long usuarioId);
    void marcarSalida(Long usuarioId);
    double calcularHorasExtras(Long usuarioId, LocalDate fecha);
}