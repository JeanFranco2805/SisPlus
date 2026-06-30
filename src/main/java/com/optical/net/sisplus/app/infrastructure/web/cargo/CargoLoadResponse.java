package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CargoLoadResponse {
    private Long id;
    private VehicleResponse vehicle;
    private LocalDate loadDate;
    private Double merchandiseValue;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private CargoSettlementResponse settlement;
}
