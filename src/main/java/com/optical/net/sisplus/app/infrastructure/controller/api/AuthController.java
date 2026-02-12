package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.config.RateLimitingConfig;
import com.optical.net.sisplus.app.infrastructure.web.request.LoginRequest;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern SAFE_IP_PATTERN = Pattern.compile("^[a-fA-F0-9.:,\\[\\] ]{1,100}$");

    private final AuthenticationManager authenticationManager;
    private final RateLimitingConfig rateLimitingConfig;

    public AuthController(AuthenticationManager authenticationManager,
                          RateLimitingConfig rateLimitingConfig) {
        this.authenticationManager = authenticationManager;
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String clientIp = getClientIP(request);
        String bucketKey = "login:" + clientIp;
        Bucket bucket = rateLimitingConfig.resolveBucket(bucketKey, RateLimitingConfig.RateLimit.LOGIN);

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "success", false,
                    "message", "Demasiados intentos. Por favor intenta en 15 minutos."
            ));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            new HttpSessionSecurityContextRepository()
                    .saveContext(SecurityContextHolder.getContext(), request, response);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "redirectUrl", "/dashboard"
            ));

        } catch (BadCredentialsException ex) {
            String errorId = UUID.randomUUID().toString();
            log.warn("Authentication failed [{}] from IP: {}", errorId, clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Credenciales incorrectas"
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        String rawIp;
        if (xfHeader != null && !xfHeader.isEmpty()) {
            rawIp = xfHeader.split(",")[0].trim();
        } else {
            rawIp = request.getRemoteAddr();
        }
        if (rawIp != null && SAFE_IP_PATTERN.matcher(rawIp).matches()) {
            return rawIp;
        }
        return "unknown";
    }
}