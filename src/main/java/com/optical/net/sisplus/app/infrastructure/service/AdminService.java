package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final PortCaseAdapter portCaseAdapter;
    private final PasswordEncoder passwordEncoder;

    public AdminService(PortCaseAdapter portCaseAdapter, PasswordEncoder passwordEncoder) {
        this.portCaseAdapter = portCaseAdapter;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminDomain save(AdminDomain adminDomain) {
        if (adminDomain.getPassword() != null && !adminDomain.getPassword().isEmpty()) {
            adminDomain.setPassword(passwordEncoder.encode(adminDomain.getPassword()));
        }
        return portCaseAdapter.save(adminDomain);
    }

    public AdminDomain findByUsername(String username) {
        return portCaseAdapter.findByUsername(username);
    }

    public boolean removeAdmin(String username) {
        return portCaseAdapter.removeAdmin(username);
    }

    public List<AdminDomain> findAllAdmins() {
        return portCaseAdapter.findAllAdmins();
    }
}