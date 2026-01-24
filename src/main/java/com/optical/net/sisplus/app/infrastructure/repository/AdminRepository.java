package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin,Long> {
    Admin findByUsername(String username);

    void deleteByUsername(String username);
}
