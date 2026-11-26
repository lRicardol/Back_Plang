# Back_Plang — Backend Tourism Intelligent Platform

API REST construida con **Java 17 + Spring Boot 3.5** (Web, Security/JWT, JPA, WebSocket/STOMP, Flyway, SpringDoc/OpenAPI).

Este README explica cómo levantar el backend **100% en local (Windows)** junto con su frontend [`Front_WEB_PLAG`](https://github.com/lRicardol/Front_WEB_PLAG), usando el perfil `local` (`src/main/resources/application-local.yml`). No se requiere AWS, PostgreSQL ni ningún secreto en la nube.

---

## Requisitos previos

| Herramienta | Versión | Notas |
|---|---|---|
| Java (JDK) | 17+ | Si no tienes un JDK global en el `PATH`, puedes usar el **JBR de PyCharm**: `C:\Program Files\JetBrains\PyCharm 2026.2.3\jbr` |
| Apache Maven | 3.9+ | Si no está instalado globalmente, se puede usar la copia local en `~/tools/apache-maven-3.9.16` |
| Node.js + npm | 18+ | Solo necesario para el frontend (Vite) |
| Git Bash o PowerShell | — | Los comandos de abajo están en sintaxis Bash (Git Bash en Windows) |

---

## Paso 1 — Encender el Backend en modo local

### Qué incluye el perfil `local`

El archivo `src/main/resources/application-local.yml` configura:

- **Base de datos H2 en memoria**: `jdbc:h2:mem:tourism_platform` (no necesitas instalar PostgreSQL).
- **Hibernate `ddl-auto: update`**: el esquema se genera desde las entidades; Flyway queda desactivado porque las migraciones `db/migration/` usan sintaxis PostgreSQL.
- **Amadeus en modo mock** (`app.integrations.amadeus.mock-mode: true`): `/catalog/*` devuelve JSON con formato Amadeus sin credenciales ni llamadas externas.
- **Swagger UI / OpenAPI sin token**: la documentación interactiva está abierta en el perfil local.
- **CORS abierto a localhost**: acepta peticiones desde `http://localhost` en **cualquier puerto** (incluye `http://localhost:5173` del frontend y WebSockets).
- **JWT secret local por defecto**: no necesitas definir `JWT_SECRET` ni `DB_*`.
- **Sin `context-path`**: la API queda servida directamente en la raíz (`http://localhost:8080/...`).

### Comandos (Git Bash)

```bash
cd "C:\Users\richa\PycharmProjects\Back_Plang"

# JDK (JBR de PyCharm) y Maven locales — omite estas líneas si ya tienes java/mvn en el PATH
export JAVA_HOME="/c/Program Files/JetBrains/PyCharm 2026.2.3/jbr"
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.16/bin:$PATH"

# Arrancar el backend con el perfil local
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

> Alternativa sin exports: configura `JAVA_HOME` y `MAVEN_HOME` como variables de entorno de Windows y ejecuta solo `mvn spring-boot:run -Dspring-boot.run.profiles=local`.

### URLs útiles una vez encendido

| Recurso | URL |
|---|---|
| Swagger UI (documentación interactiva de la API) | http://localhost:8080 → redirige a `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Consola H2 (inspeccionar datos) | http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:tourism_platform`, usuario `sa`, contraseña vacía |
| Health check | http://localhost:8080/actuator/health |

La base de datos H2 es **en memoria**: los datos se pierden al reiniciar el backend. Para probar endpoints protegidos (`/travel-plans/**`, `/social/**`, `/catalog/**`) inicia sesión con `POST /users/login` (campo `usernameOrEmail`) y usa el `token` devuelto en el botón **Authorize** de Swagger UI.

---

## Paso 2 — Conectar y encender el Frontend (`Front_WEB_PLAG`)

### 2.1 Clonar u abrir el proyecto

```bash
# Si aún no lo tienes:
git clone https://github.com/lRicardol/Front_WEB_PLAG.git
cd Front_WEB_PLAG

# O simplemente abre la carpeta existente:
cd "C:\Users\richa\PycharmProjects\Front_WEB_PLAG"
```

### 2.2 Instalar dependencias

```bash
npm install
```

### 2.3 Configurar la URL del API

El frontend lee la URL base desde la variable de entorno **`VITE_API_BASE_URL`** (ver `src/services/api.js`):

```js
baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1'
```

Crea un archivo `.env` en la raíz de `Front_WEB_PLAG`:

```env
# Backend local (Back_Plang con perfil local, API en la raíz, sin /api/v1)
VITE_API_BASE_URL=http://localhost:8080

# Opcional: chat por WebSocket STOMP en vivo (si no se define, el chat usa solo REST)
VITE_WS_BROKER_URL=ws://localhost:8080/ws-chat
```

> Nota: si el backend corriera con el `context-path` por defecto (`/api/v1`), la variable debería ser `http://localhost:8080/api/v1`. Con el perfil `local` el context-path está vacío, por eso va sin sufijo.

### 2.4 Arrancar el frontend

```bash
npm run dev
```

Vite levantará la app en **http://localhost:5173**. El backend ya tiene habilitado **CORS y WebSockets** para ese origen (`app.cors.allowed-origin-patterns: http://localhost:*` y `app.websocket.allowed-origin-patterns` en `application-local.yml`), así que no hay que tocar nada más.

---

## Flujo rápido de prueba

1. Backend arriba (`mvn spring-boot:run -Dspring-boot.run.profiles=local`) → `http://localhost:8080/actuator/health` debe responder `{"status":"UP"}`.
2. Frontend arriba (`npm run dev`) → abre http://localhost:5173.
3. **Registra un usuario o inicia sesión** desde la UI (`http://localhost:5173/login`). El registro usa `POST /users/register` y el login `POST /users/login` contra `http://localhost:8080`.
4. Verifica la integración:
   - En Swagger UI (`http://localhost:8080/swagger-ui/index.html`) el endpoint aparece en verde y responde 200/201.
   - En la consola H2 (`http://localhost:8080/h2-console`) ejecuta `SELECT * FROM users;` y verás el usuario recién creado.
   - En DevTools del navegador → pestaña Network, las peticiones irán a `http://localhost:8080/...` con el header `Authorization: Bearer <token>` tras el login.

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
