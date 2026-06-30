package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cargo_settlements")
public class CargoSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_load_id", nullable = false, unique = true)
    private CargoLoad cargoLoad;

    private Double deliveredValue;
    private Double returnedValue;
    private Double coins;
    private Double cash;
    private Double qr;
    private Double security;
    private Double total;

    private LocalDateTime settlementDate;

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        this.total = zeroIfNull(cash)
                + zeroIfNull(coins)
                + zeroIfNull(qr)
                - zeroIfNull(security)
                - zeroIfNull(returnedValue);
        if (this.settlementDate == null) {
            this.settlementDate = LocalDateTime.now();
        }
    }

    private double zeroIfNull(Double value) {
        return value == null ? 0.0 : value;
    }
}
