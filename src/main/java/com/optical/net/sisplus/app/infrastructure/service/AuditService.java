package com.optical.net.sisplus.app.infrastructure.service;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio de auditoría para registrar acciones importantes
 *
 * REGISTRA:
 * - Creación/modificación/eliminación de usuarios
 * - Cambios de configuración
 * - Registros de asistencia
 * - Acciones administrativas
 * - Intentos de acceso
 */
@Slf4j
@Service
public class AuditService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT");

    /**
     * Registra creación de usuario
     */
    public void logUserCreated(Long userId, String cc, String name) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("USER_CREATED")
                .performedBy(getCurrentUsername())
                .entityType("User")
                .entityId(userId)
                .details(String.format("Usuario creado: %s %s (CC: %s)", name, name, cc))
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra actualización de usuario
     */
    public void logUserUpdated(Long userId, String changes) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("USER_UPDATED")
                .performedBy(getCurrentUsername())
                .entityType("User")
                .entityId(userId)
                .details("Cambios: " + changes)
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra eliminación de usuario
     */
    public void logUserDeleted(Long userId, String cc) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("USER_DELETED")
                .performedBy(getCurrentUsername())
                .entityType("User")
                .entityId(userId)
                .details("Usuario eliminado con CC: " + cc)
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra entrada/salida de empleado
     */
    public void logAttendanceRegistered(Long userId, String type, LocalDateTime time) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("ATTENDANCE_" + type.toUpperCase())
                .performedBy("SYSTEM")
                .entityType("Attendance")
                .entityId(userId)
                .details(String.format("%s registrada a las %s", type, time))
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra cambio de configuración
     */
    public void logConfigurationChanged(String key, String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("CONFIG_CHANGED")
                .performedBy(getCurrentUsername())
                .entityType("Configuration")
                .details(String.format("Clave: %s, Valor anterior: %s, Nuevo valor: %s",
                        key, oldValue, newValue))
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra login exitoso
     */
    public void logLoginSuccess(String username, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("LOGIN_SUCCESS")
                .performedBy(username)
                .entityType("Authentication")
                .details("IP: " + ipAddress)
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra intento fallido de login
     */
    public void logLoginFailure(String username, String ipAddress, String reason) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("LOGIN_FAILURE")
                .performedBy(username)
                .entityType("Authentication")
                .details(String.format("IP: %s, Razón: %s", ipAddress, reason))
                .build();

        AUDIT_LOGGER.warn(auditLog.toString());
    }

    /**
     * Registra logout
     */
    public void logLogout(String username) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("LOGOUT")
                .performedBy(username)
                .entityType("Authentication")
                .details("Sesión cerrada")
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra cálculo de nómina
     */
    public void logPayrollCalculated(Long userId, String period, double amount) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("PAYROLL_CALCULATED")
                .performedBy(getCurrentUsername())
                .entityType("Payroll")
                .entityId(userId)
                .details(String.format("Período: %s, Monto: $%.2f", period, amount))
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Registra exportación de datos
     */
    public void logDataExported(String exportType, int recordCount) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .action("DATA_EXPORTED")
                .performedBy(getCurrentUsername())
                .entityType("Export")
                .details(String.format("Tipo: %s, Registros: %d", exportType, recordCount))
                .build();

        AUDIT_LOGGER.info(auditLog.toString());
    }

    /**
     * Obtiene el username del usuario actual autenticado
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener usuario autenticado", e);
        }
        return "SYSTEM";
    }

    /**
     * Clase para representar un log de auditoría
     */
    @Getter
    @Builder
    private static class AuditLog {
        private LocalDateTime timestamp;
        private String action;
        private String performedBy;
        private String entityType;
        private Long entityId;
        private String details;

        @Override
        public String toString() {
            return String.format("[%s] %s por %s | Entidad: %s%s | %s",
                    timestamp,
                    action,
                    performedBy,
                    entityType,
                    entityId != null ? " (ID: " + entityId + ")" : "",
                    details);
        }
    }
}