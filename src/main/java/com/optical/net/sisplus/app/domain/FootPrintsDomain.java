package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Builder
@Getter
@Setter
public class FootPrintsDomain {
    private LocalDateTime date;
    private UserDomain user;
    private byte [] template;
}
