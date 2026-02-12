package com.optical.net.sisplus.app.infrastructure.mapper.response;

import com.optical.net.sisplus.app.domain.AttendanceDomain;
import com.optical.net.sisplus.app.infrastructure.web.AttendanceResponse;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;

import java.util.List;
import java.util.stream.Collectors;

public class AttendanceResponseMapper {

    public static AttendanceResponse toResponse(AttendanceDomain att) {
        double extraHours = 0;
        if (att.getEntryTime() != null && att.getDepartureTime() != null) {
            extraHours = Math.max(0, att.getWorkedHours() - 8);
        }

        return AttendanceResponse.builder()
                .id(att.getId())
                .user(UserResponse.builder()
                        .id(att.getUser().getId())
                        .name(att.getUser().getName())
                        .lastName(att.getUser().getLastName())
                        .cc(att.getUser().getCc())
                        .build())
                .entryTime(att.getEntryTime())
                .departureTime(att.getDepartureTime())
                .workedHours(att.getDepartureTime() != null ? att.getWorkedHours() : 0)
                .nightHours(att.getDepartureTime() != null ? att.getNightHours() : 0)
                .extraHours(extraHours)
                .build();
    }

    public static List<AttendanceResponse> toResponseList(List<AttendanceDomain> list) {
        return list.stream()
                .map(AttendanceResponseMapper::toResponse)
                .collect(Collectors.toList());
    }
}

