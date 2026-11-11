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

## Notas

- **Datos de prueba**: H2 en memoria arranca vacía en cada reinicio. Puedes crear usuarios desde Swagger UI o desde el propio frontend.
- **Google OAuth**: en local no hay credenciales de Google; el login con usuario/contraseña funciona con normalidad. Los endpoints `/auth/google/*` solo fallan si se invocan directamente.
- **WebSockets**: endpoint STOMP en `ws://localhost:8080/ws-chat` (SockJS habilitado).
- **Scripts de despliegue**: ignora `scripts/ec2-deploy-backend.sh`; está pensado para AWS/EC2 y no aplica a ejecución local.
