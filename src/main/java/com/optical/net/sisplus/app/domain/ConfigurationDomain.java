package com.optical.net.sisplus.app.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter

public class ConfigurationDomain {
    private Long id;

    private String key;

    private String value;
}