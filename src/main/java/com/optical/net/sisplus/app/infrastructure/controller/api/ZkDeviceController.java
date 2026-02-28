package com.optical.net.sisplus.app.infrastructure.controller.api;

import com.optical.net.sisplus.app.infrastructure.entity.ZkDevice;
import com.optical.net.sisplus.app.infrastructure.entity.ZkDeviceCommand;
import com.optical.net.sisplus.app.infrastructure.service.ZkDeviceService;
import com.optical.net.sisplus.app.infrastructure.zkteco.ZkTecoConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zk")
@RequiredArgsConstructor
public class ZkDeviceController {

    private final ZkDeviceService deviceService;

    /** Lista todos los dispositivos registrados */
    @GetMapping("/devices")
    public List<ZkDevice> getAllDevices() {
        return deviceService.getAllDevices();
    }

    /** Reiniciar un dispositivo */
    @PostMapping("/devices/{sn}/commands/reboot")
    public ResponseEntity<String> reboot(@PathVariable String sn) {
        deviceService.enqueueCommand(sn, ZkTecoConstants.CMD_REBOOT);
        return ResponseEntity.ok("Comando REBOOT encolado para " + sn);
    }

    /** Sincronizar hora del dispositivo */
    @PostMapping("/devices/{sn}/commands/sync-time")
    public ResponseEntity<String> syncTime(@PathVariable String sn) {
        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern(ZkTecoConstants.DATE_FORMAT));
        deviceService.enqueueCommand(sn, "DATE TIME " + now);
        return ResponseEntity.ok("Comando de hora encolado para " + sn);
    }

    /**
     * Sincronizar un usuario al dispositivo.
     * Body esperado: {"pin":"1","name":"Juan García","pri":0,"passwd":"","card":"","grp":1,"tz":"1","category":0}
     */
    @PostMapping("/devices/{sn}/commands/sync-user")
    public ResponseEntity<String> syncUser(@PathVariable String sn,
                                           @RequestBody Map<String, Object> body) {
        String pin      = String.valueOf(body.getOrDefault("pin", ""));
        String name     = String.valueOf(body.getOrDefault("name", ""));
        int    pri      = Integer.parseInt(String.valueOf(body.getOrDefault("pri", "0")));
        String passwd   = String.valueOf(body.getOrDefault("passwd", ""));
        String card     = String.valueOf(body.getOrDefault("card", ""));
        int    grp      = Integer.parseInt(String.valueOf(body.getOrDefault("grp", "1")));
        String tz       = String.valueOf(body.getOrDefault("tz", "1"));
        int    category = Integer.parseInt(String.valueOf(body.getOrDefault("category", "0")));

        String cmd = ZkTecoConstants.CMD_UPDATE_USERINFO
                .formatted(pin, name, pri, passwd, card, grp, tz, category);

        deviceService.enqueueCommand(sn, cmd);
        return ResponseEntity.ok("Comando de sincronización de usuario encolado para " + sn);
    }

    /** Eliminar un usuario del dispositivo */
    @DeleteMapping("/devices/{sn}/users/{pin}")
    public ResponseEntity<String> deleteUser(@PathVariable String sn,
                                             @PathVariable String pin) {
        String cmd = ZkTecoConstants.CMD_DELETE_USERINFO.formatted(pin);
        deviceService.enqueueCommand(sn, cmd);
        return ResponseEntity.ok("Comando DELETE USERINFO encolado para PIN=" + pin);
    }

    /** Limpiar todos los logs de asistencia del dispositivo */
    @PostMapping("/devices/{sn}/commands/clear-logs")
    public ResponseEntity<String> clearLogs(@PathVariable String sn) {
        deviceService.enqueueCommand(sn, ZkTecoConstants.CMD_CLEAR_LOG);
        return ResponseEntity.ok("Comando CLEAR LOG encolado para " + sn);
    }

    /** Enviar un comando personalizado */
    @PostMapping("/devices/{sn}/commands/raw")
    public ResponseEntity<String> rawCommand(@PathVariable String sn,
                                             @RequestBody Map<String, String> body) {
        String cmd = body.get("command");
        if (cmd == null || cmd.isBlank()) {
            return ResponseEntity.badRequest().body("El campo 'command' es requerido");
        }
        ZkDeviceCommand command = deviceService.enqueueCommand(sn, cmd);
        return ResponseEntity.ok("Comando encolado con ID=" + command.getId());
    }
}