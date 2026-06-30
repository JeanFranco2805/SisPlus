package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ExpenseCategoryResponse {
    private Long id;
    private String name;
    private String color;
    private boolean active;
    private LocalDateTime createdAt;
}
