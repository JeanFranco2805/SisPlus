package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AttendanceResponse {
    private Long id;
    private UserResponse user;
    private LocalDateTime entryTime;
    private LocalDateTime departureTime;
    private double workedHours;
    private double nightHours;
    private double extraHours;
}