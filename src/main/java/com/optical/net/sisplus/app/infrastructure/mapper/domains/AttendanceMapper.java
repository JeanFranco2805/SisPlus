package com.optical.net.sisplus.app.infrastructure.mapper.domains;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Attendance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AttendanceMapper {
    AttendanceDomain toDomain(Attendance attendance);
    Attendance toEntity(AttendanceDomain domain);

}
