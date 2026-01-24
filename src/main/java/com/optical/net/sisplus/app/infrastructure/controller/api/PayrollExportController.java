package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.service.PayrollExportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payroll")
public class PayrollExportController {

    private final PayrollExportService payrollExportService;

    public PayrollExportController(PayrollExportService payrollExportService) {
        this.payrollExportService = payrollExportService;
    }

    @GetMapping("/export/excel")
    public ResponseEntity<Resource> exportToExcel(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        try {
            int currentMonth = month != null ? month : LocalDate.now().getMonthValue();
            int currentYear = year != null ? year : LocalDate.now().getYear();

            Resource resource = payrollExportService.generatePayrollExcel(currentMonth, currentYear);

            String filename = String.format("Nomina_%s_%d.xlsx", getMonthName(currentMonth), currentYear);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getMonthName(int month) {
        String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        return months[month - 1];
    }
}