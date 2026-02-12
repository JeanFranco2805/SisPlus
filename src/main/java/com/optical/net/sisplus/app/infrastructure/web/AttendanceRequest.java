package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRequest {
    private Long userId;
    private LocalDateTime entryTime;
    private LocalDateTime departureTime;
}