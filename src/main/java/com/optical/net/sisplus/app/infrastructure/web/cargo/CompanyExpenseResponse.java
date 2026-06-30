package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CompanyExpenseResponse {
    private Long id;
    private LocalDate expenseDate;
    private ExpenseCategoryResponse category;
    private Double amount;
    private String description;
    private VehicleResponse vehicle;
    private DriverResponse driver;
    private CargoSettlementResponse settlement;
    private boolean hasAttachment;
    private String attachmentOriginalName;
    private String attachmentContentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
