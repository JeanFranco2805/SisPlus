package com.optical.net.sisplus.app.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra un usuario
 */
public class DomainExceptions extends DomainException {
    public DomainExceptions(Long userId) {
        super("USER_NOT_FOUND", "Usuario no encontrado con ID: " + userId);
    }

    public DomainExceptions(String cc) {
        super("USER_NOT_FOUND", "Usuario no encontrado con cédula: " + cc);
    }
}

/**
 * Excepción lanzada cuando se intenta crear un usuario con cédula duplicada
 */
class DuplicateUserException extends DomainException {
    public DuplicateUserException(String cc) {
        super("DUPLICATE_USER", "Ya existe un usuario con la cédula: " + cc);
    }
}

/**
 * Excepción lanzada cuando no se encuentra una asistencia
 */
class AttendanceNotFoundException extends DomainException {
    public AttendanceNotFoundException(Long attendanceId) {
        super("ATTENDANCE_NOT_FOUND", "Asistencia no encontrada con ID: " + attendanceId);
    }
}

/**
 * Excepción para operaciones inválidas de asistencia
 */
class InvalidAttendanceOperationException extends DomainException {
    public InvalidAttendanceOperationException(String message) {
        super("INVALID_ATTENDANCE_OPERATION", message);
    }
}

/**
 * Excepción para configuraciones inválidas
 */
class InvalidConfigurationException extends DomainException {
    public InvalidConfigurationException(String configKey, String reason) {
        super("INVALID_CONFIGURATION",
                "Configuración inválida para '" + configKey + "': " + reason);
    }
}

/**
 * Excepción para fechas inválidas en asistencias
 */
class InvalidAttendanceDateException extends DomainException {
    public InvalidAttendanceDateException(String message) {
        super("INVALID_ATTENDANCE_DATE", message);
    }
}

/**
 * Excepción cuando ya existe una entrada registrada
 */
class DuplicateEntryException extends DomainException {
    public DuplicateEntryException(Long userId) {
        super("DUPLICATE_ENTRY",
                "El usuario " + userId + " ya tiene una entrada registrada para hoy");
    }
}

/**
 * Excepción cuando se intenta registrar salida sin entrada
 */
class MissingEntryException extends DomainException {
    public MissingEntryException(Long userId) {
        super("MISSING_ENTRY",
                "El usuario " + userId + " no tiene una entrada registrada para hoy");
    }
}

/**
 * Excepción para administradores
 */
class AdminNotFoundException extends DomainException {
    public AdminNotFoundException(String username) {
        super("ADMIN_NOT_FOUND", "Administrador no encontrado: " + username);
    }
}

class DuplicateAdminException extends DomainException {
    public DuplicateAdminException(String username) {
        super("DUPLICATE_ADMIN", "Ya existe un administrador con username: " + username);
    }
}