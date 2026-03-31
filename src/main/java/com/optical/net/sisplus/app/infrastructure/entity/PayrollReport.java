package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payroll_reports")
public class PayrollReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int month;
    private int year;
    private LocalDateTime generatedAt;
    private String generationType;
    private String fileName;
    private int totalEmployees;
    private double totalPayroll;

    @Column(name = "file_data", columnDefinition = "BYTEA")
    private byte[] fileData;
}
