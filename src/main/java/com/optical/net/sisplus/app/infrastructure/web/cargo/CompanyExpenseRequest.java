package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class CompanyExpenseRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    private Long categoryId;

    private Double amount;

    private String description;

    private Long vehicleId;

    private Long driverId;

    private Long settlementId;

    private MultipartFile attachment;

    private Boolean removeAttachment;
}
