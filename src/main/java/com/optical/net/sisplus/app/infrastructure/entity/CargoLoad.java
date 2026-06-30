package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cargo_loads")
public class CargoLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private LocalDate loadDate;

    @Column(nullable = false)
    private Double merchandiseValue;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CargoStatus status = CargoStatus.PENDIENTE;

    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "cargoLoad", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CargoSettlement settlement;
}
