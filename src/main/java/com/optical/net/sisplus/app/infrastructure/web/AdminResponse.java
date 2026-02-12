package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AdminResponse {
    private Long id;
    private String username;
    private String password;
    private String rolname = "ADMIN";
}
