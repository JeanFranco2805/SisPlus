package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import com.optical.net.sisplus.app.infrastructure.service.AuthService;
import com.optical.net.sisplus.app.infrastructure.web.AuthResponse;
import com.optical.net.sisplus.app.infrastructure.web.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    public AuthController(AuthenticationManager authenticationManager, AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            authService.updateLastLogin(request.getUsername());

            Admin admin = authService.findByUsername(request.getUsername());

            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Login exitoso")
                    .username(admin.getUsername())
                    .redirectUrl("/dashboard")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            AuthResponse response = AuthResponse.builder()
                    .success(false)
                    .message("Credenciales incorrectas")
                    .build();

            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {
        SecurityContextHolder.clearContext();

        AuthResponse response = AuthResponse.builder()
                .success(true)
                .message("Sesión cerrada exitosamente")
                .redirectUrl("/login")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        Admin admin = authService.findByUsername(username);

        AuthResponse response = AuthResponse.builder()
                .success(true)
                .username(admin.getUsername())
                .build();

        return ResponseEntity.ok(response);
    }
}