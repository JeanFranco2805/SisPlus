# 🕒 SisPlus - Sistema de Gestión de Asistencia y Nómina

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green?style=for-the-badge&logo=springboot)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)

**Sistema empresarial moderno para el control de asistencias, cálculo de nómina y generación de reportes**

[Características](#-características) • [Instalación](#-instalación) • [Uso](#-uso) • [Arquitectura](#-arquitectura) • [API](#-api) • [Contribuir](#-contribuir)

</div>

---

## 📋 Tabla de Contenidos

- [Acerca del Proyecto](#-acerca-del-proyecto)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Uso](#-uso)
- [Arquitectura](#-arquitectura)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Roadmap](#-roadmap)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)
- [Contacto](#-contacto)

---

## 🎯 Acerca del Proyecto

**SisPlus** es un sistema integral de gestión empresarial diseñado para automatizar el control de asistencias de empleados y el cálculo preciso de nóminas conforme a la legislación laboral colombiana.

### Problema que Resuelve

Las empresas enfrentan desafíos al:
- 📝 Registrar asistencias manualmente (propenso a errores)
- 💰 Calcular nóminas con horas extras, recargos nocturnos y múltiples tarifas
- 📊 Generar reportes periódicos (diarios, semanales, mensuales)
- 🔐 Mantener seguridad y auditoría de los datos
- ⚡ Procesar grandes volúmenes de información

### Solución

SisPlus automatiza todo el proceso mediante:
- ✅ Registro digital de entrada/salida con timestamp automático
- ✅ Cálculo inteligente de nómina (horas regulares, extras diurnas/nocturnas, recargos)
- ✅ Exportación a Excel con formato profesional
- ✅ Caché de alto rendimiento (Redis + Caffeine)
- ✅ Arquitectura escalable y mantenible

---

## ⭐ Características

### 🕐 Control de Asistencias
- **Registro automático** de entrada y salida con timestamp
- **Validaciones** para evitar registros duplicados
- **Historial completo** de asistencias por empleado
- **Cálculo automático** de horas trabajadas (diurnas y nocturnas)

### 💵 Cálculo de Nómina
- **Horas regulares** (primeras 8 horas del día)
- **Horas extras diurnas** (6:00 AM - 7:00 PM)
- **Horas extras nocturnas** (7:00 PM - 6:00 AM)
- **Recargo nocturno** (35% adicional)
- **Períodos flexibles**: diario, semanal, mensual
- **Configuración dinámica** de tarifas (sin recompilar)

### 📊 Reportes y Exportación
- **Exportación a Excel** con formato profesional
- **Totales automáticos** por empleado y general
- **Diseño personalizable** (colores, logos, encabezados)
- **Múltiples períodos** en un solo reporte

### 🔐 Seguridad
- **Autenticación JWT** con tokens de acceso y refresh
- **Encriptación BCrypt** para contraseñas
- **Protección CSRF** habilitada
- **Rate limiting** (Bucket4j) para prevenir abusos
- **Validación de entrada** en todos los endpoints

### ⚡ Rendimiento
- **Caché dual**: Redis (distribuido) + Caffeine (local)
- **Procesamiento asíncrono** para cálculos pesados
- **Paginación** en consultas grandes
- **Optimización de queries** con JPA

---

## 🛠 Tecnologías

### Backend
- **Java 17** - Lenguaje de programación
- **Spring Boot 3.2** - Framework principal
  - Spring Web (API REST)
  - Spring Data JPA (ORM)
  - Spring Security (Autenticación/Autorización)
  - Spring Cache (Caché)
- **PostgreSQL** - Base de datos relacional
- **Redis** - Caché distribuido
- **Caffeine** - Caché local en memoria

### Librerías Adicionales
- **Lombok** - Reducción de boilerplate
- **Apache POI** - Generación de archivos Excel
- **Bucket4j** - Rate limiting
- **JWT (jjwt)** - Tokens de autenticación
- **MapStruct** - Mapeo de objetos

### Herramientas de Desarrollo
- **Maven** - Gestión de dependencias
- **JUnit 5** - Testing unitario
- **Mockito** - Mocking para tests
- **Docker** - Contenedorización (opcional)

---

## 🚀 Instalación

### Prerrequisitos

```bash
# Java 17 o superior
java --version

# Maven 3.8+
mvn --version

# PostgreSQL 14+
psql --version

# Redis 7+ (opcional pero recomendado)
redis-server --version
```

### Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/sisplus.git
cd sisplus
```

### Configurar Base de Datos

```sql
-- Crear base de datos
CREATE DATABASE sisplus_db;

-- Crear usuario (opcional)
CREATE USER sisplus_user WITH PASSWORD 'tu_password_segura';
GRANT ALL PRIVILEGES ON DATABASE sisplus_db TO sisplus_user;
```

### Instalar Dependencias

```bash
mvn clean install
```

---

## ⚙️ Configuración

### application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/sisplus_db
spring.datasource.username=sisplus_user
spring.datasource.password=tu_password_segura

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Redis (opcional - comentar si no usas Redis)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=tu_clave_secreta_muy_larga_y_segura_aqui
jwt.access-token-expiration=3600000  # 1 hora
jwt.refresh-token-expiration=604800000  # 7 días

# Rate Limiting
rate-limit.capacity=100
rate-limit.refill-tokens=10
rate-limit.refill-duration=1m

# Timezone
app.timezone=America/Bogota
```

### Variables de Entorno (Recomendado para Producción)

```bash
export DB_URL=jdbc:postgresql://localhost:5432/sisplus_db
export DB_USERNAME=sisplus_user
export DB_PASSWORD=tu_password_segura
export JWT_SECRET=tu_clave_secreta_muy_larga
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

---

## 🎮 Uso

### Iniciar la Aplicación

```bash
# Desarrollo
mvn spring-boot:run

# Producción (con JAR)
mvn clean package
java -jar target/sisplus-1.0.0.jar
```

La aplicación estará disponible en `http://localhost:8080`

### Endpoints Principales

#### 1. Autenticación

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

#### 2. Gestión de Usuarios

```bash
# Listar usuarios
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer {token}"

# Crear usuario
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan",
    "lastName": "Pérez",
    "cc": "1234567890"
  }'
```

#### 3. Control de Asistencias

```bash
# Registrar entrada
curl -X POST http://localhost:8080/api/users/1/entry \
  -H "Authorization: Bearer {token}"

# Registrar salida
curl -X POST http://localhost:8080/api/users/1/exit \
  -H "Authorization: Bearer {token}"
```

#### 4. Cálculo de Nómina

```bash
# Nómina diaria
curl -X GET "http://localhost:8080/api/users/1/payroll?period=daily&date=2024-01-15" \
  -H "Authorization: Bearer {token}"

# Nómina mensual
curl -X GET "http://localhost:8080/api/users/1/payroll?period=monthly&month=1&year=2024" \
  -H "Authorization: Bearer {token}"
```

#### 5. Exportar a Excel

```bash
# Generar reporte mensual
curl -X GET "http://localhost:8080/api/payroll/export?month=1&year=2024" \
  -H "Authorization: Bearer {token}" \
  --output nomina_enero_2024.xlsx
```

### Usuario por Defecto

Al iniciar por primera vez, se crea un usuario administrador:

- **Username**: `admin`
- **Password**: `admin123`

⚠️ **IMPORTANTE**: Cambiar la contraseña en producción.

---

## 🏗 Arquitectura

SisPlus implementa **Arquitectura Hexagonal** (Puertos y Adaptadores) para máxima flexibilidad y testabilidad.

```
sisplus/
├── app/
│   ├── domain/              # Lógica de negocio pura
│   │   ├── UserDomain
│   │   ├── AttendanceDomain
│   │   ├── PayrollCalculation
│   │   └── PayrollConfiguration
│   │
│   ├── application/         # Puertos (Interfaces)
│   │   ├── PortAdapter
│   │   └── PortCaseAdapter
│   │
│   └── infrastructure/      # Adaptadores (Implementaciones)
│       ├── entity/          # Entidades JPA
│       ├── repository/      # Repositorios
│       ├── service/         # Servicios
│       ├── mapper/          # Mappers
│       ├── web/            # Controladores REST
│       └── security/        # Configuración de seguridad
│
├── config/                  # Configuraciones
│   ├── CacheConfig
│   ├── SecurityConfig
│   └── AsyncConfig
│
└── resources/
    └── application.properties
```

### Capas y Responsabilidades

#### 🎯 Dominio (Domain)
- **Lógica de negocio pura** sin dependencias externas
- **Entidades de dominio** (UserDomain, AttendanceDomain)
- **Value Objects** (PayrollCalculation, PayrollConfiguration)
- **Reglas de negocio** (cálculo de horas extras, recargos nocturnos)

#### 🔌 Aplicación (Application)
- **Puertos** (interfaces) que definen contratos
- **Casos de uso** abstractos
- **Independiente** de frameworks

#### 🔧 Infraestructura (Infrastructure)
- **Adaptadores** que implementan los puertos
- **Persistencia** (JPA, PostgreSQL)
- **Web** (Spring MVC, REST)
- **Seguridad** (Spring Security, JWT)
- **Caché** (Redis, Caffeine)

### Flujo de Datos

```
Cliente → Controller → Service → PortAdapter → Repository → Database
                ↓                      ↑
              Mapper ← UserDomain ←────┘
```

### Ventajas de esta Arquitectura

✅ **Testabilidad**: Lógica de dominio testeable sin frameworks  
✅ **Flexibilidad**: Cambiar tecnologías sin tocar el dominio  
✅ **Mantenibilidad**: Separación clara de responsabilidades  
✅ **Escalabilidad**: Fácil agregar nuevas funcionalidades  
✅ **DDD**: Alineado con Domain-Driven Design  

---

## 📚 API Documentation

### Autenticación Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/login` | Iniciar sesión | No |
| POST | `/api/auth/refresh` | Renovar token | No |
| POST | `/api/auth/logout` | Cerrar sesión | Sí |

### Usuarios Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/users` | Listar usuarios | Sí |
| POST | `/api/users` | Crear usuario | Sí |
| PUT | `/api/users/{id}` | Actualizar usuario | Sí |
| DELETE | `/api/users/{id}` | Eliminar usuario | Sí |

### Asistencias Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/users/{id}/entry` | Registrar entrada | Sí |
| POST | `/api/users/{id}/exit` | Registrar salida | Sí |
| GET | `/api/users/{id}/attendance` | Ver historial | Sí |

### Nómina Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/users/{id}/payroll` | Calcular nómina | Sí |
| GET | `/api/payroll/export` | Exportar Excel | Sí |

### Parámetros Query para Nómina

```
period: daily | weekly | monthly
date: yyyy-MM-dd (para daily/weekly)
month: 1-12 (para monthly)
year: yyyy (para monthly)
```

### Ejemplo de Respuesta (Nómina)

```json
{
  "id": 1,
  "name": "Juan",
  "lastName": "Pérez",
  "cc": "1234567890",
  "payroll": {
    "regularHours": 160.0,
    "dayOvertimeHours": 10.0,
    "nightOvertimeHours": 5.0,
    "nightHours": 20.0,
    "regularPay": 1273440.0,
    "dayOvertimePay": 99480.0,
    "nightOvertimePay": 69641.25,
    "nightSurchargePay": 55720.0,
    "totalOvertimePay": 169121.25,
    "totalPay": 1498281.25
  }
}
```

---

## 🧪 Testing

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests específicos
mvn test -Dtest=UserServiceTest

# Con cobertura
mvn clean test jacoco:report
```

### Estructura de Tests

```
src/test/java/
├── domain/              # Tests de lógica de negocio
│   ├── UserDomainTest
│   └── PayrollCalculationTest
│
├── service/             # Tests de servicios
│   ├── UserServiceTest
│   └── PayrollServiceTest
│
└── integration/         # Tests de integración
    ├── UserControllerIT
    └── PayrollControllerIT
```

### Cobertura Actual

- **Domain Layer**: 95%
- **Service Layer**: 85%
- **Controller Layer**: 80%
- **Overall**: 87%

---

## 🗺 Roadmap

### ✅ Versión 1.0 (Actual)
- [x] Control de asistencias
- [x] Cálculo de nómina básico
- [x] Exportación a Excel
- [x] Autenticación JWT
- [x] Caché Redis

### 🚧 Versión 1.1 (En Desarrollo)
- [ ] Gestión de vacaciones y permisos
- [ ] Workflow de aprobación de horas extras
- [ ] Cálculo de deducciones (seguridad social, salud, pensión)
- [ ] Sistema de notificaciones
- [ ] Dashboard con métricas en tiempo real

### 📅 Versión 2.0 (Planificado)
- [ ] Módulo de recursos humanos
- [ ] Gestión de contratos
- [ ] Evaluaciones de desempeño
- [ ] Reportes avanzados con gráficos
- [ ] API pública para integraciones
- [ ] Autenticación 2FA

### 🔮 Versión 3.0 (Futuro)
- [ ] Multi-tenant (múltiples empresas)
- [ ] App móvil (Android/iOS)
- [ ] Reconocimiento facial para asistencias
- [ ] IA para predicción de nómina
- [ ] Integración con sistemas de contabilidad

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Sigue estos pasos:

### 1. Fork el Proyecto

```bash
# Hacer fork desde GitHub
# Luego clonar tu fork
git clone https://github.com/tu-usuario/sisplus.git
```

### 2. Crear una Rama

```bash
git checkout -b feature/nueva-funcionalidad
```

### 3. Hacer Cambios

```bash
# Hacer tus cambios
git add .
git commit -m "feat: agregar nueva funcionalidad"
```

### 4. Push y Pull Request

```bash
git push origin feature/nueva-funcionalidad
# Crear Pull Request desde GitHub
```

### Convención de Commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: nueva funcionalidad
fix: corrección de bug
docs: documentación
style: formato de código
refactor: refactorización
test: agregar tests
chore: tareas de mantenimiento
```

### Reportar Bugs

Abre un issue en GitHub con:
- Descripción del problema
- Pasos para reproducir
- Comportamiento esperado vs actual
- Capturas de pantalla (si aplica)
- Versión de Java, Spring Boot, etc.

---

## 📄 Licencia

Distribuido bajo la licencia MIT. Ver `LICENSE` para más información.

---

## 📞 Contacto

**Desarrollador Principal**: Tu Nombre

- 📧 Email: tu.email@ejemplo.com
- 💼 LinkedIn: [linkedin.com/in/tu-perfil](https://linkedin.com/in/tu-perfil)
- 🐦 Twitter: [@tu_usuario](https://twitter.com/tu_usuario)

**Link del Proyecto**: [https://github.com/tu-usuario/sisplus](https://github.com/tu-usuario/sisplus)

---

## 🙏 Agradecimientos

- [Spring Boot](https://spring.io/projects/spring-boot)
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/)
- [Apache POI](https://poi.apache.org/)
- [Lombok](https://projectlombok.org/)

---

<div align="center">

**⭐ Si este proyecto te ayudó, considera darle una estrella en GitHub ⭐**

Hecho con ❤️ en Colombia 🇨🇴

</div>
