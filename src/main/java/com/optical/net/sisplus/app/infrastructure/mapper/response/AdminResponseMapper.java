package com.optical.net.sisplus.app.infrastructure.mapper.response;

import com.optical.net.sisplus.app.domain.AdminDomain;
import com.optical.net.sisplus.app.infrastructure.web.AdminRequest;
import com.optical.net.sisplus.app.infrastructure.web.AdminResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminResponseMapper {
    AdminResponse fromDomain(AdminDomain domain);

    List<AdminResponse> fromDomains(List<AdminDomain> domains);

    AdminDomain fromRequest(AdminRequest request);

    AdminDomain fromResponse(AdminResponse response);
}
