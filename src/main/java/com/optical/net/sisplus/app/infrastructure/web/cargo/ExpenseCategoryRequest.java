package com.optical.net.sisplus.app.infrastructure.web.cargo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseCategoryRequest {
    private String name;
    private String color;
    private Boolean active;
}
