package com.optical.net.sisplus.app.infrastructure.mapper;

import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UserResponseMapper {

    public static UserResponse fromDomain(
            UserDomain user,
            LocalDate date,
            int month,
            int year
    ) {
        return UserResponse.builder()
                .name(user.getName())
                .lastName(user.getLastName())
                .cc(user.getCc())
                .horasExtrasDiarias(user.calcularHorasExtrasDiarias(date))
                .horasExtrasSemanales(user.calcularHorasExtrasSemanales(date))
                .horasExtrasMensuales(user.calcularHorasExtrasMensuales(month, year))
                .horasNocturnas(user.calcularHorasNocturnas(date))
                .pagoOrdinarioDiurnoDiario(user.calcularPagoOrdinarioDiurnoDiario(date))
                .pagoOrdinarioDiurnoSemanal(user.calcularPagoOrdinarioDiurnoSemanal(date))
                .pagoOrdinarioDiurnoMensual(user.calcularPagoOrdinarioDiurnoMensual(month, year))
                .pagoRecargoNocturno(user.calcularPagoRecargoNocturno(date))
                .build();
    }

    public static List<UserResponse> fromDomainList(
            List<UserDomain> users,
            LocalDate date,
            int month,
            int year
    ) {
        return users.stream()
                .map(user -> fromDomain(user, date, month, year))
                .collect(Collectors.toList());
    }
}
