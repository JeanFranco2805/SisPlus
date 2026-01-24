package com.optical.net.sisplus.app.infrastructure.mapper.domains;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.entity.Admin;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMapper {
    Admin toEntity(AdminDomain domain);

    AdminDomain toDomain(Admin admin);

    List<Admin> toEntitiesList(List<AdminDomain> domains);

    List<AdminDomain> toDomainsList(List<Admin> admins);
}
