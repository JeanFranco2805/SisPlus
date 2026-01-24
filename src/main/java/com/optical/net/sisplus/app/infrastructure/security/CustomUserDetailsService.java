package com.optical.net.sisplus.app.infrastructure.security;

import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import com.optical.net.sisplus.app.infrastructure.repository.AdminRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public CustomUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findOptionalByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Set<GrantedAuthority> authorities = admin.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());

        return User.builder()
                .username(admin.getUsername())
                .password(admin.getPassword())
                .authorities(authorities)
                .accountExpired(!admin.isAccountNonExpired())
                .accountLocked(!admin.isAccountNonLocked())
                .credentialsExpired(!admin.isCredentialsNonExpired())
                .disabled(!admin.isEnabled())
                .build();
    }
}