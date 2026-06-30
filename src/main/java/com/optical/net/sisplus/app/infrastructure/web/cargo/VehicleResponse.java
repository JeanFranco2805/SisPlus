package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class VehicleResponse {
    private Long id;
    private String plate;
    private String name;
    private DriverResponse driver;
    private boolean active;
    private LocalDateTime createdAt;
}
