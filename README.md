# Backend · Sistema de Gestión de Órdenes

API REST para el taller de rectificación. Construida con Java 21, Spring Boot 3.5, Spring Security, JPA, Flyway y PostgreSQL.

## Módulos incluidos

- Autenticación JWT y roles `ADMIN` / `OPERADOR`.
- Usuarios con contraseñas BCrypt.
- Clientes con múltiples vehículos.
- Catálogo editable de tareas y precios.
- Órdenes, ítems, estados, fecha prometida y control de concurrencia.
- Pagos parciales, métodos de pago y saldo calculado.
- Estadísticas mensuales.
- Auditoría persistente.
- Migraciones de base de datos y manejo uniforme de errores.
- Docker Compose para API + PostgreSQL.
- Exportación administrativa y respaldo diario automático con retención de 30 días.

## Arranque rápido con Docker

1. Copiar `.env.example` como `.env` y cambiar las claves.
2. Ejecutar `docker compose up --build`.
3. La API queda disponible en `http://localhost:8080`.
4. Verificar con `GET http://localhost:8080/api/health`.

Con `SPRING_PROFILES_ACTIVE=demo` se cargan automáticamente clientes, vehículos,
órdenes, tareas y pagos ficticios. Para una instalación real, usar
`SPRING_PROFILES_ACTIVE=prod`; los datos de ejemplo no se cargarán.

El usuario administrador inicial se define mediante `ADMIN_EMAIL` y
`ADMIN_PASSWORD`. El proyecto no publica contraseñas predeterminadas.

## Autenticación

`POST /api/auth/login`

```json
{ "email": "<ADMIN_EMAIL>", "password": "<ADMIN_PASSWORD>" }
```

Enviar el token en las demás solicitudes:

```text
Authorization: Bearer <token>
```

Las operaciones de escritura también requieren el encabezado `X-XSRF-TOKEN`,
tomado de la cookie `XSRF-TOKEN` emitida por la API. Los orígenes autorizados se
configuran explícitamente mediante `CORS_ORIGINS`.

## Endpoints

| Recurso | Endpoints principales |
|---|---|
| Autenticación | `POST /api/auth/login` |
| Clientes | `GET/POST /api/clients`, `GET/PUT/DELETE /api/clients/{id}` |
| Tareas | `GET/POST /api/tasks`, `PUT /api/tasks/{id}` |
| Órdenes | `GET/POST /api/orders`, `GET/PUT /api/orders/{id}` |
| Estado | `PATCH /api/orders/{id}/status?value=EN_PROCESO` |
| Pagos | `POST /api/orders/{id}/payments` |
| Usuarios | `GET/POST /api/users`, `PUT/DELETE /api/users/{id}` |
| Estadísticas | `GET /api/statistics?year=2026&month=8` |
| Auditoría | `GET /api/audit` |
| Exportación | `GET /api/backups` |

## Conexión del frontend

Configurar en React la URL base como `http://IP-DE-LA-PC-SERVIDOR:8080/api`.
El modo conectado utiliza la API para clientes, órdenes, pagos, estadísticas,
usuarios, auditoría y respaldos; el navegador conserva solamente la sesión.
`CORS_ORIGINS` debe incluir `http://IP-DE-LA-PC-SERVIDOR:3000` y cualquier otra
dirección real desde la que se abra React.

## Respaldos

El servicio `backup` crea un archivo PostgreSQL comprimido en `./backups` al
iniciar y luego cada 24 horas. Conserva los últimos 30 días. Además, un
administrador puede descargar una exportación JSON desde la interfaz.

Para restaurar una copia completa:

```bash
chmod +x scripts/restaurar-respaldo.sh
./scripts/restaurar-respaldo.sh backups/rectificadora-AAAAMMDD-HHMMSS.dump
```

La restauración reemplaza los datos actuales, por lo que debe hacerse con el
taller detenido y después de conservar una copia reciente.

## Producción

- Cambiar `DB_PASSWORD`, `JWT_SECRET` y la contraseña inicial.
- PostgreSQL no publica el puerto `5432` en Docker Compose.
- Usar HTTPS mediante un proxy inverso si se instala en una VM.
- Realizar copias periódicas con `pg_dump`.
