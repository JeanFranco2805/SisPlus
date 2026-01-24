package com.optical.net.sisplus.app.infrastructure.web;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRequest {
    private Long id;
    private String username;
    private String password;
}
