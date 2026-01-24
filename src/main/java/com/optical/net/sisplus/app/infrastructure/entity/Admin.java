package com.optical.net.sisplus.app.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Admin {
    @Id
    private Long id;
    private String username;
    private String password;
}
