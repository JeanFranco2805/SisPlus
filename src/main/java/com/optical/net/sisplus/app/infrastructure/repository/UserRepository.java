package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    void removeById(Long id);
}
