package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.PayrollConfiguration;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.adapter.PortCaseAdapter;
import com.optical.net.sisplus.app.infrastructure.entity.PayrollReport;
import com.optical.net.sisplus.app.infrastructure.repository.PayrollReportRepository;
import com.optical.net.sisplus.app.infrastructure.web.PayrollReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollReportService {

    private final PayrollReportRepository reportRepository;
    private final PayrollExportService payrollExportService;
    private final PayrollConfigurationService configurationService;
    private final PortCaseAdapter portCaseAdapter;

    private static final String[] MONTH_NAMES = {
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
    };

    @Transactional
    public PayrollReportResponse generateReport(int month, int year, String type) {
        try {
            List<UserDomain> users = portCaseAdapter.getAllUsers();
            PayrollConfiguration config = configurationService.getPayrollConfig();

            double totalPayroll = users.stream().mapToDouble(u -> {
                UserDomain full = portCaseAdapter.findUserById(u.getId());
                return full.calculateMonthlyPayroll(month, year, config).getTotalPay();
            }).sum();

            Resource resource = payrollExportService.generatePayrollExcel(month, year);
            byte[] fileData = resource.getInputStream().readAllBytes();
            String fileName = "Nomina_" + MONTH_NAMES[month - 1] + "_" + year + ".xlsx";

            PayrollReport report = PayrollReport.builder()
                    .month(month)
                    .year(year)
                    .generatedAt(LocalDateTime.now())
                    .generationType(type)
                    .fileName(fileName)
                    .totalEmployees(users.size())
                    .totalPayroll(totalPayroll)
                    .fileData(fileData)
                    .build();

            PayrollReport saved = reportRepository.save(report);
            return toResponse(saved);
        } catch (Exception e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 55 23 L * *")
    @Transactional
    public void generateMonthlyAutoReport() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        if (reportRepository.existsByMonthAndYear(month, year)) {
            log.info("Reporte automático ya existe para {}/{}", month, year);
            return;
        }

        try {
            generateReport(month, year, "AUTOMATIC");
            log.info("Reporte automático generado para {}/{}", month, year);
        } catch (Exception e) {
            log.error("Error en generación automática de reporte mensual", e);
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollReportResponse> getAll() {
        return reportRepository.findAll()
                .stream()
                .map(this::toResponseNoFile)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getYear(), a.getYear());
                    return cmp != 0 ? cmp : Integer.compare(b.getMonth(), a.getMonth());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PayrollReport getById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public Resource getFileResource(Long id) {
        PayrollReport report = getById(id);
        return new ByteArrayResource(report.getFileData());
    }

    @Transactional
    public void delete(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new RuntimeException("Reporte no encontrado con ID: " + id);
        }
        reportRepository.deleteById(id);
    }

    private PayrollReportResponse toResponse(PayrollReport r) {
        return PayrollReportResponse.builder()
                .id(r.getId())
                .month(r.getMonth())
                .year(r.getYear())
                .monthName(MONTH_NAMES[r.getMonth() - 1])
                .generatedAt(r.getGeneratedAt())
                .generationType(r.getGenerationType())
                .fileName(r.getFileName())
                .totalEmployees(r.getTotalEmployees())
                .totalPayroll(r.getTotalPayroll())
                .build();
    }

    private PayrollReportResponse toResponseNoFile(PayrollReport r) {
        return toResponse(r);
    }
}
