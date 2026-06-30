package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.CargoLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CargoLoadRepository extends JpaRepository<CargoLoad, Long> {

    List<CargoLoad> findByLoadDateOrderByVehicleNameAsc(LocalDate loadDate);

    @Query("SELECT cl FROM CargoLoad cl LEFT JOIN FETCH cl.settlement WHERE cl.loadDate = :date ORDER BY cl.vehicle.name ASC")
    List<CargoLoad> findByLoadDateWithSettlement(@Param("date") LocalDate date);

    Optional<CargoLoad> findByVehicleIdAndLoadDate(Long vehicleId, LocalDate loadDate);

    long countByVehicleId(Long vehicleId);
}
