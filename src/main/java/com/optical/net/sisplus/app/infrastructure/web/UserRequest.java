package com.optical.net.sisplus.app.infrastructure.web;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserRequest {

    private Long id;
    private String name;
    private String lastName;
    private String cc;

    private LocalDate date;
    private Integer month;
    private Integer year;
}
