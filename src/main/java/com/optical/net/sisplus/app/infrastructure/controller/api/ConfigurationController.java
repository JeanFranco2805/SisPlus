package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.infrastructure.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService service;

    @PostMapping
    public ConfigurationDomain create(@RequestBody ConfigurationDomain config) {
        return service.save(config);
    }

    @GetMapping
    public List<ConfigurationDomain> getAll() {
        return service.getAll();
    }

    @GetMapping("/{key}")
    public ConfigurationDomain get(@PathVariable String key) {
        return service.get(key);
    }

    @PutMapping("/{key}")
    public ConfigurationDomain update(
            @PathVariable String key,
            @RequestParam String value
    ) {
        return service.update(key, value);
    }
}

