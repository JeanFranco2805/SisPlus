package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.response.UserResponseMapper;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    private final PortAdapter portAdapter;

    public UserService(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    public List<UserResponse> getAllUsers() {
        return portAdapter.obtenerTodosUsuarios()
                .stream()
                .map(UserResponseMapper::toUserResponse)  // <- este ahora es compatible
                .toList();
    }
    public UserResponse createUser(UserRequest request) {
        UserDomain user = UserDomain.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
                .build();

        UserDomain saved = portAdapter.guardarUsuario(user);
        return UserResponseMapper.toUserResponse(saved);
    }


    public UserResponse calculatePayroll(Long id, LocalDate date, Integer month, Integer year) {
        UserDomain user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int targetMonth = month != null ? month : targetDate.getMonthValue();
        int targetYear = year != null ? year : targetDate.getYear();

        return UserResponseMapper.fromDomain(user, targetDate, targetMonth, targetYear);
    }

    public double getDailyExtraHours(Long id, LocalDate date) {
        UserDomain user = portAdapter.buscarUsuarioPorId(id);
        return user.calcularHorasExtrasDiarias(date != null ? date : LocalDate.now());
    }

    public double getWeeklyExtraHours(Long id, LocalDate date) {
        UserDomain user = portAdapter.buscarUsuarioPorId(id);
        return user.calcularHorasExtrasSemanales(date != null ? date : LocalDate.now());
    }

    public double getMonthlyExtraHours(Long id, Integer month, Integer year) {
        UserDomain user = portAdapter.buscarUsuarioPorId(id);
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return user.calcularHorasExtrasMensuales(m, y);
    }

    public double getNightSurcharge(Long id, LocalDate date) {
        UserDomain user = portAdapter.buscarUsuarioPorId(id);
        return user.calcularPagoRecargoNocturno(date != null ? date : LocalDate.now());
    }

    public void registerEntry(Long id) {
        portAdapter.registrarEntrada(id);
    }

    public void registerExit(Long id) {
        portAdapter.registrarSalida(id);
    }
}
