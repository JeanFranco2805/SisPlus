package com.optical.net.sisplus.app.infrastructure.mapper.domains;

import com.optical.net.sisplus.app.domain.UserDomain;
import com.optical.net.sisplus.app.infrastructure.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {FootPrintsMapper.class, AttendanceMapper.class})
public interface UserMapper {
    User toEntity(UserDomain domain);
    UserDomain toDomain(User user);
}
