package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.mapper.response.AdminResponseMapper;
import com.optical.net.sisplus.app.infrastructure.service.AdminService;
import com.optical.net.sisplus.app.infrastructure.web.AdminRequest;
import com.optical.net.sisplus.app.infrastructure.web.AdminResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminResponseMapper adminResponseMapper;
    private final AdminService adminService;

    public AdminController(AdminResponseMapper adminResponseMapper, AdminService adminService) {
        this.adminResponseMapper = adminResponseMapper;
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<String> register(@RequestBody AdminRequest request) {
        adminService.save(adminResponseMapper.fromRequest(request));
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/")
    public List<AdminResponse> findAll() {
        return adminResponseMapper.fromDomains(adminService.findAllAdmins());
    }
    @GetMapping("/{username}")
    public List<AdminResponse> findByUsername(@PathVariable String username) {
        return adminResponseMapper.fromDomains(adminService.findAllAdmins());
    }
    @DeleteMapping("/{username}")
    public ResponseEntity<String> remove(@PathVariable String username) {
        adminService.removeAdmin(username);
        return ResponseEntity.ok("ok");
    }

}
