package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverRequest {
    private String name;
    private String phone;
    private Boolean active;
}
