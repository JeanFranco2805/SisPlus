package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class PayrollReportResponse {
    private Long id;
    private int month;
    private int year;
    private String monthName;
    private LocalDateTime generatedAt;
    private String generationType;
    private String fileName;
    private int totalEmployees;
    private double totalPayroll;
}
