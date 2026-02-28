package com.optical.net.sisplus.app.infrastructure.repository;

import com.optical.net.sisplus.app.infrastructure.entity.ZkDeviceCommand;
import com.optical.net.sisplus.app.infrastructure.entity.ZkDeviceCommand.CommandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ZkDeviceCommandRepository extends JpaRepository<ZkDeviceCommand, Long> {

    /** Obtiene los comandos PENDING para un dispositivo, ordenados por creación */
    List<ZkDeviceCommand> findByDeviceSnAndStatusOrderByCommitTimeAsc(
            String deviceSn, CommandStatus status);

    /** Actualiza estado de una lista de comandos a SENT */
    @Modifying
    @Query("UPDATE ZkDeviceCommand c SET c.status = 'SENT', c.transTime = :time WHERE c.id IN :ids")
    int markAsSent(@Param("ids") List<Long> ids, @Param("time") LocalDateTime time);

    /** Busca un comando SENT por su ID (para procesar respuesta del dispositivo) */
    @Query("SELECT c FROM ZkDeviceCommand c WHERE c.deviceSn = :sn AND c.id = :id AND c.status = 'SENT'")
    java.util.Optional<ZkDeviceCommand> findSentCommand(@Param("sn") String deviceSn,
                                                        @Param("id") Long id);
}