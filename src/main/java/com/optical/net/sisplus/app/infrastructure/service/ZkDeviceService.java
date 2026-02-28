package com.optical.net.sisplus.app.infrastructure.service;

import com.optical.net.sisplus.app.infrastructure.entity.ZkDevice;
import com.optical.net.sisplus.app.infrastructure.entity.ZkDeviceCommand;
import com.optical.net.sisplus.app.infrastructure.entity.ZkDeviceCommand.CommandStatus;
import com.optical.net.sisplus.app.infrastructure.repository.ZkDeviceCommandRepository;
import com.optical.net.sisplus.app.infrastructure.repository.ZkDeviceRepository;
import com.optical.net.sisplus.app.infrastructure.zkteco.ZkTecoConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio que gestiona los dispositivos ZKTeco y su cola de comandos.
 * Portar la lógica de DeviceManager + DeviceCommandManager del SDK original.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZkDeviceService {

    private final ZkDeviceRepository deviceRepository;
    private final ZkDeviceCommandRepository commandRepository;

    // =========================================================
    //  Gestión de dispositivos
    // =========================================================

    /**
     * Registra o actualiza un dispositivo cuando hace handshake.
     * Si el SN ya existe, actualiza su estado e IP.
     * Si es nuevo, lo crea con valores por defecto.
     */
    @Transactional
    public ZkDevice registerOrUpdate(String deviceSn, String ipAddress,
                                     String pushVersion, String language) {
        return deviceRepository.findByDeviceSn(deviceSn)
                .map(existing -> {
                    existing.setIpAddress(ipAddress);
                    existing.setState("Online");
                    existing.setLastActivity(LocalDateTime.now());
                    if (pushVersion != null) existing.setPushVersion(pushVersion);
                    if (language != null) existing.setDevLanguage(language);
                    log.info("[ZKTeco] Dispositivo actualizado: SN={}, IP={}", deviceSn, ipAddress);
                    return deviceRepository.save(existing);
                })
                .orElseGet(() -> {
                    ZkDevice newDevice = ZkDevice.builder()
                            .deviceSn(deviceSn)
                            .deviceName(deviceSn + " (" + ipAddress + ")")
                            .ipAddress(ipAddress)
                            .state("Online")
                            .pushVersion(pushVersion)
                            .devLanguage(language)
                            .attLogStamp("None")
                            .opLogStamp("9999")
                            .attPhotoStamp("None")
                            .build();
                    ZkDevice saved = deviceRepository.save(newDevice);
                    log.info("[ZKTeco] Nuevo dispositivo registrado: SN={}, IP={}", deviceSn, ipAddress);
                    // Encolar INFO para obtener datos del dispositivo
                    enqueueCommand(deviceSn, ZkTecoConstants.CMD_INFO);
                    return saved;
                });
    }

    /**
     * Genera la respuesta de configuración para el handshake inicial.
     * El dispositivo usa estos parámetros para saber cómo conectarse.
     */
    public String buildHandshakeResponse(ZkDevice device) {
        // ATTLOGStamp=None → enviar todos los logs desde el principio
        // ATTLOGStamp=<timestamp> → enviar solo logs posteriores a ese timestamp
        return "GET OPTION FROM: " + device.getDeviceSn() + "\n"
                + "ATTLOGStamp=" + device.getAttLogStamp() + "\n"
                + "OPERLOGStamp=" + device.getOpLogStamp() + "\n"
                + "ATTPHOTOStamp=" + device.getAttPhotoStamp() + "\n"
                + "ErrorDelay=30\n"
                + "Delay=10\n"
                + "TransTimes=00:00;14:00\n"
                + "TransInterval=1\n"
                + "TransFlag=1111000000\n"
                + "TimeZone=5\n"       // UTC-5 (Colombia)
                + "Realtime=1\n"
                + "Encrypt=None\n";
    }

    /**
     * Actualiza el stamp de ATTLOG después de procesar registros exitosamente.
     * El stamp es el timestamp del último registro procesado.
     */
    @Transactional
    public void updateAttLogStamp(String deviceSn, String lastTimestamp) {
        deviceRepository.findByDeviceSn(deviceSn).ifPresent(device -> {
            device.setAttLogStamp(lastTimestamp);
            deviceRepository.save(device);
            log.debug("[ZKTeco] ATTLOGStamp actualizado a '{}' para SN={}", lastTimestamp, deviceSn);
        });
    }

    /**
     * Actualiza el INFO del dispositivo cuando responde al comando INFO.
     * Formato: "FirmwareVer, userCount, fpCount, attCount, IP, fpAlg, faceAlg, ..."
     */
    @Transactional
    public void updateDeviceInfo(String deviceSn, String infoString) {
        deviceRepository.findByDeviceSn(deviceSn).ifPresent(device -> {
            try {
                String[] parts = infoString.split(",");
                if (parts.length >= 1) device.setFirmwareVersion(parts[0].trim());
                if (parts.length >= 2) device.setUserCount(parseInt(parts[1]));
                if (parts.length >= 3) device.setFpCount(parseInt(parts[2]));
                if (parts.length >= 4) device.setTransCount(parseInt(parts[3]));
                if (parts.length >= 6) device.setFpAlgVersion(parts[5].trim());
                if (parts.length >= 7) device.setFaceAlgVersion(parts[6].trim());
                device.setLastActivity(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("[ZKTeco] INFO actualizado para SN={}: firmware={}, users={}",
                        deviceSn, device.getFirmwareVersion(), device.getUserCount());
            } catch (Exception e) {
                log.warn("[ZKTeco] Error procesando INFO para SN={}: {}", deviceSn, e.getMessage());
            }
        });
    }

    /** Marca el dispositivo como Offline */
    @Transactional
    public void markOffline(String deviceSn) {
        deviceRepository.updateState(deviceSn, "Offline", LocalDateTime.now());
    }

    public List<ZkDevice> getAllDevices() {
        return deviceRepository.findAll();
    }

    // =========================================================
    //  Gestión de la cola de comandos
    // =========================================================

    /**
     * Encola un comando para ser enviado al dispositivo en su próximo heartbeat.
     *
     * Ejemplos:
     *   // Sincronizar usuario
     *   String cmd = ZkTecoConstants.CMD_UPDATE_USERINFO
     *       .formatted("1", "Juan García", 0, "", "", 1, "1", 0);
     *   deviceService.enqueueCommand("SN123456", cmd);
     *
     *   // Reiniciar dispositivo
     *   deviceService.enqueueCommand("SN123456", ZkTecoConstants.CMD_REBOOT);
     */
    @Transactional
    public ZkDeviceCommand enqueueCommand(String deviceSn, String cmdContent) {
        ZkDeviceCommand command = ZkDeviceCommand.builder()
                .deviceSn(deviceSn)
                .cmdContent(cmdContent)
                .status(CommandStatus.PENDING)
                .build();
        ZkDeviceCommand saved = commandRepository.save(command);
        log.info("[ZKTeco] Comando encolado para SN={}: {}", deviceSn, cmdContent);
        return saved;
    }

    /**
     * Construye la respuesta al heartbeat GET /iclock/getrequest.
     * Si hay comandos pendientes, los formatea y marca como SENT.
     * Si no hay, devuelve "OK".
     *
     * Formato de cada comando: "C:<id>:<content>\n"
     */
    @Transactional
    public String buildGetRequestResponse(String deviceSn) {
        List<ZkDeviceCommand> pending = commandRepository
                .findByDeviceSnAndStatusOrderByCommitTimeAsc(deviceSn, CommandStatus.PENDING);

        if (pending.isEmpty()) {
            return "OK";
        }

        StringBuilder sb = new StringBuilder();
        List<Long> ids = new java.util.ArrayList<>();

        for (ZkDeviceCommand cmd : pending) {
            sb.append(ZkTecoConstants.CMD_TITLE)
                    .append(cmd.getId()).append(":")
                    .append(cmd.getCmdContent()).append("\n");
            ids.add(cmd.getId());
        }

        // Marcar como SENT
        commandRepository.markAsSent(ids, LocalDateTime.now());
        log.info("[ZKTeco] Enviando {} comandos a SN={}", pending.size(), deviceSn);

        return sb.toString();
    }

    /**
     * Procesa la respuesta del dispositivo al ejecutar un comando.
     * El dispositivo hace POST /iclock/devicecmd con:
     *   ID=<cmdId>&Return=<code>&CMD=<content>
     */
    @Transactional
    public void processCommandResponse(String deviceSn, String id, String returnCode, String returnInfo) {
        try {
            Long cmdId = Long.parseLong(id);
            commandRepository.findSentCommand(deviceSn, cmdId).ifPresentOrElse(
                    cmd -> {
                        cmd.setReturnCode(returnCode);
                        cmd.setReturnInfo(returnInfo);
                        cmd.setOverTime(LocalDateTime.now());
                        cmd.setStatus("0".equals(returnCode) ? CommandStatus.COMPLETED : CommandStatus.FAILED);
                        commandRepository.save(cmd);
                        log.info("[ZKTeco] Comando ID={} para SN={} completado con Return={}",
                                cmdId, deviceSn, returnCode);
                    },
                    () -> log.warn("[ZKTeco] No se encontró comando SENT con ID={} para SN={}", cmdId, deviceSn)
            );
        } catch (NumberFormatException e) {
            log.warn("[ZKTeco] ID de comando inválido: {}", id);
        }
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}