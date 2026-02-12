package com.optical.net.sisplus.app.infrastructure.web.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String redirectUrl;
}