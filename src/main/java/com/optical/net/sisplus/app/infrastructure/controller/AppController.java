package com.optical.net.sisplus.app.infrastructure.controller;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.UserResponseMapper;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class AppController {

    private final PortAdapter portAdapter;

    public AppController(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    @PostMapping("/users/payroll")
    public UserResponse calcularNomina(@RequestBody UserRequest request) {

        var user = portAdapter.buscarUsuarioPorId(request.getUserId());

        LocalDate date = request.getDate() != null
                ? request.getDate()
                : LocalDate.now();

        int month = request.getMonth() != null
                ? request.getMonth()
                : date.getMonthValue();

        int year = request.getYear() != null
                ? request.getYear()
                : date.getYear();

        return UserResponseMapper.fromDomain(user, date, month, year);
    }
    @PostMapping("/users")
    public UserResponse registrarUsuario(@RequestBody UserRequest request) {

        var userDomain = UserDomain.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
                .build();

        var savedUser = portAdapter.guardarUsuario(userDomain);

        return UserResponse.builder()
                .name(savedUser.getName())
                .lastName(savedUser.getLastName())
                .cc(savedUser.getCc())
                .build();
    }
    @PostMapping("/users/{id}/entry")
    public void registrarEntrada(@PathVariable Long id) {
        portAdapter.registrarEntrada(id);
    }
    @PostMapping("/users/{id}/exit")
    public void registrarSalida(@PathVariable Long id) {
        portAdapter.registrarSalida(id);
    }
    @GetMapping("/users/{id}/extra-hours/daily")
    public double obtenerHorasExtrasDiarias(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularHorasExtrasDiarias(targetDate);
    }
    @GetMapping("/users/{id}/extra-hours/weekly")
    public double obtenerHorasExtrasSemanales(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularHorasExtrasSemanales(targetDate);
    }
    @GetMapping("/users/{id}/extra-hours/monthly")
    public double obtenerHorasExtrasMensuales(
            @PathVariable Long id,
            @RequestParam int month,
            @RequestParam int year
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        return user.calcularHorasExtrasMensuales(month, year);
    }
    @GetMapping("/users/{id}/night-surcharge")
    public double obtenerRecargoNocturno(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        var user = portAdapter.buscarUsuarioPorId(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return user.calcularPagoRecargoNocturno(targetDate);
    }

}
