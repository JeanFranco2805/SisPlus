package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {
    private Long userId;

    private String name;
    private String lastName;
    private String cc;

    private double horasExtrasDiarias;
    private double horasExtrasSemanales;
    private double horasExtrasMensuales;

    private long horasNocturnas;

    private double pagoOrdinarioDiurnoDiario;
    private double pagoOrdinarioDiurnoSemanal;
    private double pagoOrdinarioDiurnoMensual;
    private double pagoRecargoNocturno;
}
