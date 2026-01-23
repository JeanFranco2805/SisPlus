package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.FootPrints;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootPrintsRepository extends JpaRepository<FootPrints, Long> {
    List<FootPrints> findByTemplate(byte[] template);
}
