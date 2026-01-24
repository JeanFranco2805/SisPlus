package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigurationService {


    private final PortCaseAdapter portCaseAdapter;

    public ConfigurationDomain save(ConfigurationDomain config) {
        var entity = portCaseAdapter.saveConfig(config);
        return ConfigurationDomain.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .value(entity.getValue())
                .build();
    }

    public ConfigurationDomain get(String key) {
        var entity = portCaseAdapter.getConfig(key);
        return ConfigurationDomain.builder()
                .id(entity.getId())
                .value(entity.getValue())
                .key(entity.getKey())
                .build();
    }

    public List<ConfigurationDomain> getAll() {
        var entities = portCaseAdapter.getAllConfig();
        return entities.stream().map(e -> ConfigurationDomain.builder()
                .key(e.getKey())
                .id(e.getId())
                .value(e.getValue())
                .build()).toList();
    }

    public ConfigurationDomain update(String key, String value) {
        var config = get(key);
        config.setValue(value);
        var configuration = portCaseAdapter.saveConfig(config);
        return ConfigurationDomain.builder()
                .id(configuration.getId())
                .value(configuration.getValue())
                .key(configuration.getKey())
                .build();
    }
}

