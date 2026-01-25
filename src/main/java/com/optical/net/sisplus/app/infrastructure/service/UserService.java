package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.PayrollCalculation;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.response.UserResponseMapper;
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

    private final PortAdapter portAdapter;
    private final PayrollService payrollService;

    public UserService(PortAdapter portAdapter, PayrollService payrollService) {
        this.portAdapter = portAdapter;
        this.payrollService = payrollService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Obteniendo todos los usuarios");
        return UserResponseMapper.toBasicUserResponseList(portAdapter.getAllUsers());
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creando nuevo usuario: {}", request.getCc());

        UserDomain user = UserDomain.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
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
        log.info("Actualizando usuario con ID: {}", id);

        UserDomain user = UserDomain.builder()
                .id(id)
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
                .build();

        UserDomain saved = portAdapter.saveUser(user);
        return UserResponseMapper.toBasicUserResponse(saved);
    }

    /**
     * MEJORADO: Ahora usa PayrollService con caché
     */
    @Transactional(readOnly = true)
    public UserResponse calculatePayroll(
            Long id,
            LocalDate date,
            Integer month,
            Integer year,
            String period
    ) {
        log.debug("Calculando nómina para usuario {} - periodo: {}", id, period);

        UserDomain user = portAdapter.findUserById(id);

        PayrollCalculation payroll = payrollService.calculatePayroll(
                id, period, month, year, date
        );

        return UserResponseMapper.fromDomainWithPayroll(user, payroll);
    }

    @Transactional
    public void registerEntry(Long id) {
        log.info("Registrando entrada para usuario: {}", id);
        portAdapter.registerAttendance(id);
    }

    @Transactional
    public void registerExit(Long id) {
        log.info("Registrando salida para usuario: {}", id);
        portAdapter.registerDeparture(id);
    }
}