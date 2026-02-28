package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Representa un dispositivo ZKTeco registrado en el sistema.
 * Equivale a DeviceInfo.java del SDK original, portado a Spring Boot / JPA.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zk_devices")
public class ZkDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de serie del dispositivo (único) */
    @Column(name = "device_sn", unique = true, nullable = false)
    private String deviceSn;

    /** Nombre descriptivo (por defecto: SN + IP) */
    @Column(name = "device_name")
    private String deviceName;

    /** IP actual del dispositivo */
    @Column(name = "ip_address")
    private String ipAddress;

    /** Estado: Online / Offline / connecting */
    @Column(name = "state")
    @Builder.Default
    private String state = "Offline";

    /** Versión del protocolo Push que usa el dispositivo (ej: 2.4.1) */
    @Column(name = "push_version")
    private String pushVersion;

    /** Versión del firmware */
    @Column(name = "firmware_version")
    private String firmwareVersion;

    /** Idioma configurado en el dispositivo */
    @Column(name = "dev_language")
    private String devLanguage;

    /**
     * Stamps de sincronización — le dicen al dispositivo desde qué punto
     * debe reenviar datos (resumable upload).
     * "None" = enviar todo desde el principio.
     * "9999" = no enviar.
     */
    @Column(name = "att_log_stamp")
    @Builder.Default
    private String attLogStamp = "None";

    @Column(name = "op_log_stamp")
    @Builder.Default
    private String opLogStamp = "9999";

    @Column(name = "att_photo_stamp")
    @Builder.Default
    private String attPhotoStamp = "None";

    @Column(name = "bio_data_stamp")
    @Builder.Default
    private String bioDataStamp = "0";

    @Column(name = "error_log_stamp")
    @Builder.Default
    private String errorLogStamp = "0";

    /** Capacidades reportadas por el dispositivo */
    @Column(name = "user_count")
    @Builder.Default
    private int userCount = 0;

    @Column(name = "fp_count")
    @Builder.Default
    private int fpCount = 0;

    @Column(name = "face_count")
    @Builder.Default
    private int faceCount = 0;

    @Column(name = "trans_count")
    @Builder.Default
    private int transCount = 0;

    @Column(name = "fp_alg_version")
    private String fpAlgVersion;

    @Column(name = "face_alg_version")
    private String faceAlgVersion;

    /** Última vez que el dispositivo hizo heartbeat */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }
}