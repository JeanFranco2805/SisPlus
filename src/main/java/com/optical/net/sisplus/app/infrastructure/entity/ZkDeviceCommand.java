package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Cola de comandos para enviar a dispositivos ZKTeco.
 * Equivale a DeviceCommand.java del SDK original.
 *
 * Flujo:
 *  1. Tu app encola un comando aquí (estado: PENDING)
 *  2. El dispositivo hace GET /iclock/getrequest
 *  3. El servidor responde con los comandos pendientes (estado: SENT)
 *  4. El dispositivo ejecuta el comando y responde a /iclock/devicecmd
 *  5. El servidor marca el comando como COMPLETED con el resultado
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zk_device_commands", indexes = {
        @Index(name = "idx_cmd_sn_status", columnList = "device_sn, status")
})
public class ZkDeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_sn", nullable = false)
    private String deviceSn;

    @Column(name = "cmd_content", nullable = false, length = 4096)
    private String cmdContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CommandStatus status = CommandStatus.PENDING;

    @Column(name = "commit_time")
    private LocalDateTime commitTime;

    @Column(name = "trans_time")
    private LocalDateTime transTime;

    @Column(name = "over_time")
    private LocalDateTime overTime;

    @Column(name = "return_code")
    private String returnCode;

    @Column(name = "return_info", length = 512)
    private String returnInfo;

    @PrePersist
    protected void onCreate() {
        commitTime = LocalDateTime.now();
    }

    public enum CommandStatus {
        PENDING,
        SENT,
        COMPLETED,
        FAILED
    }
}