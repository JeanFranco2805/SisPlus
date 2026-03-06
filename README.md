# SisPlus — Sistema de Gestión de Personal y Nómina

> Sistema web empresarial para registro de asistencia, cálculo de nómina y gestión de empleados, con integración a lectores biométricos ZKTeco.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Módulos del Sistema](#módulos-del-sistema)
- [API REST](#api-rest)
- [Seguridad](#seguridad)
- [Configuración de Nómina](#configuración-de-nómina)
- [Caché](#caché)
- [Procesamiento Asíncrono](#procesamiento-asíncrono)
- [Integración Biométrica](#integración-biométrica)
- [Instalación y Configuración](#instalación-y-configuración)
- [Variables de Entorno](#variables-de-entorno)
- [Estructura del Proyecto](#estructura-del-proyecto)

---

## Descripción General

**SisPlus** es una aplicación Spring Boot diseñada para empresas colombianas que necesitan:

- Registrar entrada y salida de empleados (manual o mediante lector biométrico)
- Calcular automáticamente la nómina diaria, semanal y mensual según la legislación colombiana vigente
- Exportar reportes de nómina en formato Excel (`.xlsx`)
- Gestionar usuarios, administradores y configuraciones del sistema
- Proteger el acceso con autenticación basada en sesiones y múltiples capas de seguridad

---

## Arquitectura

El proyecto sigue una **arquitectura hexagonal (Ports & Adapters)**, separando claramente las responsabilidades:

```
Presentación (Controllers)
        ↓
Servicios de Aplicación (Services)
        ↓
Puerto de Aplicación (PortAdapter - interfaz)
        ↓
Adaptador de Infraestructura (PortCaseAdapter)
        ↓
Repositorios JPA / Base de Datos
```

### Capas

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| **Domain** | `app.domain` | Entidades de dominio, lógica de negocio pura, cálculos de nómina |
| **Application** | `app.application` | Interfaz `PortAdapter` que define los casos de uso |
| **Infrastructure - Adapter** | `app.infrastructure.adapter` | Implementación del puerto con JPA y caché |
| **Infrastructure - Controller** | `app.infrastructure.controller` | Endpoints REST y vistas Thymeleaf |
| **Infrastructure - Service** | `app.infrastructure.service` | Orquestación de casos de uso |
| **Infrastructure - Security** | `app.infrastructure.security` | Configuración Spring Security, sanitización XSS |
| **Infrastructure - Config** | `app.infrastructure.config` | Caché, rate limiting, async |

---

## Tecnologías

| Tecnología | Versión | Uso |
|------------|---------|-----|
| **Java** | 17+ | Lenguaje principal |
| **Spring Boot** | 3.x | Framework principal |
| **Spring Security** | 6.x | Autenticación y autorización |
| **Spring Data JPA** | 3.x | Persistencia |
| **Hibernate** | 6.x | ORM |
| **MapStruct** | 1.5+ | Mapeo entre capas |
| **Lombok** | 1.18+ | Reducción de boilerplate |
| **Apache POI** | 5.x | Generación de reportes Excel |
| **Bucket4j** | 8.x | Rate limiting |
| **Caffeine Cache** | 3.x | Caché en memoria (desarrollo) |
| **Redis** | 7.x | Caché distribuido (producción) |
| **Thymeleaf** | 3.x | Motor de plantillas HTML |
| **BCrypt** | — | Encriptación de contraseñas (factor 12) |

---

## Módulos del Sistema

### 1. Gestión de Usuarios (Empleados)

Permite crear, actualizar, eliminar y consultar empleados. Cada empleado tiene:
- Nombre, apellido y cédula (única)
- Historial de asistencias asociadas

**Validaciones de entrada:**
- Cédula: solo dígitos, entre 5 y 15 caracteres
- Nombre y apellido: máximo 100 caracteres, sin caracteres maliciosos (sanitización XSS)

---

### 2. Registro de Asistencia

Los registros de asistencia almacenan:
- Hora de entrada (`entryTime`)
- Hora de salida (`departureTime`)
- Referencia al empleado

El sistema detecta automáticamente si ya existe una entrada registrada para el día; en ese caso, registra la salida en lugar de una nueva entrada.

**Filtros disponibles para consultas:**
- Por día específico
- Por rango de fechas
- Por semana o mes
- Filtrados por usuario

---

### 3. Cálculo de Nómina

El cálculo de nómina se realiza con base en las tarifas configuradas en la base de datos (valores Colombia 2026 por defecto):

| Concepto | Tarifa por hora (COP) |
|----------|-----------------------|
| Hora regular | $7.959 |
| Hora extra diurna | $9.948 |
| Recargo nocturno | $2.786 |
| Hora extra nocturna | $13.928,25 |

**Períodos de cálculo soportados:**
- `daily` — nómina del día
- `weekly` — últimos 7 días
- `monthly` — mes completo por mes y año

**Horario nocturno:** 19:00 a 06:00 (configurable)

**Lógica de distribución de horas extras:**
- Las primeras 8 horas se consideran regulares
- Las horas adicionales se clasifican como diurnas o nocturnas según el rango horario

---

### 4. Exportación de Nómina a Excel

Genera un archivo `.xlsx` por mes con formato profesional que incluye:
- Encabezado con título, mes, año y fecha de generación
- Columnas de horas regulares, extras diurnas, extras nocturnas y nocturnas
- Columnas de pago regular, recargos y extras
- Fila de totales con fórmulas Excel
- Formato de moneda y ajuste automático de columnas

**Endpoint:** `GET /api/payroll/export/excel?month=3&year=2026`

---

### 5. Gestión de Administradores

Los administradores son los únicos usuarios con acceso al sistema. Poseen:
- Username único
- Contraseña encriptada con BCrypt (factor 12)
- Roles (`ADMIN`)
- Control de estado: `enabled`, `accountNonExpired`, `accountNonLocked`, `credentialsNonExpired`
- Registro de último login y fecha de creación

**El sistema crea automáticamente un administrador por defecto** (`admin` / `admin123`) si no existe ninguno al iniciar. **Se recomienda cambiar la contraseña inmediatamente en producción.**

---

### 6. Configuración del Sistema

Las configuraciones se almacenan en base de datos y pueden actualizarse sin reiniciar la aplicación:

| Clave | Descripción | Valor por defecto |
|-------|-------------|-------------------|
| `TIME_ZONE` | Zona horaria del sistema | `America/Bogota` |
| `REGULAR_HOUR_RATE` | Tarifa hora regular | `7959` |
| `DAY_OVERTIME_RATE` | Tarifa hora extra diurna | `9948` |
| `NIGHT_SURCHARGE_RATE` | Recargo nocturno por hora | `2786` |
| `NIGHT_OVERTIME_RATE` | Tarifa hora extra nocturna | `13928.25` |
| `NIGHT_START_HOUR` | Inicio horario nocturno (24h) | `19` |
| `NIGHT_END_HOUR` | Fin horario nocturno (24h) | `6` |

---

### 7. Integración con Lector Biométrico

El sistema se integra con dispositivos de control de acceso biométrico (huella dactilar, reconocimiento facial, tarjeta) mediante el protocolo **ZKTeco Push**. Los dispositivos registran las marcaciones directamente en el sistema en tiempo real sin intervención manual.

La integración soporta:
- Registro y actualización automática de dispositivos conectados
- Recepción de marcaciones en tiempo real con identificación del método de verificación
- Cola de comandos para enviar instrucciones a los dispositivos (reinicio, sincronización de usuarios, etc.)
- Procesamiento de logs de asistencia con conversión automática a registros de entrada/salida

> Los endpoints de comunicación con los dispositivos biométricos **no están documentados públicamente** por razones de seguridad.

---

## API REST

Todos los endpoints bajo `/api/**` requieren autenticación, excepto los indicados.

### Autenticación — `/api/auth`

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/api/auth/login` | Público | Iniciar sesión |
| `POST` | `/api/auth/logout` | Autenticado | Cerrar sesión |
| `GET` | `/api/auth/me` | Autenticado | Obtener usuario actual |

**Body de login:**
```json
{
  "username": "admin",
  "password": "tu_contraseña"
}
```

---

### Empleados — `/api/users`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/users` | Listar todos los empleados |
| `POST` | `/api/users` | Crear nuevo empleado |
| `PUT` | `/api/users/{id}` | Actualizar empleado |
| `DELETE` | `/api/users/{id}` | Eliminar empleado |
| `GET` | `/api/users/{id}/payroll` | Calcular nómina |
| `POST` | `/api/users/{id}/entry` | Registrar entrada manualmente |
| `POST` | `/api/users/{id}/exit` | Registrar salida manualmente |

**Parámetros de nómina (`/payroll`):**

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `period` | `daily` / `weekly` / `monthly` | Período de cálculo |
| `date` | `LocalDate` | Fecha específica (daily/weekly) |
| `month` | `Integer` | Mes (monthly) |
| `year` | `Integer` | Año (monthly) |

---

### Asistencias — `/api/attendances`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/attendances` | Listar asistencias con filtros |
| `GET` | `/api/attendances/{id}` | Obtener asistencia por ID |
| `PUT` | `/api/attendances/{id}` | Editar asistencia (entrada/salida) |
| `DELETE` | `/api/attendances/{id}` | Eliminar asistencia |

**Parámetros de filtro:**

| Parámetro | Descripción |
|-----------|-------------|
| `date` | Día específico |
| `startDate` + `endDate` | Rango de fechas |
| `userId` | Filtrar por empleado |
| `filter` | `today`, `week`, `month`, `all` |

---

### Configuración — `/api/config`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/config` | Listar todas las configuraciones |
| `GET` | `/api/config/{key}` | Obtener configuración por clave |
| `POST` | `/api/config` | Crear configuración |
| `PUT` | `/api/config/{key}?value=X` | Actualizar configuración |

---

### Administradores — `/api/admin` *(Requiere rol ADMIN)*

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/admin/` | Listar administradores |
| `GET` | `/api/admin/{username}` | Buscar por username |
| `POST` | `/api/admin` | Crear administrador |
| `DELETE` | `/api/admin/{username}` | Eliminar administrador |

---

### Exportación de Nómina — `/api/payroll`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/payroll/export/excel` | Exportar nómina mensual a Excel |

**Parámetros:** `month` (opcional), `year` (opcional). Si se omiten, usa el mes y año actual.

---

### Vistas Web

| Ruta | Vista |
|------|-------|
| `/` | Pantalla de login |
| `/dashboard` | Panel principal |
| `/employee` | Gestión de empleados |
| `/assistance` | Registro de asistencias |
| `/payroll` | Nómina |
| `/admin` | Administración |
| `/config` | Configuración del sistema |

---

## Seguridad

### Autenticación

- Basada en **sesiones HTTP** con Spring Security
- Protección contra **session fixation** (cambio de ID de sesión al autenticar)
- Máximo **1 sesión activa** por usuario
- Sesión invalidada completamente al hacer logout

### Protección contra Fuerza Bruta

- Tras **5 intentos fallidos** consecutivos desde una misma IP, se bloquea por **15 minutos**
- El contador se limpia automáticamente tras un login exitoso
- Los segundos restantes de bloqueo se informan en la respuesta

### Rate Limiting (Bucket4j)

| Endpoint | Límite |
|----------|--------|
| `/api/auth/login` | 5 requests / 5 minutos |
| `/api/payroll/**` | 10 requests / minuto |
| `/api/users/**` | 30 requests / minuto |
| Resto de `/api/**` | 100 requests / minuto |

### Validación y Sanitización (XSS)

Todos los datos de entrada son sanitizados antes de persistirse:

| Campo | Tipo de sanitización |
|-------|----------------------|
| Nombre, apellido | Solo letras, números y caracteres españoles |
| Cédula | Solo dígitos |
| Username | Alfanumérico, punto, guión |
| Claves de configuración | Solo mayúsculas y guión bajo |
| Valores numéricos | Solo dígitos y punto decimal |

Adicionalmente se eliminan: etiquetas `<script>`, atributos de evento (`onclick`, `onload`, etc.), protocolos peligrosos (`javascript:`, `vbscript:`), etiquetas HTML peligrosas (`<iframe>`, `<object>`, `<form>`, etc.), bytes nulos e inyecciones CRLF.

### Protección contra DoS

- Rechazo de payloads mayores a **1 MB**
- Rechazo de query strings mayores a **500 caracteres**
- Queries a base de datos con **timeout de 5 segundos**
- Consultas de asistencia filtradas en SQL (no en memoria)

### Headers de Seguridad HTTP

```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; ...
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

### CORS

Orígenes permitidos por defecto (ajustar en producción):
- `http://localhost:8080`
- `http://localhost:3000`

### Manejo de Errores

El `GlobalExceptionHandler` controla todas las excepciones:
- En **producción**: mensajes genéricos con un `errorId` para rastrear en logs sin exponer información interna
- En **desarrollo** (`dev` / `local`): se incluye el mensaje de la excepción
- Nunca se expone el stack trace al cliente

---

## Configuración de Nómina

La nómina se calcula usando `PayrollConfiguration`, un objeto inmutable que se carga desde la base de datos al inicio y se cachea por 1 hora.

```
Sueldo diario = Horas regulares × Tarifa regular
             + Horas nocturnas × Recargo nocturno
             + Horas extras diurnas × Tarifa extra diurna
             + Horas extras nocturnas × Tarifa extra nocturna
```

Las tarifas se actualizan en tiempo real a través del endpoint `PUT /api/config/{key}`, sin necesidad de reiniciar el servidor. El caché se invalida automáticamente al modificar cualquier configuración.

---

## Caché

El sistema usa una estrategia de caché en dos modos configurables:

### Modo Caffeine (por defecto — desarrollo/instancia única)

Activado cuando `spring.cache.type=caffeine` (valor por defecto si no se especifica).

| Cache | TTL |
|-------|-----|
| `users` | 10 minutos |
| `userById` | 10 minutos |
| `payrollConfig` | 10 minutos |
| `attendances` | 10 minutos |
| `todayAttendances` | 10 minutos |
| `payrollCalculations` | 10 minutos |

### Modo Redis (producción — entornos distribuidos)

Activado con `spring.cache.type=redis`.

| Cache | TTL |
|-------|-----|
| `users` / `userById` | 15 minutos |
| `payrollConfig` | 1 hora |
| `attendances` | 5 minutos |
| `todayAttendances` | 2 minutos |
| `payrollCalculations` | 30 minutos |

### Estrategia de invalidación

Las operaciones de escritura (crear, actualizar, eliminar) invalidan automáticamente los caches relevantes mediante `@CacheEvict`. Esto garantiza que nunca se sirvan datos desactualizados.

---

## Procesamiento Asíncrono

Los cálculos de nómina pesados (múltiples empleados) se procesan de forma asíncrona con un pool de threads dedicado:

| Pool | Threads mínimos | Threads máximos | Cola |
|------|-----------------|-----------------|------|
| `payrollExecutor` | 5 | 10 | 25 |
| `taskExecutor` (general) | 3 | 6 | 50 |

Si la cola se llena, la tarea se ejecuta en el hilo que la solicitó (política `CallerRunsPolicy`) para evitar la pérdida de datos.

El cálculo masivo de nómina para todos los empleados usa `parallelStream()` dentro del executor dedicado.

---

## Integración Biométrica

SisPlus se integra con dispositivos de lectura biométrica (huella dactilar, reconocimiento facial, tarjeta de proximidad) de la marca ZKTeco mediante el protocolo **Push**.

### Funcionamiento general

1. El dispositivo biométrico establece conexión con el servidor al iniciar
2. El servidor registra el dispositivo y le envía su configuración
3. El dispositivo envía las marcaciones en tiempo real al servidor
4. El servidor procesa cada marcación e identifica al empleado por su PIN (ID de usuario)
5. Se registra automáticamente la entrada o salida según el estado de la marcación

### Métodos de verificación soportados

- Huella dactilar
- Reconocimiento facial
- Tarjeta de proximidad
- Combinaciones (huella + tarjeta, facial + contraseña, etc.)
- Vena del dedo y palma (en dispositivos compatibles)

### Gestión de dispositivos

Desde el panel de administración (`/api/zk/**`, requiere rol ADMIN) es posible:
- Ver todos los dispositivos registrados y su estado (Online/Offline)
- Enviar comandos al dispositivo: reinicio, sincronización de hora, sincronización de usuarios, limpieza de logs
- Consultar información del firmware y capacidades del dispositivo

---

## Instalación y Configuración

### Requisitos previos

- Java 17 o superior
- Maven 3.8+
- Base de datos compatible con JPA (MySQL, PostgreSQL, H2)
- Redis (opcional, solo para producción)

### Pasos de instalación

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-org/sisplus.git
cd sisplus

# 2. Configurar las variables de entorno (ver sección siguiente)

# 3. Compilar el proyecto
mvn clean package -DskipTests

# 4. Ejecutar
java -jar target/sisplus-*.jar
```

### Acceso inicial

Al arrancar por primera vez, el sistema crea automáticamente:
- Las configuraciones de nómina por defecto
- Un administrador por defecto

> ⚠️ **Importante:** Cambia la contraseña del administrador por defecto inmediatamente después del primer inicio.

---

## Variables de Entorno

Configura las siguientes propiedades en `application.properties` o como variables de entorno:

### Base de datos

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sisplus
spring.datasource.username=TU_USUARIO_DB
spring.datasource.password=TU_CONTRASEÑA_DB
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### Caché

```properties
# Desarrollo (Caffeine - por defecto)
spring.cache.type=caffeine

# Producción (Redis)
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Perfil de la aplicación

```properties
# Controla si se exponen detalles de errores al cliente
spring.profiles.active=prod
# Usar "dev" o "local" para mensajes de error detallados en desarrollo
```

### CORS (producción)

Modifica los orígenes permitidos en `SecurityConfig.java`:

```java
config.setAllowedOrigins(List.of(
    "https://tu-dominio.com"
));
```

---

## Estructura del Proyecto

```
src/main/java/com/optical/net/sisplus/
│
├── SisPlusApplication.java                    # Punto de entrada, inicialización de configs
│
└── app/
    ├── application/
    │   └── PortAdapter.java                   # Interfaz de puertos (casos de uso)
    │
    ├── domain/
    │   ├── UserDomain.java                    # Dominio usuario + cálculo de nómina
    │   ├── AttendanceDomain.java              # Dominio asistencia + horas nocturnas
    │   ├── PayrollCalculation.java            # Resultado de cálculo de nómina
    │   ├── PayrollConfiguration.java          # Configuración inmutable de tarifas
    │   ├── ConfigurationDomain.java           # Configuración del sistema
    │   ├── AdminDomain.java                   # Dominio administrador
    │   └── exception/                         # Excepciones de dominio tipadas
    │
    └── infrastructure/
        ├── adapter/
        │   └── PortCaseAdapter.java           # Implementación JPA del puerto
        │
        ├── config/
        │   ├── AsyncConfig.java               # Thread pools asíncronos
        │   ├── CacheConfig.java               # Redis y Caffeine
        │   ├── RateLimitingConfig.java        # Buckets y bloqueo por IP
        │   ├── RateLimitInterceptor.java      # Interceptor HTTP de rate limiting
        │   └── WebMvcConfig.java              # Registro de interceptores
        │
        ├── controller/
        │   ├── api/                           # Controladores REST
        │   │   ├── AuthController.java
        │   │   ├── UserController.java
        │   │   ├── AttendanceController.java
        │   │   ├── ConfigurationController.java
        │   │   ├── PayrollExportController.java
        │   │   ├── AdminController.java
        │   │   └── ZkDeviceController.java
        │   ├── view/
        │   │   └── MainController.java        # Rutas de vistas Thymeleaf
        │   └── exception/
        │       └── GlobalExceptionHandler.java
        │
        ├── entity/                            # Entidades JPA
        │   ├── User.java
        │   ├── Attendance.java
        │   ├── Configuration.java
        │   ├── Admin.java
        │   ├── ZkDevice.java
        │   └── ZkDeviceCommand.java
        │
        ├── mapper/
        │   ├── domains/                       # MapStruct: Entity ↔ Domain
        │   └── response/                      # Mappers: Domain → Response DTO
        │
        ├── repository/                        # Interfaces JPA con queries optimizadas
        │
        ├── security/
        │   ├── SecurityConfig.java            # Configuración Spring Security
        │   ├── CustomUserDetailsService.java  # Carga de usuarios para autenticación
        │   └── XssSanitizer.java              # Sanitización de entradas
        │
        ├── service/
        │   ├── UserService.java
        │   ├── AttendanceService.java
        │   ├── PayrollService.java
        │   ├── PayrollConfigurationService.java
        │   ├── PayrollExportService.java
        │   ├── ConfigurationService.java
        │   ├── AdminService.java
        │   ├── AuthService.java
        │   ├── ZkDeviceService.java
        │   └── AuditService.java
        │
        ├── web/                               # DTOs de Request y Response
        │
        └── zkteco/
            └── ZkTecoConstants.java           # Constantes del protocolo biométrico
```

---

## Auditoría

El sistema registra en un logger dedicado (`AUDIT`) las siguientes acciones:

- Creación, actualización y eliminación de usuarios
- Registros de entrada y salida de asistencia
- Cambios de configuración (clave, valor anterior y nuevo)
- Login exitoso y fallido (con IP)
- Logout
- Cálculos de nómina
- Exportaciones de datos

Los logs de auditoría se escriben con nivel `INFO` (o `WARN` para fallos de login) y pueden dirigirse a un archivo separado configurando el appender correspondiente en `logback.xml`.

---

## Licencia

Propiedad de **Optical Net**. Todos los derechos reservados.
