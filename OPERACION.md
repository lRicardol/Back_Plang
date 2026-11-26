# Operación: verificar que SmartTrip funciona y volver a una versión estable

Este documento explica cómo comprobar que la solución está funcionando (health check y logs) y cómo regresar a la última versión funcional si un cambio la rompe.

---

## 1. Verificar que el backend funciona

### Health check

Con el backend encendido (`mvn spring-boot:run -Dspring-boot.run.profiles=local`):

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

- `UP`: la aplicación está arriba y responde.
- Si no responde o el estado no es `UP`, revisar los logs (sección siguiente).

También se puede abrir http://localhost:8080/actuator/health en el navegador.

### Logs estructurados

El backend escribe los logs en formato JSON en la consola y en el archivo `logs/tourism-platform.log` (dentro de la carpeta del backend). Cada petición HTTP deja una línea como esta:

```json
{"timestamp":"2026-11-26T08:27:27.139-05:00","level":"INFO","logger":"c.t.p.o.TraceRequestFilter","traceId":"prueba-traza-123","requestId":"prueba-traza-123","userId":"","message":"event=request_completed method=POST path=/users/login status=401 durationMs=63 endpoint=/users/login statusLabel=ERROR"}
```

Qué se puede leer en cada línea:

- `traceId`: identificador de la petición. Permite seguir todo lo que pasó en esa petición.
- `method`, `path`, `status`: qué se pidió y con qué resultado (200 correcto, 4xx error del cliente, 5xx error del servidor).
- `durationMs`: cuánto tardó la respuesta.
- `userId`: usuario autenticado, si lo hay.

### Rastrear una petición concreta

Enviar un `traceId` propio en el encabezado `X-Trace-Id` y buscarlo en el log:

```bash
curl -H "X-Trace-Id: prueba-traza-123" http://localhost:8080/actuator/health
grep "prueba-traza-123" logs/tourism-platform.log
```

Para ver solo los errores:

```bash
grep '"level":"ERROR"' logs/tourism-platform.log
```

## 2. Verificar que el frontend funciona

- Con `npm run dev`, abrir http://localhost:5173: debe cargar la página de inicio.
- Iniciar sesión y abrir "Mis viajes": si carga, el frontend se está comunicando con el backend.
- Si algo falla, abrir las herramientas del navegador (F12) → pestañas **Console** y **Network** para ver el error y el código de respuesta del backend.

## 3. Verificación automática en cada cambio

Cada push y pull request hacia `main` ejecuta en GitHub Actions:

- **SonarQube Cloud Analysis**: compila, corre las pruebas y analiza la calidad.
- **OWASP Dependency-Check**: revisa dependencias con vulnerabilidades conocidas.

Si un check sale en rojo, el cambio no se integra a `main`.

---

## 4. Volver a la última versión funcional (rollback)

### Versiones estables

Cada cierre de sprint queda marcado con un tag de Git:

- `v0-estado-inicial`: el proyecto heredado al iniciar la asignatura.
- `v1.0-sprint1`: cierre del Sprint 1.
- `v2.0-sprint2`: cierre del Sprint 2.

Para ver los tags: `git tag`.

### Opción A: ejecutar rápidamente la última versión estable

Útil para recuperar el servicio mientras se corrige el problema.

```bash
git fetch --tags
git checkout v2.0-sprint2
mvn spring-boot:run -Dspring-boot.run.profiles=local   # backend
# o en el frontend: npm install && npm run dev
```

Verificar con el health check que responde `UP`. Para volver al trabajo normal: `git checkout main`.

### Opción B: deshacer en main el cambio que rompió la aplicación

Es la forma correcta de corregir `main` sin borrar historia.

1. Identificar el commit que causó el fallo: `git log --oneline`.
2. Crear una rama y revertir ese commit:

```bash
git checkout main
git pull
git checkout -b fix/revert-<descripcion>
git revert <commit>
git push -u origin fix/revert-<descripcion>
```

Si el cambio entró con un merge de un pull request, se revierte el commit de merge: `git revert -m 1 <commit-del-merge>`. GitHub también ofrece el botón **Revert** en la página del pull request integrado.

3. Abrir un pull request, esperar los checks en verde, revisión de otro integrante y merge.
4. Verificar con el health check y el recorrido principal de la aplicación.

---

## 5. Prueba del procedimiento de rollback

**Fecha:** [completar] · **Responsable:** [completar]

**Qué se hizo:** [completar, por ejemplo: se ejecutó la Opción A con `git checkout v2.0-sprint2` en el backend, se arrancó con el perfil local y se verificó el health check].

**Resultado:** [completar, por ejemplo: el health check respondió UP en X segundos; luego se volvió a main con `git checkout main`].
