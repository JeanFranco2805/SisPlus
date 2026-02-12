package com.optical.net.sisplus.app.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "Name must contain only letters")
    private String name;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "Last name must contain only letters")
    private String lastName;

    @NotBlank(message = "Document number is required")
    @Size(min = 6, max = 20, message = "Document number must be between 6 and 20 characters")
    @Pattern(regexp = "^[0-9]+$", message = "Document number must contain only digits")
    private String cc;

    @Size(max = 200, message = "Position must not exceed 200 characters")
    private String position;

    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;
}