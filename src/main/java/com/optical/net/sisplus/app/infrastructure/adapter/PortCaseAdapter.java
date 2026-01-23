package com.optical.net.sisplus.app.infrastructure.adapter;
import com.optical.net.sisplus.app.application.PortAdapter;
import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.mapper.AttendanceMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.FootPrintsMapper;
import com.optical.net.sisplus.app.infrastructure.mapper.UserMapper;
import com.optical.net.sisplus.app.infrastructure.repository.AttendanceRepository;
import com.optical.net.sisplus.app.infrastructure.repository.FootPrintsRepository;
import com.optical.net.sisplus.app.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public class PortCaseAdapter implements PortAdapter {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final FootPrintsRepository footPrintsRepository;
    private final FootPrintsMapper footPrintsMapper;

    public PortCaseAdapter(UserRepository userRepository, UserMapper userMapper, AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper, FootPrintsRepository footPrintsRepository, FootPrintsMapper footPrintsMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
        this.footPrintsRepository = footPrintsRepository;
        this.footPrintsMapper = footPrintsMapper;
    }

    @Override
    public UserDomain guardarUsuario(UserDomain userDomain) {
        return  userMapper.toDomain(userRepository.save(userMapper.toEntity(userDomain)));
    }

    @Override
    public UserDomain buscarUsuarioPorId(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow();
        return userMapper.toDomain(user);
    }

    @Override
    public void guardarHuella(FootPrintsDomain footPrintsDomain) {
        footPrintsRepository.save(footPrintsMapper.toEntity(footPrintsDomain));
    }

    @Override
    public UserDomain identificarUsuarioPorHuella(byte[] templateHuella) {
        var fps = footPrintsRepository.findByTemplate(templateHuella).getLast();
        return userMapper.toDomain(fps.getUser());
    }

    @Override
    public AttendanceDomain obtenerAsistenciaDelDia(Long usuarioId, LocalDate fecha) {
        return null;
    }

    @Override
    public void registrarEntrada(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow();
        attendanceRepository.save(attendanceMapper.toEntity(  AttendanceDomain.builder()
                .entryTime(LocalDateTime.now())
                .user(userMapper.toDomain(user))
                .build()));
    }

    @Override
    public void registrarSalida(Long usuarioId) {
        var user = userRepository.findById(usuarioId).orElseThrow();
        var attendances = attendanceRepository.findByUser(user);
        var attendance =  attendances.stream().filter(e-> e.getEntry_time()
                .toLocalDate().equals(LocalDate.now())).findFirst().orElseThrow();
        attendance.setDeparture_time(LocalDateTime.now());
    }

}
