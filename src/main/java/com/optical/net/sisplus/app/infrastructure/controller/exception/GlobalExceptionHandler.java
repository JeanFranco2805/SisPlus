package com.optical.net.sisplus.app.infrastructure.controller.exception;

import com.optical.net.sisplus.app.domain.exception.DomainException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manejador global de excepciones con seguridad mejorada
 * NO expone stack traces ni información sensible en producción
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Maneja excepciones de dominio personalizadas
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            DomainException ex,
            WebRequest request) {

        String errorId = UUID.randomUUID().toString();
        log.error("Domain exception [{}]: {} - {}", errorId, ex.getErrorCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .path(getPath(request))
                .errorId(errorId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja errores de validación de campos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        String errorId = UUID.randomUUID().toString();
        log.warn("Validation error [{}]: {}", errorId, fieldErrors);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Los datos proporcionados no son válidos")
                .errorCode("VALIDATION_ERROR")
                .path(getPath(request))
                .fieldErrors(fieldErrors)
                .errorId(errorId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Maneja errores de autenticación
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {

        String errorId = UUID.randomUUID().toString();
        // NO loguear detalles de credenciales por seguridad
        log.warn("Authentication failed [{}] from IP: {}", errorId, getClientIP(request));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Credenciales incorrectas")
                .errorCode("INVALID_CREDENTIALS")
                .path(getPath(request))
                .errorId(errorId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Maneja excepciones genéricas NO contempladas
     * CRÍTICO: NO exponer stack trace en producción
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {

        String errorId = UUID.randomUUID().toString();

        // Loguear el error completo solo en servidor
        log.error("Unexpected error [{}]: ", errorId, ex);

        // Mensaje genérico para el cliente (NO exponer detalles)
        String clientMessage = isDevelopment()
                ? ex.getMessage()
                : "Ha ocurrido un error interno. Por favor, contacta al administrador con el código: " + errorId;

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(clientMessage)
                .errorCode("INTERNAL_ERROR")
                .path(getPath(request))
                .errorId(errorId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Maneja IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        String errorId = UUID.randomUUID().toString();
        log.error("Illegal argument [{}]: {}", errorId, ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode("ILLEGAL_ARGUMENT")
                .path(getPath(request))
                .errorId(errorId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getClientIP(WebRequest request) {
        return request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For")
                : "unknown";
    }

    private boolean isDevelopment() {
        return "dev".equalsIgnoreCase(activeProfile) || "local".equalsIgnoreCase(activeProfile);
    }

    /**
     * Clase para respuestas de error estandarizadas y seguras
     */
    @Getter
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String errorCode;
        private String path;
        private String errorId; // Para rastrear en logs sin exponer info
        private Map<String, String> fieldErrors;
    }
}