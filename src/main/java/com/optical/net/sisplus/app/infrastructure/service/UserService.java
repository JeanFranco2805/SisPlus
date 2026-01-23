package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.response.UserResponseMapper;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserService {

    private final PortAdapter portAdapter;

    public UserService(PortAdapter portAdapter) {
        this.portAdapter = portAdapter;
    }

    public List<UserResponse> getAllUsers() {
        return UserResponseMapper.toBasicUserResponseList(portAdapter.getAllUsers());
    }

    public UserResponse createUser(UserRequest request) {
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
        portAdapter.deleteUser(id);
        return true;
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        UserDomain user = UserDomain.builder()
                .id(id)
                .name(request.getName())
                .lastName(request.getLastName())
                .cc(request.getCc())
                .build();

        UserDomain saved = portAdapter.saveUser(user);
        return UserResponseMapper.toBasicUserResponse(saved);
    }

    public UserResponse calculatePayroll(
            Long id,
            LocalDate date,
            Integer month,
            Integer year,
            String period
    ) {
        UserDomain user = portAdapter.findUserById(id);
        return UserResponseMapper.fromDomainWithPayroll(user, date, month, year, period);
    }

    public void registerEntry(Long id) {
        portAdapter.registerAttendance(id);
    }

    public void registerExit(Long id) {
        portAdapter.registerDeparture(id);
    }
}
