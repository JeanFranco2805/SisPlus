package com.optical.net.sisplus.app.infrastructure.zkteco;

/**
 * Constantes del protocolo Push de ZKTeco.
 * Portado desde Constants.java del SDK oficial (Pushdemo04042022).
 *
 * Uso de los comandos de formato:
 *   String cmd = ZkTecoConstants.CMD_UPDATE_USERINFO
 *       .formatted(pin, name, pri, passwd, card, grp, tz, category);
 */
public final class ZkTecoConstants {

    private ZkTecoConstants() {}

    // =========================================================
    //  Prefijo de comandos
    // =========================================================

    /** Todos los comandos al dispositivo tienen este prefijo + ID + ":" */
    public static final String CMD_TITLE = "C:";

    // =========================================================
    //  Comandos de control
    // =========================================================

    public static final String CMD_REBOOT        = "REBOOT";
    public static final String CMD_CHECK         = "CHECK";
    public static final String CMD_INFO          = "INFO";
    public static final String CMD_CLEAR_LOG     = "CLEAR LOG";
    public static final String CMD_CLEAR_PHOTO   = "CLEAR PHOTO";
    public static final String CMD_CLEAR_DATA    = "CLEAR DATA";
    public static final String CMD_CLEAR_BIODATA = "CLEAR BIODATA";
    public static final String CMD_AC_UNLOCK     = "AC_UNLOCK %d";    // %d = lockNum
    public static final String CMD_SHELL         = "SHELL %s";        // %s = command
    public static final String CMD_SET_OPTION    = "SET OPTION %s";   // %s = key=value
    public static final String CMD_RELOAD_OPTION = "RELOAD OPTION";

    // =========================================================
    //  Comandos DATA UPDATE — enviar datos al dispositivo
    // =========================================================

    /**
     * Sincronizar usuario al dispositivo.
     * Parámetros: PIN, Name, Pri (privilege: 0=user/14=admin),
     *             Passwd, Card, Grp (group 1-5), TZ (timezones), Category
     */
    public static final String CMD_UPDATE_USERINFO =
            "DATA UPDATE USERINFO PIN=%s\tName=%s\tPri=%d\tPasswd=%s\tCard=%s\tGrp=%d\tTZ=%s\tCategory=%d";

    /**
     * Sincronizar plantilla de huella dactilar.
     * Parámetros: PIN, FID (finger index 0-9), Size, Valid (1=normal/3=duress), TMP (Base64)
     */
    public static final String CMD_UPDATE_FINGERTMP =
            "DATA UPDATE FINGERTMP PIN=%s\tFID=%d\tSize=%d\tValid=%d\tTMP=%s";

    /**
     * Sincronizar plantilla de rostro.
     * Parámetros: PIN, FID, Size, Valid, TMP (Base64 con 16 bytes aleatorios como prefijo)
     */
    public static final String CMD_UPDATE_FACE =
            "DATA UPDATE FACE PIN=%s\tFID=%d\tSize=%d\tValid=%d\tTMP=%s";

    /**
     * Sincronizar foto de perfil del usuario.
     * Parámetros: PIN, Size, Content (Base64 de la foto JPG)
     */
    public static final String CMD_UPDATE_USERPIC =
            "DATA UPDATE USERPIC PIN=%s\tSize=%d\tContent=%s";

    /**
     * Sincronizar datos biométricos unificados (protocolo híbrido, v2.2.14+).
     * Parámetros: Pin, No (índice de plantilla), Index, Valid, Duress,
     *             Type (1=FP/2=Face/9=VL-Face), MajorVer, MinorVer, Format, Tmp (Base64)
     */
    public static final String CMD_UPDATE_BIODATA =
            "DATA UPDATE BIODATA Pin=%s\tNo=%d\tIndex=%d\tValid=%d\tDuress=%d\tType=%d\tMajorVer=%d\tMinorVer=%d\tFormat=%d\tTmp=%s";

    /**
     * Sincronizar foto biométrica de comparación.
     * Parámetros: PIN, Type, Size, Content (Base64)
     */
    public static final String CMD_UPDATE_BIOPHOTO =
            "DATA UPDATE BIOPHOTO PIN=%s\tType=%d\tSize=%d\tContent=%s";

    /** Enviar mensaje SMS al dispositivo. */
    public static final String CMD_UPDATE_SMS =
            "DATA UPDATE SMS MSG=%s\tTAG=%d\tUID=%d\tMIN=%d\tStartTime=%s";

    // =========================================================
    //  Comandos DATA DELETE — eliminar datos en el dispositivo
    // =========================================================

    public static final String CMD_DELETE_USERINFO  = "DATA DELETE USERINFO PIN=%s";
    public static final String CMD_DELETE_FINGERTMP = "DATA DELETE FINGERTMP PIN=%s\tFID=%d";
    public static final String CMD_DELETE_FACE      = "DATA DELETE FACE PIN=%s\tFID=%d";
    public static final String CMD_DELETE_USERPIC   = "DATA DELETE USERPIC PIN=%s";
    public static final String CMD_DELETE_BIODATA   = "DATA DELETE BIODATA Pin=%s\tType=%d";
    public static final String CMD_DELETE_SMS       = "DATA DELETE SMS UID=%d";
    public static final String CMD_CLEAR_USERINFO   = "CLEAR ALL USERINFO";

    // =========================================================
    //  Comandos DATA QUERY — consultar datos en el dispositivo
    // =========================================================

    /** Consultar logs de asistencia por rango de fechas. */
    public static final String CMD_QUERY_ATTLOG =
            "DATA QUERY ATTLOG StartTime=%s\tEndTime=%s";

    public static final String CMD_QUERY_USERINFO  = "DATA QUERY USERINFO PIN=%s";
    public static final String CMD_QUERY_FINGERTMP = "DATA QUERY FINGERTMP PIN=%s";

    // =========================================================
    //  Enrollment remoto
    // =========================================================

    public static final String CMD_ENROLL_FP  = "ENROLL_FP PIN=%s\tFID=%d\tOverwrite=%d";
    public static final String CMD_ENROLL_BIO = "ENROLL_BIO PIN=%s\tType=%d\tID=%d\tOverwrite=%d";

    // =========================================================
    //  Tablas de datos del dispositivo (table= en POST /cdata)
    // =========================================================

    public static final String TABLE_ATTLOG  = "ATTLOG";
    public static final String TABLE_OPERLOG = "OPERLOG";
    public static final String TABLE_ATTPHOTO  = "ATTPHOTO";
    public static final String TABLE_BIODATA   = "BIODATA";
    public static final String TABLE_ERRORLOG  = "ERRORLOG";
    public static final String TABLE_OPTIONS   = "options";

    // =========================================================
    //  Prefijos en OPERLOG
    // =========================================================

    public static final String OPERLOG_PREFIX_OPLOG    = "OPLOG";
    public static final String OPERLOG_PREFIX_USER     = "USER";
    public static final String OPERLOG_PREFIX_FP       = "FP";
    public static final String OPERLOG_PREFIX_FACE     = "FACE";
    public static final String OPERLOG_PREFIX_BIOPHOTO = "BIOPHOTO";
    public static final String OPERLOG_PREFIX_USERPIC  = "USERPIC";

    // =========================================================
    //  Status de marcación en ATTLOG
    // =========================================================

    /** Status 0: Check-In (Entrada) */
    public static final int ATT_STATUS_CHECK_IN       = 0;
    /** Status 1: Check-Out (Salida) */
    public static final int ATT_STATUS_CHECK_OUT      = 1;
    /** Status 4: Break-Out */
    public static final int ATT_STATUS_BREAK_OUT      = 4;
    /** Status 5: Break-In */
    public static final int ATT_STATUS_BREAK_IN       = 5;
    /** Status 255: Auto (el dispositivo decide según lógica interna) */
    public static final int ATT_STATUS_AUTO           = 255;

    // =========================================================
    //  Idiomas del dispositivo
    // =========================================================

    public static final String DEV_LANG_ZH_CN = "83";  // Chino Simplificado
    public static final String DEV_LANG_EN    = "69";  // Inglés
    public static final String DEV_LANG_ES    = "34";  // Español

    // =========================================================
    //  Formato de fecha del protocolo Push
    // =========================================================

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}