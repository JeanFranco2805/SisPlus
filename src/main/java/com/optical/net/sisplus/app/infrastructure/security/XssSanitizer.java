package com.optical.net.sisplus.app.infrastructure.security;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  PROTECCIÓN XSS — Sanitizador de entradas de usuario
 * ============================================================
 *
 * VULNERABILIDADES QUE CIERRA:
 *  - Inyección de <script> en name, lastName, cc, username
 *  - Atributos de evento HTML (onclick, onload, etc.)
 *  - Protocolos peligrosos (javascript:, vbscript:)
 *  - Etiquetas HTML peligrosas (<iframe>, <object>, etc.)
 *  - Bytes nulos e inyección CRLF
 *
 * CÓMO USAR:
 *  Inyectar en UserService, AdminService y ConfigurationService
 *  y llamar sanitize() antes de persistir cualquier String.
 */
@Component
public class XssSanitizer {

    private static final Pattern SCRIPT_TAG =
            Pattern.compile("<script[^>]*>.*?</script>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern LONE_SCRIPT =
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVAL_EXPR =
            Pattern.compile("(eval|expression)\\s*\\(.*?\\)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern JAVASCRIPT_PROTO =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern VBS_PROTO =
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern ON_EVENT =
            Pattern.compile(
                    "\\bon(load|click|mouseover|mouseout|focus|blur|change|submit" +
                            "|keydown|keyup|keypress|error|abort|input|reset|select" +
                            "|contextmenu|dblclick|drag|drop|touchstart|touchend|wheel)\\s*=",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern DANGEROUS_TAGS =
            Pattern.compile(
                    "<\\s*(iframe|object|embed|form|input|button|select|textarea" +
                            "|link|meta|base|applet|style|layer|ilayer|frame|frameset" +
                            "|bgsound|xml|xss|img)[^>]*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern NULL_BYTES = Pattern.compile("\0");
    private static final Pattern CRLF       = Pattern.compile("[\\r\\n]");
    private static final Pattern SRC_ATTR   =
            Pattern.compile("src\\s*=\\s*['\"][^'\"]*['\"]", Pattern.CASE_INSENSITIVE);



    /**
     * Sanitización general — para name, lastName, cualquier texto libre.
     * Elimina contenido peligroso y codifica entidades HTML.
     */
    public String sanitize(String input) {
        if (input == null) return null;
        String s = input.trim();
        s = NULL_BYTES.matcher(s).replaceAll("");
        s = CRLF.matcher(s).replaceAll(" ");
        s = SCRIPT_TAG.matcher(s).replaceAll("");
        s = LONE_SCRIPT.matcher(s).replaceAll("");
        s = SRC_ATTR.matcher(s).replaceAll("");
        s = EVAL_EXPR.matcher(s).replaceAll("");
        s = JAVASCRIPT_PROTO.matcher(s).replaceAll("");
        s = VBS_PROTO.matcher(s).replaceAll("");
        s = ON_EVENT.matcher(s).replaceAll("");
        s = DANGEROUS_TAGS.matcher(s).replaceAll("");
        return encodeHtml(s);
    }

    /**
     * Sanitización estricta — solo letras, números y caracteres básicos.
     * Usar para: username, nombres, apellidos.
     */
    public String sanitizeAlphanumeric(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s.\\-_]", "");
    }

    /**
     * Sanitización de cédula — solo dígitos.
     */
    public String sanitizeCc(String cc) {
        if (cc == null) return null;
        return cc.trim().replaceAll("[^0-9]", "");
    }

    /**
     * Sanitización de username — solo alfanumérico, punto, guión, guión bajo.
     */
    public String sanitizeUsername(String username) {
        if (username == null) return null;
        return username.trim().replaceAll("[^a-zA-Z0-9._\\-]", "");
    }

    /**
     * Sanitización de clave de configuración — solo mayúsculas y guión bajo.
     */
    public String sanitizeConfigKey(String key) {
        if (key == null) return null;
        return key.trim().replaceAll("[^A-Z0-9_]", "");
    }

    /**
     * Sanitización de valor numérico — solo dígitos y punto decimal.
     */
    public String sanitizeNumericValue(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("[^0-9.]", "");
    }

    private String encodeHtml(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&#x27;");
                case '&'  -> sb.append("&amp;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }
}