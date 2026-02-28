// ── ZkDeviceRepository.java ──────────────────────────────────────────────────
package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.ZkDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ZkDeviceRepository extends JpaRepository<ZkDevice, Long> {

    Optional<ZkDevice> findByDeviceSn(String deviceSn);

    boolean existsByDeviceSn(String deviceSn);

    List<ZkDevice> findByState(String state);

    @Modifying
    @Query("UPDATE ZkDevice d SET d.state = :state, d.lastActivity = :time WHERE d.deviceSn = :sn")
    int updateState(@Param("sn") String deviceSn,
                    @Param("state") String state,
                    @Param("time") LocalDateTime time);

    @Modifying
    @Query("UPDATE ZkDevice d SET d.attLogStamp = :stamp WHERE d.deviceSn = :sn")
    int updateAttLogStamp(@Param("sn") String deviceSn, @Param("stamp") String stamp);
}