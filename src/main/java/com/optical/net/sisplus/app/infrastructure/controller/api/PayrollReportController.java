package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.entity.PayrollReport;
import com.optical.net.sisplus.app.infrastructure.service.PayrollReportService;
import com.optical.net.sisplus.app.infrastructure.web.PayrollReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class PayrollReportController {

    private final PayrollReportService reportService;

    @GetMapping
    public List<PayrollReportResponse> getAll() {
        return reportService.getAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Integer> body) {
        int month = body.getOrDefault("month", LocalDate.now().getMonthValue());
        int year = body.getOrDefault("year", LocalDate.now().getYear());
        try {
            PayrollReportResponse report = reportService.generateReport(month, year, "MANUAL");
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        try {
            PayrollReport report = reportService.getById(id);
            Resource resource = reportService.getFileResource(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + report.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        try {
            reportService.delete(id);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
