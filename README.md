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

## Arranque rápido con Docker

1. Copiar `.env.example` como `.env` y cambiar las claves.
2. Ejecutar `docker compose up --build`.
3. La API queda disponible en `http://localhost:8080`.
4. Verificar con `GET http://localhost:8080/api/health`.

Usuario inicial de desarrollo:

- Email: `admin@taller.com`
- Contraseña: `admin123`

Debe cambiarse al instalar el sistema en el taller.

## Autenticación

`POST /api/auth/login`

```json
{ "email": "admin@taller.com", "password": "admin123" }
```

Enviar el token en las demás solicitudes:

```text
Authorization: Bearer <token>
```

Las operaciones de escritura también requieren el encabezado `X-XSRF-TOKEN`,
tomado de la cookie `XSRF-TOKEN` emitida por la API. Esta protección se suma al
token JWT y a la lista explícita de orígenes permitidos.

## Endpoints

| Recurso | Endpoints principales |
|---|---|
| Autenticación | `POST /api/auth/login` |
| Clientes | `GET/POST /api/clients`, `GET/PUT /api/clients/{id}` |
| Tareas | `GET/POST /api/tasks`, `PUT /api/tasks/{id}` |
| Órdenes | `GET/POST /api/orders`, `GET/PUT /api/orders/{id}` |
| Estado | `PATCH /api/orders/{id}/status?value=EN_PROCESO` |
| Pagos | `POST /api/orders/{id}/payments` |
| Usuarios | `GET/POST /api/users`, `PUT /api/users/{id}` |
| Estadísticas | `GET /api/statistics?year=2026&month=8` |
| Auditoría | `GET /api/audit` |

## Conexión del frontend

Configurar en React la URL base como `http://IP-DE-LA-PC-SERVIDOR:8080/api`. El frontend debe reemplazar el acceso a `localStorage` por llamadas HTTP y conservar solamente el token de sesión. `CORS_ORIGINS` debe incluir las direcciones desde las cuales se abre React.

## Producción

- Cambiar `DB_PASSWORD`, `JWT_SECRET` y la contraseña inicial.
- No publicar PostgreSQL (`5432`) hacia Internet.
- Usar HTTPS mediante un proxy inverso si se instala en una VM.
- Realizar copias periódicas con `pg_dump`.
