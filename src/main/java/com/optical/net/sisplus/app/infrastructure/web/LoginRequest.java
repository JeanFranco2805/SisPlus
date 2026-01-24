package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
    private boolean rememberMe;
}