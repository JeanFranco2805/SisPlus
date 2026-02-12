package com.optical.net.sisplus.app.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @PastOrPresent(message = "Entry time cannot be in the future")
    private LocalDateTime entryTime;

    @PastOrPresent(message = "Departure time cannot be in the future")
    private LocalDateTime departureTime;
}