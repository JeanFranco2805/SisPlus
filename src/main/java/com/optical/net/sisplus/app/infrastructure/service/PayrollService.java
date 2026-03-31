package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.PayrollConfiguration;
import com.optical.net.sisplus.app.domain.UserDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PayrollService {

    private final PortAdapter portAdapter;
    private final PayrollConfigurationService configurationService;

    public PayrollService(PortAdapter portAdapter, PayrollConfigurationService configurationService) {
        this.portAdapter = portAdapter;
        this.configurationService = configurationService;
    }

    public PayrollCalculation calculatePayroll(Long userId, String period,
                                               Integer month, Integer year,
                                               LocalDate date,
                                               PayrollConfiguration config) {
        log.debug("Calculando nómina para usuario {} - periodo: {}", userId, period);

        UserDomain user = portAdapter.findUserById(userId);

        if (user.getAttendance() == null || user.getAttendance().isEmpty()) {
            log.warn("Usuario {} no tiene asistencias cargadas", userId);
        } else {
            log.debug("Usuario {} tiene {} asistencias", userId, user.getAttendance().size());
        }

        return switch (period.toLowerCase()) {
            case "weekly" -> user.calculateWeeklyPayroll(
                    date != null ? date : LocalDate.now(),
                    config
            );
            case "monthly" -> {
                int m = month != null ? month : LocalDate.now().getMonthValue();
                int y = year != null ? year : LocalDate.now().getYear();
                yield user.calculateMonthlyPayroll(m, y, config);
            }
            default -> user.calculateDailyPayroll(
                    date != null ? date : LocalDate.now(),
                    config
            );
        };
    }

    @Async("payrollExecutor")
    public CompletableFuture<PayrollCalculation> calculateMonthlyPayrollAsync(
            Long userId, int month, int year) {

        log.info("Iniciando cálculo asíncrono de nómina mensual para usuario: {}", userId);

        try {
            UserDomain user = portAdapter.findUserById(userId);
            PayrollConfiguration config = configurationService.getPayrollConfig();

            PayrollCalculation result = user.calculateMonthlyPayroll(month, year, config);

            log.info("Cálculo asíncrono completado para usuario: {}", userId);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error en cálculo asíncrono para usuario {}: {}", userId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("payrollExecutor")
    public CompletableFuture<List<EmployeePayroll>> calculatePayrollForAllEmployees(
            int month, int year) {

        log.info("Iniciando cálculo masivo de nómina para todos los empleados");
        long startTime = System.currentTimeMillis();

        try {
            List<UserDomain> users = portAdapter.getAllUsers();
            PayrollConfiguration config = configurationService.getPayrollConfig();

            List<EmployeePayroll> payrolls = users.parallelStream()
                    .map(user -> {
                        try {
                            UserDomain fullUser = portAdapter.findUserById(user.getId());
                            PayrollCalculation calc = fullUser.calculateMonthlyPayroll(month, year, config);
                            return new EmployeePayroll(user.getId(), user.getName(),
                                    user.getLastName(), calc);
                        } catch (Exception e) {
                            log.error("Error calculando nómina para usuario {}: {}",
                                    user.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(p -> p != null)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cálculo masivo completado en {} ms para {} empleados",
                    duration, payrolls.size());

            return CompletableFuture.completedFuture(payrolls);

        } catch (Exception e) {
            log.error("Error en cálculo masivo de nómina: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public record EmployeePayroll(
            Long userId,
            String firstName,
            String lastName,
            PayrollCalculation payroll
    ) {}
}