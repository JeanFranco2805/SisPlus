# SisPlus — Sistema de Control de Asistencia y Nómina

Sistema web de gestión de asistencia y cálculo de nómina para empresas colombianas. Permite registrar entradas/salidas, calcular pagos (horas regulares, extras diurnas, extras nocturnas y recargos), exportar reportes en Excel y administrar usuarios con autenticación segura.

---

## Características

- **Control de asistencia** — Registro de entradas y salidas por empleado con zona horaria configurable (America/Bogota por defecto)
- **Cálculo de nómina** — Cálculo diario, semanal y mensual con soporte para horas regulares, extras diurnas, extras nocturnas y recargo nocturno
- **Exportación Excel** — Generación de reportes de nómina en `.xlsx` con formato profesional usando Apache POI
- **Configuración dinámica** — Tarifas y horarios configurables desde el panel de administración (sin redesplegar)
- **Autenticación** — Login con sesión HTTP y Spring Security. Rate limiting en endpoints sensibles
- **Caché** — Soporte para Caffeine (desarrollo) y Redis (producción)
- **Procesamiento asíncrono** — Cálculo masivo de nómina con pool de threads dedicado
- **Auditoría** — Logger de auditoría para todas las acciones relevantes del sistema

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security |
| Persistencia | Spring Data JPA, Hibernate |
| Base de datos | Compatible con H2 / PostgreSQL / MySQL |
| Caché | Caffeine (dev) / Redis (prod) |
| Mapeo | MapStruct, Lombok |
| Excel | Apache POI |
| Rate Limiting | Bucket4j |
| Frontend | Thymeleaf, HTML, CSS, JavaScript vanilla |

---

## Estructura del proyecto

```
src/
└── main/
    ├── java/com/optical/net/sisplus/
    │   ├── app/
    │   │   ├── application/        # Puerto (interfaz PortAdapter)
    │   │   ├── domain/             # Entidades de dominio y lógica de negocio
    │   │   └── infrastructure/
    │   │       ├── adapter/        # Implementación del puerto
    │   │       ├── config/         # Caché, async, rate limiting, MVC
    │   │       ├── controller/     # REST API + controladores de vistas
    │   │       ├── entity/         # Entidades JPA
    │   │       ├── mapper/         # MapStruct mappers
    │   │       ├── repository/     # Spring Data repositories
    │   │       ├── security/       # Spring Security
    │   │       ├── service/        # Servicios de aplicación
    │   │       └── web/            # DTOs (Request / Response)
    │   └── SisPlusApplication.java
    └── resources/
        └── templates/              # Vistas Thymeleaf
```

---

## Configuración

Al iniciar, la aplicación crea automáticamente los parámetros de nómina por defecto en base de datos (Colombia 2026):

| Clave | Valor por defecto |
|---|---|
| `REGULAR_HOUR_RATE` | 7.959 COP |
| `DAY_OVERTIME_RATE` | 9.948 COP |
| `NIGHT_SURCHARGE_RATE` | 2.786 COP |
| `NIGHT_OVERTIME_RATE` | 13.928,25 COP |
| `NIGHT_START_HOUR` | 19:00 |
| `NIGHT_END_HOUR` | 06:00 |
| `TIME_ZONE` | America/Bogota |

Estos valores se pueden modificar desde `/config` en la interfaz web o mediante la API REST en `/api/config`.

---

## API REST — Endpoints principales

### Usuarios
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/users` | Listar empleados |
| `POST` | `/api/users` | Crear empleado |
| `PUT` | `/api/users/{id}` | Actualizar empleado |
| `DELETE` | `/api/users/{id}` | Eliminar empleado |
| `POST` | `/api/users/{id}/entry` | Registrar entrada |
| `POST` | `/api/users/{id}/exit` | Registrar salida |
| `GET` | `/api/users/{id}/payroll` | Calcular nómina (`?period=daily\|weekly\|monthly`) |

### Asistencias
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/attendances` | Listar asistencias (filtros: `date`, `startDate`, `endDate`, `filter`, `userId`) |
| `GET` | `/api/attendances/{id}` | Obtener asistencia por ID |
| `PUT` | `/api/attendances/{id}` | Editar asistencia |
| `DELETE` | `/api/attendances/{id}` | Eliminar asistencia |

### Nómina
| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/payroll/export/excel` | Exportar nómina mensual (`?month=&year=`) |

### Autenticación
| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión |
| `POST` | `/api/auth/logout` | Cerrar sesión |
| `GET` | `/api/auth/me` | Usuario autenticado actual |

---

## Ejecución

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/sisplus.git
cd sisplus

# Compilar y ejecutar
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.  
Credenciales por defecto: **admin / admin123**

> Se recomienda cambiar la contraseña del administrador por defecto antes de desplegar en producción.

---

## Despliegue con Redis (producción)

Añadir en `application.properties`:

```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

---

## Arquitectura

El proyecto sigue una arquitectura **hexagonal (ports & adapters)**:

- **Domain** — Lógica de negocio pura (`UserDomain`, `AttendanceDomain`, `PayrollConfiguration`). Sin dependencias de infraestructura.
- **Application** — Puerto `PortAdapter` que define los contratos.
- **Infrastructure** — Adaptadores de JPA, controladores REST, servicios de aplicación, seguridad y configuración.
