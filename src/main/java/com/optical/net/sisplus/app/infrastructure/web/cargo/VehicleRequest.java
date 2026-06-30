package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequest {
    private String plate;
    private String name;
    private Long driverId;
    private Boolean active;
}
