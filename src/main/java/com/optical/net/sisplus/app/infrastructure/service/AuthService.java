package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import com.optical.net.sisplus.app.infrastructure.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Admin registerAdmin(String username, String password) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }

        Admin admin = Admin.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(Set.of("ADMIN"))
                .build();

        return adminRepository.save(admin);
    }

    @Transactional
    public void updateLastLogin(String username) {
        adminRepository.findOptionalByUsername(username).ifPresent(admin -> {
            admin.setLastLogin(LocalDateTime.now());
            adminRepository.save(admin);
        });
    }

    public Admin findByUsername(String username) {
        return adminRepository.findOptionalByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}