package com.optical.net.sisplus.app.infrastructure.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * ============================================================
 *  INTERCEPTOR DE RATE LIMITING — Versión reforzada
 * ============================================================
 *
 * MEJORAS SOBRE EL ORIGINAL:
 *  1. Integra el bloqueo progresivo de RateLimitingConfig
 *     (login bloqueado tras MAX_FAILURES intentos)
 *  2. Límite de tamaño de payload — rechaza bodies > 1 MB
 *     para prevenir ataques DoS por cuerpos enormes
 *  3. Límite de longitud de query string — previene DoS por params gigantes
 *  4. Headers de respuesta estandarizados (X-Rate-Limit-*)
 *  5. Logging de seguridad con IP real (considera proxies)
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** Tamaño máximo de body permitido (1 MB) */
    private static final long MAX_BODY_SIZE_BYTES = 1024 * 1024;

    /** Longitud máxima de query string */
    private static final int MAX_QUERY_STRING_LENGTH = 500;

    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitInterceptor(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String ip  = getClientIP(request);
        String uri = request.getRequestURI();

        // ----------------------------------------------------------
        // 1. Validar tamaño del payload (anti-DoS por body enorme)
        // ----------------------------------------------------------
        int contentLength = request.getContentLength();
        if (contentLength > MAX_BODY_SIZE_BYTES) {
            log.warn("[DoS] Body demasiado grande ({} bytes) desde IP: {}", contentLength, ip);
            sendError(response, HttpStatus.PAYLOAD_TOO_LARGE,
                    "El tamaño del cuerpo de la petición supera el límite permitido.");
            return false;
        }

        // ----------------------------------------------------------
        // 2. Validar longitud del query string (anti-DoS por params)
        // ----------------------------------------------------------
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > MAX_QUERY_STRING_LENGTH) {
            log.warn("[DoS] Query string demasiado larga ({} chars) desde IP: {}",
                    queryString.length(), ip);
            sendError(response, HttpStatus.BAD_REQUEST,
                    "Parámetros de consulta demasiado largos.");
            return false;
        }

        // ----------------------------------------------------------
        // 3. Verificar bloqueo por fuerza bruta (solo en /auth/login)
        // ----------------------------------------------------------
        if (uri.contains("/auth/login")) {
            if (rateLimitingConfig.isIpBlocked(ip)) {
                long waitSeconds = rateLimitingConfig.getBlockedSecondsRemaining(ip);
                log.warn("[BRUTE-FORCE] Acceso bloqueado para IP: {} — {}s restantes", ip, waitSeconds);
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitSeconds));
                sendError(response, HttpStatus.TOO_MANY_REQUESTS,
                        String.format("IP bloqueada temporalmente. Intente en %d segundos.", waitSeconds));
                return false;
            }
        }

        // ----------------------------------------------------------
        // 4. Rate limiting por velocidad (bucket4j)
        // ----------------------------------------------------------
        String key = getClientKey(request, ip);
        RateLimitingConfig.RateLimit rateLimit = getRateLimitForEndpoint(uri);

        Bucket bucket = rateLimitingConfig.resolveBucket(key, rateLimit);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        log.warn("[RATE-LIMIT] Límite excedido — IP: {}, endpoint: {}", ip, uri);
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitSeconds));
        sendError(response, HttpStatus.TOO_MANY_REQUESTS,
                String.format("Demasiadas peticiones. Espere %d segundos.", waitSeconds));
        return false;
    }

    // ----------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------

    private String getClientKey(HttpServletRequest request, String ip) {
        String user = request.getRemoteUser();
        return (user != null && !user.isBlank()) ? user + "@" + ip : ip;
    }

    /**
     * Obtiene la IP real del cliente respetando proxies/load balancers.
     * Toma solo el primer valor de X-Forwarded-For para evitar spoofing.
     */
    private String getClientIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Solo el primer IP del chain — los demás son añadidos por proxies intermedios
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitingConfig.RateLimit getRateLimitForEndpoint(String uri) {
        if (uri.contains("/auth/login"))  return RateLimitingConfig.RateLimit.LOGIN;
        if (uri.contains("/payroll"))     return RateLimitingConfig.RateLimit.PAYROLL;
        if (uri.contains("/users"))       return RateLimitingConfig.RateLimit.USERS;
        return RateLimitingConfig.RateLimit.API_DEFAULT;
    }

    private void sendError(HttpServletResponse response,
                           HttpStatus status,
                           String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
                "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\"}",
                status.getReasonPhrase(), status.value(), message));
    }
}