package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.ConfigurationDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final PortCaseAdapter portCaseAdapter;

    public ConfigurationDomain save(ConfigurationDomain config) {
        return portCaseAdapter.saveConfig(config);
    }

    public ConfigurationDomain get(String key) {
        return portCaseAdapter.getConfig(key);
    }

    public List<ConfigurationDomain> getAll() {
        return portCaseAdapter.getAllConfig();
    }

    @CacheEvict(value = "payrollConfig", allEntries = true)
    public ConfigurationDomain update(String key, String value) {
        return portCaseAdapter.updateConfig(key, value);
    }
}