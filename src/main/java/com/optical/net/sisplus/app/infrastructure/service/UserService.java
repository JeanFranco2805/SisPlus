package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.PayrollConfiguration;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.response.UserResponseMapper;
import com.optical.net.sisplus.app.infrastructure.security.XssSanitizer;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private static final double DEFAULT_SALARY = 1_423_500.0;

    private final PortAdapter portAdapter;
    private final PayrollConfigurationService configurationService;
    private final XssSanitizer xssSanitizer;

    public UserService(PortAdapter portAdapter,
                       PayrollConfigurationService configurationService,
                       XssSanitizer xssSanitizer) {
        this.portAdapter = portAdapter;
        this.configurationService = configurationService;
        this.xssSanitizer = xssSanitizer;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Obteniendo todos los usuarios");
        return UserResponseMapper.toBasicUserResponseList(portAdapter.getAllUsers());
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        String safeName = sanitizeName(request.getName(), "name");
        String safeLastName = sanitizeName(request.getLastName(), "lastName");
        String safeCc = xssSanitizer.sanitizeCc(request.getCc());
        double safeSalary = resolveSalary(request.getSalary());

        validateUserFields(safeName, safeLastName, safeCc);
        validateSalary(safeSalary);

        log.info("Creando nuevo usuario con CC: {}", safeCc);

        UserDomain user = UserDomain.builder()
                .name(safeName)
                .lastName(safeLastName)
                .cc(safeCc)
                .salary(safeSalary)
                .build();

        UserDomain saved = portAdapter.saveUser(user);
        return UserResponseMapper.toBasicUserResponse(saved);
    }

    @Transactional
    public boolean deleteUserById(Long id) {
        log.info("Eliminando usuario con ID: {}", id);
        portAdapter.deleteUser(id);
        return true;
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        String safeName = sanitizeName(request.getName(), "name");
        String safeLastName = sanitizeName(request.getLastName(), "lastName");
        String safeCc = xssSanitizer.sanitizeCc(request.getCc());
        double safeSalary = resolveSalary(request.getSalary());

        validateUserFields(safeName, safeLastName, safeCc);
        validateSalary(safeSalary);

        log.info("Actualizando usuario con ID: {}", id);

        UserDomain user = UserDomain.builder()
                .id(id)
                .name(safeName)
                .lastName(safeLastName)
                .cc(safeCc)
                .salary(safeSalary)
                .build();

        UserDomain saved = portAdapter.saveUser(user);
        return UserResponseMapper.toBasicUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse calculatePayroll(Long id, LocalDate date,
                                         Integer month, Integer year,
                                         String period) {
        log.debug("Calculando nómina para usuario {} - periodo: {}", id, period);

        UserDomain user = portAdapter.findUserById(id);

        log.debug("Usuario {} cargado con {} asistencias", id,
                user.getAttendance() != null ? user.getAttendance().size() : 0);

        PayrollConfiguration config = configurationService.getPayrollConfig();

        PayrollCalculation payroll = calculatePayrollByPeriod(user, date, month, year, period, config);

        log.debug("Nómina calculada para usuario {}: totalPay={}, regularHours={}, dayOT={}, nightOT={}",
                id, payroll.getTotalPay(), payroll.getRegularHours(),
                payroll.getDayOvertimeHours(), payroll.getNightOvertimeHours());

        return UserResponseMapper.fromDomainWithPayroll(user, payroll);
    }

    private PayrollCalculation calculatePayrollByPeriod(UserDomain user, LocalDate date,
                                                        Integer month, Integer year,
                                                        String period, PayrollConfiguration config) {
        if (period != null) {
            return switch (period.toLowerCase()) {
                case "weekly" -> {
                    LocalDate d = date != null ? date : LocalDate.now();
                    log.debug("Calculando nómina semanal para fecha: {}, weekStart: {}", d, d.minusDays(6));
                    yield user.calculateWeeklyPayroll(d, config);
                }
                case "monthly" -> {
                    int m = month != null ? month : LocalDate.now().getMonthValue();
                    int y = year != null ? year : LocalDate.now().getYear();
                    log.debug("Calculando nómina mensual para {}/{}", m, y);
                    yield user.calculateMonthlyPayroll(m, y, config);
                }
                default -> {
                    LocalDate d = date != null ? date : LocalDate.now();
                    log.debug("Calculando nómina diaria para fecha: {}", d);
                    yield user.calculateDailyPayroll(d, config);
                }
            };
        }

        if (month != null && year != null) {
            return user.calculateMonthlyPayroll(month, year, config);
        }

        return user.calculateDailyPayroll(date != null ? date : LocalDate.now(), config);
    }

    @Transactional
    public void registerEntry(Long id) {
        log.info("Registrando entrada para usuario: {}", id);
        portAdapter.registerAttendance(id);
    }

    @Transactional
    public void registerEntryByCC(String cc) {
        log.info("Registrando entrada para usuario con CC: {}", cc);
        portAdapter.registerAttendanceByCc(cc);
    }

    @Transactional
    public void registerExitByCC(String cc) {
        log.info("Registrando salida para usuario: {}", cc);
        portAdapter.registerAttendanceByCc(cc);
    }

    @Transactional
    public void registerExit(Long id) {
        log.info("Registrando salida para usuario: {}", id);
        portAdapter.registerDeparture(id);
    }

    private double resolveSalary(Double salary) {
        if (salary == null || salary <= 0) return DEFAULT_SALARY;
        return salary;
    }

    private String sanitizeName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El campo '" + field + "' es requerido.");
        }
        return xssSanitizer.sanitizeAlphanumeric(value);
    }

    private void validateUserFields(String name, String lastName, String cc) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        if (name.length() > 100)
            throw new IllegalArgumentException("El nombre excede el máximo de 100 caracteres.");
        if (lastName == null || lastName.isBlank())
            throw new IllegalArgumentException("El apellido no puede estar vacío.");
        if (lastName.length() > 100)
            throw new IllegalArgumentException("El apellido excede el máximo de 100 caracteres.");
        if (cc == null || cc.isBlank())
            throw new IllegalArgumentException("La cédula no puede estar vacía.");
        if (!cc.matches("^[0-9]{5,15}$"))
            throw new IllegalArgumentException("La cédula debe contener entre 5 y 15 dígitos.");
    }

    private void validateSalary(double salary) {
        if (salary < 0)
            throw new IllegalArgumentException("El salario no puede ser negativo.");
        if (salary > 100_000_000)
            throw new IllegalArgumentException("El salario excede el valor máximo permitido.");
    }
}