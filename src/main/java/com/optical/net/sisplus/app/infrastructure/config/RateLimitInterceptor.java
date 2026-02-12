package com.optical.net.sisplus.app.infrastructure.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor que aplica rate limiting a las peticiones HTTP
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitInterceptor(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String key = getClientKey(request);
        RateLimitingConfig.RateLimit rateLimit = getRateLimitForEndpoint(request.getRequestURI());

        Bucket bucket = rateLimitingConfig.resolveBucket(key, rateLimit);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request permitido
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Rate limit excedido
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(String.format(
                    "{\"error\":\"Demasiadas peticiones\",\"retryAfter\":%d}", waitForRefill));
            return false;
        }
    }

    /**
     * Obtiene una clave única por cliente (IP + Usuario)
     */
    private String getClientKey(HttpServletRequest request) {
        String ip = getClientIP(request);
        String user = request.getRemoteUser();
        return user != null ? user : ip;
    }

    /**
     * Obtiene la IP real del cliente (considera proxies)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Determina el límite según el endpoint
     */
    private RateLimitingConfig.RateLimit getRateLimitForEndpoint(String uri) {
        if (uri.contains("/auth/login")) {
            return RateLimitingConfig.RateLimit.LOGIN;
        } else if (uri.contains("/payroll")) {
            return RateLimitingConfig.RateLimit.PAYROLL;
        }
        return RateLimitingConfig.RateLimit.API_DEFAULT;
    }
}