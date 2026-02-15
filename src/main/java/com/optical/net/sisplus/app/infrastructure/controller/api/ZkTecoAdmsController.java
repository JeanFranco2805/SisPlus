// src/main/java/com/optical/net/sisplus/app/infrastructure/controller/api/ZkTecoAdmsController.java
package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Implementa el protocolo ADMS/Push de ZKTeco.
 *
 * Flujo:
 *  1. El F22 hace GET /iclock/cdata?SN=XXX  → respondemos con config del servidor
 *  2. El F22 hace POST /iclock/cdata         → envía marcaciones (attendance logs)
 *  3. El F22 hace GET /iclock/getrequest     → consulta si hay comandos pendientes
 */
@Slf4j
@RestController
@RequestMapping("/iclock")
public class ZkTecoAdmsController {

    private final UserService userService;

    // Formato de fecha que usa el F22: "2025-02-14 08:30:00"
    private static final DateTimeFormatter ZK_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ZkTecoAdmsController(UserService userService) {
        this.userService = userService;
    }

    /**
     * HANDSHAKE / CONFIGURACIÓN INICIAL
     *
     * El dispositivo hace GET /iclock/cdata?SN=<serial>
     * Respondemos con parámetros de conexión en texto plano.
     * Esta respuesta es CRÍTICA: le dice al F22 cómo conectarse.
     */
    @GetMapping(value = "/cdata", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handshake(
            @RequestParam(value = "SN", required = false) String serialNumber,
            @RequestParam(value = "options", required = false) String options) {

        log.info("[ZKTeco] Handshake recibido - SN: {}", serialNumber);

        // Respuesta de configuración que el dispositivo espera
        // Cada línea es un parámetro clave=valor
        String response = "GET OPTION FROM: " + serialNumber + "\n" +
                "ATTLOGStamp=None\n" +         // Sin marca de tiempo previa (sync completo)
                "OPERLOGStamp=9999\n" +
                "ATTPHOTOStamp=None\n" +
                "ErrorDelay=30\n" +             // Segundos de espera ante error
                "Delay=10\n" +                  // Intervalo de heartbeat (segundos)
                "TransTimes=00:00;14:00\n" +    // Ventanas de sincronización
                "TransInterval=1\n" +           // Minutos entre sync
                "TransFlag=1111000000\n" +       // Flags: usuarios, marcaciones, etc.
                "TimeZone=5\n" +                // UTC-5 (Colombia)
                "Realtime=1\n" +               // Envío en tiempo real
                "Encrypt=None\n";              // Sin cifrado (desarrollo)

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(response);
    }

    /**
     * RECEPCIÓN DE DATOS (MARCACIONES Y USUARIOS)
     *
     * El F22 hace POST /iclock/cdata?SN=XXX&table=ATTLOG
     * El body contiene líneas de texto con las marcaciones.
     *
     * Formato de una línea de ATTLOG:
     *   PIN\tTimestamp\tStatus\tVerify\tWorkCode\tReserved
     *   Ejemplo: "1234\t2025-02-14 08:30:00\t0\t1\t0\t0"
     *
     * PIN    = ID del usuario en el dispositivo (= tu campo cc o id)
     * Status = 0=Check-In, 1=Check-Out, 4=Overtime-In, 5=Overtime-Out
     */
    @PostMapping(value = "/cdata", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receiveData(
            @RequestParam(value = "SN", required = false) String serialNumber,
            @RequestParam(value = "table", required = false) String table,
            @RequestParam(value = "Stamp", required = false) String stamp,
            @RequestParam(value = "OpStamp", required = false) String opStamp,
            @RequestParam(value = "PhotoStamp", required = false) String photoStamp,
            HttpServletRequest request) {

        try {
            String body = new BufferedReader(
                    new InputStreamReader(request.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));

            log.info("[ZKTeco] POST /cdata - SN:{}, table:{}, body:\n{}", serialNumber, table, body);

            if ("ATTLOG".equalsIgnoreCase(table)) {
                processAttendanceLogs(body, serialNumber);
            } else if ("OPERLOG".equalsIgnoreCase(table)) {
                log.debug("[ZKTeco] OPERLOG recibido (ignorado): {}", body);
            } else {
                log.debug("[ZKTeco] Tabla desconocida: {} - body: {}", table, body);
            }

            // El dispositivo ESPERA exactamente "OK" para confirmar recepción
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("[ZKTeco] Error procesando cdata", e);
            return ResponseEntity.ok("OK"); // Siempre responder OK para no atascar el dispositivo
        }
    }

    /**
     * HEARTBEAT / COLA DE COMANDOS
     *
     * El F22 hace GET /iclock/getrequest?SN=XXX periódicamente.
     * Aquí puedes responder con comandos para el dispositivo.
     * Si no hay comandos, responder "OK".
     */
    @GetMapping(value = "/getrequest", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRequest(
            @RequestParam(value = "SN", required = false) String serialNumber,
            @RequestParam(value = "INFO", required = false) String info) {

        log.debug("[ZKTeco] Heartbeat - SN: {}, INFO: {}", serialNumber, info);

        // Sin comandos pendientes → responder OK
        // Para enviar un comando al dispositivo, responder con el comando aquí.
        // Ejemplo para sincronizar hora:
        //   return ResponseEntity.ok("C:1:DATE TIME " + LocalDateTime.now().format(ZK_DATE_FORMAT));
        return ResponseEntity.ok("OK");
    }

    /**
     * RESPUESTA A COMANDOS
     * El dispositivo responde aquí cuando ejecutó un comando enviado en /getrequest
     */
    @PostMapping(value = "/devicecmd", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deviceCommandResponse(HttpServletRequest request) {
        try {
            String body = new BufferedReader(
                    new InputStreamReader(request.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            log.info("[ZKTeco] Respuesta a comando: {}", body);
        } catch (Exception e) {
            log.error("[ZKTeco] Error en devicecmd", e);
        }
        return ResponseEntity.ok("OK");
    }

    // ----------------------------------------------------------
    //  Lógica de procesamiento de marcaciones
    // ----------------------------------------------------------

    /**
     * Parsea y procesa cada línea de un ATTLOG.
     *
     * Formato: PIN\tFecha\tStatus\tVerify\tWorkCode\tReserved
     *
     * PIN en el dispositivo debe coincidir con el ID del usuario en tu BD.
     * Si usas CC (cédula) como PIN en el dispositivo, ajusta la lógica abajo.
     */
    private void processAttendanceLogs(String body, String serialNumber) {
        if (body == null || body.isBlank()) return;

        String[] lines = body.split("\n");
        log.info("[ZKTeco] Procesando {} marcaciones del dispositivo {}", lines.length, serialNumber);

        for (String line : lines) {
            line = line.trim();
            if (line.isBlank()) continue;

            try {
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    log.warn("[ZKTeco] Línea con formato inesperado: {}", line);
                    continue;
                }

                String pin        = parts[0].trim();   // ID del usuario en el dispositivo
                String timestamp  = parts[1].trim();   // "2025-02-14 08:30:00"
                int status        = parts.length > 2 ? parseIntSafe(parts[2]) : 0;
                // Status: 0=Check-In, 1=Check-Out

                log.info("[ZKTeco] Marcación: PIN={}, Time={}, Status={}", pin, timestamp, status);

                Long userId = parsePinToUserId(pin);
                if (userId == null) {
                    log.warn("[ZKTeco] No se pudo mapear PIN {} a un usuario", pin);
                    continue;
                }

                // Delegar al servicio existente de asistencia
                if (status == 0) {
                    // Check-In → registrar entrada
                    userService.registerEntry(userId);
                    log.info("[ZKTeco] ✅ Entrada registrada para usuario ID: {}", userId);
                } else if (status == 1) {
                    // Check-Out → registrar salida
                    userService.registerExit(userId);
                    log.info("[ZKTeco] ✅ Salida registrada para usuario ID: {}", userId);
                } else {
                    log.info("[ZKTeco] Status {} no manejado para PIN {}", status, pin);
                }

            } catch (Exception e) {
                log.error("[ZKTeco] Error procesando línea '{}': {}", line, e.getMessage());
            }
        }
    }

    /**
     * Convierte el PIN del dispositivo al ID de usuario en tu base de datos.
     *
     * IMPORTANTE: El PIN que programas en el F22 para cada empleado
     * debe coincidir con el ID (Long) del usuario en tu BD.
     *
     * Alternativa: si usas la CC como PIN, cambia este método para
     * buscar por CC en el repositorio.
     */
    private Long parsePinToUserId(String pin) {
        try {
            return Long.parseLong(pin);
        } catch (NumberFormatException e) {
            log.warn("[ZKTeco] PIN no numérico: {}", pin);
            return null;
        }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}