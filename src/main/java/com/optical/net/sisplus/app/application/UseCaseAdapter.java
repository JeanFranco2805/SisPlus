package com.optical.net.sisplus.app.application;

import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.UserDomain;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UseCaseAdapter implements UseCases {

    private final PortAdapter portAdapter;

    public UseCaseAdapter(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    @Override
    public void registrarUsuario(String nombre, String apellido, String cc) {
        var domain = UserDomain.builder().name(nombre).lastName(apellido).cc(cc).build();
        portAdapter.saveUser(domain);
    }

    @Override
    public void registrarHuella(Long usuarioId, byte[] templateHuella) {
        var usuario = portAdapter.findUserById(usuarioId);
        portAdapter.saveFingerprint(FootPrintsDomain.builder()
                .date(LocalDateTime.now())
                .user(usuario)
                .template(templateHuella)
                .build());
    }

    @Override
    public void marcarAsistencia(Long usuarioId) {
        portAdapter.registerAttendance(usuarioId);
    }

    @Override
    public void marcarSalida(Long usuarioId) {
        portAdapter.registerDeparture(usuarioId);
    }

    @Override
    public double calcularHorasExtras(Long usuarioId, LocalDate fecha) {
        var usuario = portAdapter.findUserById(usuarioId);
        PayrollCalculation payroll = usuario.calculateDailyPayroll(fecha);
        return payroll.getDayOvertimeHours() + payroll.getNightOvertimeHours();
    }
}