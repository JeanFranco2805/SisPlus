package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.service.UserService;
import com.optical.net.sisplus.app.infrastructure.web.UserRequest;
import com.optical.net.sisplus.app.infrastructure.web.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser( @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/payroll")
    public UserResponse calculatePayroll(
            @PathVariable Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LocalDate date
    ) {
        return userService.calculatePayroll(id, date, month, year);
    }

    @GetMapping("/{id}/extra-hours")
    public double getExtraHours(
            @PathVariable Long id,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return switch (period.toLowerCase()) {
            case "daily" -> userService.getDailyExtraHours(id, date);
            case "weekly" -> userService.getWeeklyExtraHours(id, date);
            case "monthly" -> userService.getMonthlyExtraHours(id, month, year);
            default -> throw new IllegalArgumentException("Invalid period: " + period);
        };
    }

    @GetMapping("/{id}/night-surcharge")
    public double getNightSurcharge(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate date
    ) {
        return userService.getNightSurcharge(id, date);
    }

    @PostMapping("/{id}/entry")
    public ResponseEntity<String> registerEntry(@PathVariable Long id) {
        userService.registerEntry(id);
        return ResponseEntity.ok("Entrada registrada exitosamente");
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUserById(id);
        if (deleted) {
            return ResponseEntity.ok("Usuario eliminado exitosamente");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest request
    ) {
        UserResponse updatedUser = userService.updateUser(id, request);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
