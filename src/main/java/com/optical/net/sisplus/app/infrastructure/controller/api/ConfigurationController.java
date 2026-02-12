package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.infrastructure.security.XssSanitizer;
import com.optical.net.sisplus.app.infrastructure.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 *  CONFIGURATION CONTROLLER — Con sanitización XSS
 * ============================================================
 *
 * CAMBIOS:
 *  - key sanitizado: solo mayúsculas y guión bajo (REGULAR_HOUR_RATE)
 *  - value sanitizado según el tipo de clave (numérico vs texto)
 *  - Validación de longitud para prevenir valores enormes en BD
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService service;
    private final XssSanitizer xssSanitizer;

    /** Claves que solo aceptan valores numéricos */
    private static final java.util.Set<String> NUMERIC_KEYS = java.util.Set.of(
            "REGULAR_HOUR_RATE", "DAY_OVERTIME_RATE",
            "NIGHT_SURCHARGE_RATE", "NIGHT_OVERTIME_RATE",
            "NIGHT_START_HOUR", "NIGHT_END_HOUR"
    );

    @PostMapping
    public ConfigurationDomain create(@RequestBody ConfigurationDomain config) {
        config.setKey(sanitizeKey(config.getKey()));
        config.setValue(sanitizeValue(config.getKey(), config.getValue()));
        return service.save(config);
    }

    @GetMapping
    public List<ConfigurationDomain> getAll() {
        return service.getAll();
    }

    @GetMapping("/{key}")
    public ConfigurationDomain get(@PathVariable String key) {
        return service.get(sanitizeKey(key));
    }

    @PutMapping("/{key}")
    public ConfigurationDomain update(
            @PathVariable String key,
            @RequestParam String value
    ) {
        String safeKey   = sanitizeKey(key);
        String safeValue = sanitizeValue(safeKey, value);
        return service.update(safeKey, safeValue);
    }

    // ----------------------------------------------------------

    private String sanitizeKey(String key) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("La clave de configuración no puede estar vacía.");
        String safe = xssSanitizer.sanitizeConfigKey(key);
        if (safe.length() > 50)
            throw new IllegalArgumentException("La clave excede el máximo de 50 caracteres.");
        return safe;
    }

    private String sanitizeValue(String key, String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El valor de configuración no puede estar vacío.");
        if (value.length() > 255)
            throw new IllegalArgumentException("El valor excede el máximo de 255 caracteres.");

        if (NUMERIC_KEYS.contains(key)) {
            String numeric = xssSanitizer.sanitizeNumericValue(value);
            if (numeric.isEmpty())
                throw new IllegalArgumentException(
                        "El valor para '" + key + "' debe ser numérico.");
            return numeric;
        }

        return xssSanitizer.sanitize(value);
    }
}