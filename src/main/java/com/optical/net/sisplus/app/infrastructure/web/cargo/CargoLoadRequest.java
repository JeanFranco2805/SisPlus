package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CargoLoadRequest {
    private Long vehicleId;
    private LocalDate loadDate;
    private Double merchandiseValue;
    private String notes;
}
