package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final PortCaseAdapter portCaseAdapter;

    public AdminService(PortCaseAdapter portCaseAdapter) {
        this.portCaseAdapter = portCaseAdapter;
    }

    public AdminDomain save(AdminDomain adminDomain) {
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
