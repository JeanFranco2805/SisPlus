package com.optical.net.sisplus.app.infrastructure.mapper;

import com.optical.net.sisplus.app.domain.FootPrintsDomain;
import com.optical.net.sisplus.app.infrastructure.entity.FootPrints;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface FootPrintsMapper {

    FootPrintsDomain toDomain(FootPrints footPrints);

    FootPrints toEntity(FootPrintsDomain domain);

}
