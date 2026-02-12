package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import com.optical.net.sisplus.app.infrastructure.repository.AdminRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AdminService {
    private final PortCaseAdapter portCaseAdapter;
    private final PasswordEncoder passwordEncoder;
    private final AdminRepository adminRepository;

    @Value("${app.admin.default-username:#{null}}")
    private String defaultAdminUsername;

    @Value("${app.admin.default-password:#{null}}")
    private String defaultAdminPassword;

    public AdminService(PortCaseAdapter portCaseAdapter,
                        PasswordEncoder passwordEncoder,
                        AdminRepository adminRepository) {
        this.portCaseAdapter = portCaseAdapter;
        this.passwordEncoder = passwordEncoder;
        this.adminRepository = adminRepository;
    }

    public AdminDomain save(AdminDomain adminDomain) {
        if (StringUtils.hasText(adminDomain.getPassword())) {
            adminDomain.setPassword(passwordEncoder.encode(adminDomain.getPassword()));
        }
        return portCaseAdapter.save(adminDomain);
    }

    @Transactional
    public void initializeDefaultAdmin() {
        if (!StringUtils.hasText(defaultAdminUsername) || !StringUtils.hasText(defaultAdminPassword)) {
            log.warn("Default admin credentials not configured. Skipping initialization.");
            return;
        }

        if (!adminRepository.existsByUsername(defaultAdminUsername)) {
            Admin defaultAdmin = new Admin();
            defaultAdmin.setUsername(defaultAdminUsername);
            defaultAdmin.setPassword(passwordEncoder.encode(defaultAdminPassword));
            defaultAdmin.setEnabled(true);
            defaultAdmin.setAccountNonExpired(true);
            defaultAdmin.setAccountNonLocked(true);
            defaultAdmin.setCredentialsNonExpired(true);

            Set<String> roles = new HashSet<>();
            roles.add("ADMIN");
            defaultAdmin.setRoles(roles);

            adminRepository.save(defaultAdmin);
            log.info("Default admin user created.");
        }
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