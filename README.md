# Back_Plang — Backend de SmartTrip

API REST construida con **Java 17 + Spring Boot** (Web, Security/JWT, JPA, WebSocket/STOMP, Flyway, SpringDoc/OpenAPI).

Este README explica cómo ejecutar el backend **en local** junto con su frontend [`Front_WEB_PLAG`](https://github.com/lRicardol/Front_WEB_PLAG), usando el perfil `local`. No se necesita PostgreSQL, AWS ni ninguna clave: toda la configuración local está en el repositorio.

---

## Requisitos

| Herramienta | Versión | Cómo comprobarla |
|---|---|---|
| Java (JDK) | 17 o superior | `java -version` |
| Apache Maven | 3.9 o superior | `mvn -v` |
| Node.js + npm | 20 o superior (solo para el frontend) | `node -v` |
| Git | cualquiera | `git --version` |

Los comandos funcionan igual en **Windows** (PowerShell o Git Bash), **macOS** y **Linux**.

---

## Paso 1 — Ejecutar el backend

```bash
git clone https://github.com/lRicardol/Back_Plang.git
cd Back_Plang
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

La primera vez tarda unos minutos porque descarga las dependencias. Está listo cuando aparece `Started TourismPlatformApplication`.

### Qué configura el perfil `local`

El archivo [`src/main/resources/application-local.yml`](src/main/resources/application-local.yml) está versionado y **no contiene secretos reales**:

- **Base de datos H2 en memoria** (`jdbc:h2:mem:tourism_platform`): no hay que instalar PostgreSQL. Los datos se borran al apagar el backend.
- **Flyway desactivado** y esquema generado por Hibernate, porque las migraciones de `db/migration/` usan sintaxis de PostgreSQL.
- **Amadeus en modo simulado**: el catálogo responde sin credenciales externas.
- **Clave JWT de desarrollo**: solo sirve en local. En otros entornos la clave se define con la variable de entorno `JWT_SECRET`, nunca en el repositorio.
- **CORS y WebSocket abiertos para `http://localhost:*`**, para que el frontend pueda conectarse.
- **API en la raíz** (`http://localhost:8080`), sin el prefijo `/api/v1`.

### Comprobar que funciona

| Recurso | URL |
|---|---|
| Health check | http://localhost:8080/actuator/health → debe responder `{"status":"UP"}` |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Consola H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:tourism_platform`, usuario `sa`, sin contraseña) |

Para probar endpoints protegidos en Swagger, primero inicia sesión con `POST /users/login` y usa el `token` devuelto en el botón **Authorize**.

---

## Paso 2 — Ejecutar el frontend

En otra terminal:

```bash
git clone https://github.com/lRicardol/Front_WEB_PLAG.git
cd Front_WEB_PLAG
npm install
```

Crea el archivo `.env` a partir del ejemplo:

```bash
# macOS / Linux / Git Bash
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Y arráncalo:

```bash
npm run dev
```

Abre http://localhost:5173. Más detalles en el README de [`Front_WEB_PLAG`](https://github.com/lRicardol/Front_WEB_PLAG).

---

## Flujo rápido de prueba

1. Backend encendido → http://localhost:8080/actuator/health responde `UP`.
2. Frontend encendido → abre http://localhost:5173.
3. **Regístrate** y luego **inicia sesión con usuario y contraseña**. El inicio de sesión con Google no funciona en local porque requiere credenciales de Google Cloud que no se guardan en el repositorio.
4. Crea un plan de viaje y agrégale actividades.

## Ejecutar las pruebas

```bash
mvn test
```

## Notas

- Los scripts de `scripts/` (por ejemplo `ec2-deploy-backend.sh`) son del despliegue en AWS del semestre anterior y no se usan en local.
- Los valores reales de producción (base de datos, `JWT_SECRET`, credenciales de Google o Amadeus) se pasan por variables de entorno y nunca se suben al repositorio.

---

## Punto de partida y código reutilizado

Este backend es la **evolución de un proyecto existente**: la plataforma SmartTrip, desarrollada el semestre anterior (2026-1) por un equipo de 5 integrantes. En la asignatura Ingeniería de Software la retoman 3 integrantes.

**Estado inicial:** tag [`v0-estado-inicial`](https://github.com/lRicardol/Back_Plang/tree/v0-estado-inicial) (commit `f55a1ea`, 11 de noviembre de 2026). Para ver todo lo que cambió desde entonces: [comparar v0-estado-inicial con main](https://github.com/lRicardol/Back_Plang/compare/v0-estado-inicial...main).

**Qué se heredó (ya existía al iniciar la asignatura):**
- API REST en Java 17 + Spring Boot con usuarios y JWT, planes de viaje, actividades, módulo social y chat por WebSocket.
- Health check con Actuator y logs estructurados en JSON con traceId.
- Migraciones de base de datos con Flyway y perfil local con H2 en memoria.
- Pruebas unitarias con JUnit y Mockito.

**Qué se agregó o cambió en la asignatura:**
- Análisis estático con SonarCloud en GitHub Actions (`.github/workflows/sonarqube-analysis.yml`).
- Análisis de dependencias con OWASP Dependency-Check en GitHub Actions, con reporte HTML y alertas en Security (`.github/workflows/dependency-check.yml`).
- Corrección de dependencias vulnerables: actualización a Spring Boot 4.2, controlador PostgreSQL 42.7.14 y springdoc 3.2.
- Ajuste de pruebas y configuración de Jackson para la nueva versión de Spring Boot.

## Librerías de terceros y licencias

Principales dependencias directas. Todas tienen licencias de código abierto que permiten su uso en este proyecto.

- Spring Boot, Spring Security y Spring Cloud (web, websocket, data-jpa, security, validation, actuator, oauth2-client, test) — Apache-2.0
- Micrometer (Prometheus) — Apache-2.0
- Flyway — Apache-2.0
- jjwt (JSON Web Tokens) — Apache-2.0
- MapStruct — Apache-2.0
- springdoc-openapi (Swagger UI) — Apache-2.0
- Jackson (databind, jsr310) — Apache-2.0
- Lombok — MIT
- Controlador JDBC de PostgreSQL — BSD-2-Clause
- H2 Database — MPL-2.0 / EPL-1.0

La lista completa de dependencias (incluidas las transitivas) se obtiene con `mvn dependency:tree`.


## Notas

- **Datos de prueba**: H2 en memoria arranca vacía en cada reinicio. Puedes crear usuarios desde Swagger UI o desde el propio frontend.
- **Google OAuth**: en local no hay credenciales de Google; el login con usuario/contraseña funciona con normalidad. Los endpoints `/auth/google/*` solo fallan si se invocan directamente.
- **WebSockets**: endpoint STOMP en `ws://localhost:8080/ws-chat` (SockJS habilitado).
- **Scripts de despliegue**: ignora `scripts/ec2-deploy-backend.sh`; está pensado para AWS/EC2 y no aplica a ejecución local.
