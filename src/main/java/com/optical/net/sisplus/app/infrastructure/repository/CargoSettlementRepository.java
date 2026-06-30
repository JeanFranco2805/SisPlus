package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.CargoSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargoSettlementRepository extends JpaRepository<CargoSettlement, Long> {
    Optional<CargoSettlement> findByCargoLoadId(Long cargoLoadId);
}
