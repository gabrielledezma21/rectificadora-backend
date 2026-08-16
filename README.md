# Rectificadora · Backend

API REST del sistema de gestión para una rectificadora de motores. Centraliza órdenes de trabajo, tareas del taller, clientes, vehículos, pagos, usuarios, auditoría y respaldos.

El backend está pensado para funcionar como servidor del taller dentro de una red local o en una VM, con PostgreSQL como almacenamiento persistente y un frontend separado que consume la API.

## Funcionalidades principales

- Autenticación segura con JWT, BCrypt y protección CSRF para operaciones de escritura.
- Roles diferenciados para dueño/administrador, personal administrativo y empleados del taller.
- Permisos granulares por módulo.
- Clientes con múltiples vehículos e historial de órdenes.
- Catálogo editable de trabajos, categorías y precios.
- Órdenes de trabajo con tareas, fecha prometida, importes, pagos y saldo pendiente.
- Flujo controlado de estados de una orden.
- Tablero operativo del taller con asignación y seguimiento de tareas.
- Historial persistente de acciones realizadas sobre cada tarea.
- Historial personal de tareas para cada empleado.
- Regla de una única tarea activa por empleado.
- Revisión administrativa obligatoria antes de finalizar una orden.
- Pagos parciales y distintos medios de pago.
- Estadísticas mensuales y auditoría de operaciones.
- Datos de demostración opcionales.
- Migraciones automáticas con Flyway.
- Respaldo diario de PostgreSQL con retención de 30 días.
- Docker Compose para levantar API, base de datos y servicio de respaldo.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.16 |
| API | Spring Web |
| Seguridad | Spring Security + JWT |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 17 |
| Migraciones | Flyway |
| Validación | Jakarta Validation |
| Build y pruebas | Maven |
| Contenedores | Docker + Docker Compose |

## Roles y permisos

El sistema contempla tres roles principales:

- `ADMIN`: dueño o administrador. Tiene control completo sobre el sistema, usuarios, asignaciones, auditoría, estadísticas y respaldos.
- `OPERADOR`: personal administrativo. Sus accesos dependen de los permisos asignados.
- `EMPLEADO_TALLER`: empleado que trabaja sobre las tareas de las órdenes desde el tablero del taller.

Los permisos disponibles son:

```text
ORDENES_GESTIONAR
CLIENTES_DATOS_BASICOS
CLIENTES_VER_HISTORIAL
CATALOGO_GESTIONAR
PAGOS_REGISTRAR
FINANZAS_VER
ESTADISTICAS_VER
AUDITORIA_VER
USUARIOS_GESTIONAR
RESPALDOS_GESTIONAR
TAREAS_TALLER
```

## Flujo de una orden

El cambio de estado se realiza de forma controlada:

```text
Recepción → En proceso → Finalizada → Entregada
```

También es posible cancelar una orden antes de la entrega.

Una orden **no se finaliza automáticamente** cuando termina la última tarea. Primero queda lista para revisión del dueño o del personal administrativo autorizado. El backend impide marcarla como `FINALIZADO` mientras exista alguna tarea sin finalizar.

Desde `FINALIZADO` se puede volver a `EN_PROCESO` cuando se detecta una corrección necesaria. `ENTREGADO` y `CANCELADO` son estados terminales.

## Flujo de las tareas del taller

Estados de tarea disponibles:

```text
DISPONIBLE
ASIGNADA
ACEPTADA
EN_PROCESO
PENDIENTE
FINALIZADA
```

El flujo habitual es:

```text
Disponible → Aceptada → En proceso → Finalizada
```

Una tarea también puede ser asignada previamente por el administrador. Si un empleado la deja pendiente, queda nuevamente disponible para ser retomada. Las tareas finalizadas pueden ser reabiertas por el administrador.

El sistema conserva un historial con acciones como aceptación, inicio, pausa, reanudación, finalización, asignación, reasignación y reapertura.

Cada empleado puede tener **una sola tarea activa al mismo tiempo**.

## Arranque rápido con Docker

### 1. Crear el archivo de entorno

Copiar `.env.example` como `.env` y reemplazar las claves de ejemplo:

```env
DB_PASSWORD=una-clave-segura
JWT_SECRET=una-clave-aleatoria-de-al-menos-32-caracteres
ADMIN_EMAIL=administrador@taller.local
ADMIN_PASSWORD=reemplazar-por-una-clave-segura-de-12-o-mas-caracteres
SPRING_PROFILES_ACTIVE=demo
CORS_ORIGINS=http://localhost:3000,http://IP-DE-LA-PC-SERVIDOR:3000
```

### 2. Levantar los servicios

```bash
docker compose up -d --build
```

Se inician tres servicios:

- `postgres`: PostgreSQL con volumen persistente.
- `api`: backend Spring Boot en el puerto `8080`.
- `backup`: respaldo automático diario de PostgreSQL.

### 3. Comprobar el backend

```text
GET http://localhost:8080/api/health
```

Respuesta esperada:

```json
{
  "status": "UP",
  "service": "gestion-ordenes-backend"
}
```

> No utilizar `docker compose down -v` salvo que realmente se quiera eliminar la base de datos persistente. La opción `-v` borra el volumen de PostgreSQL.

## Variables de entorno

| Variable | Uso |
|---|---|
| `DB_PASSWORD` | Contraseña del usuario PostgreSQL |
| `JWT_SECRET` | Clave usada para firmar los JWT |
| `ADMIN_EMAIL` | Correo del administrador inicial |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial |
| `SPRING_PROFILES_ACTIVE` | Perfil de ejecución (`demo` o `prod`) |
| `CORS_ORIGINS` | Orígenes permitidos para el frontend |

### Perfiles

`demo` carga automáticamente datos ficticios para probar el sistema:

```env
SPRING_PROFILES_ACTIVE=demo
```

Para una instalación real:

```env
SPRING_PROFILES_ACTIVE=prod
```

El proyecto no publica contraseñas predeterminadas. El administrador inicial se crea a partir de `ADMIN_EMAIL` y `ADMIN_PASSWORD`.

## Autenticación y seguridad

Inicio de sesión:

```text
POST /api/auth/login
```

Ejemplo:

```json
{
  "email": "<ADMIN_EMAIL>",
  "password": "<ADMIN_PASSWORD>"
}
```

Las solicitudes autenticadas utilizan:

```text
Authorization: Bearer <token>
```

Las operaciones que modifican información también requieren el encabezado `X-XSRF-TOKEN`, obtenido mediante el mecanismo CSRF de la API.

Los orígenes habilitados se definen explícitamente mediante `CORS_ORIGINS`.

## Endpoints principales

| Módulo | Rutas principales |
|---|---|
| Salud | `GET /api/health` |
| Autenticación | `POST /api/auth/login`, CSRF bajo `/api/auth` |
| Clientes | `/api/clients` |
| Catálogo de tareas | `/api/tasks` |
| Órdenes | `/api/orders` |
| Estado de órdenes | `PATCH /api/orders/{id}/status?value=...` |
| Pagos | `POST /api/orders/{id}/payments` |
| Tablero del taller | `GET /api/workshop/orders` |
| Aceptar tarea | `PATCH /api/workshop/tasks/{id}/accept` |
| Iniciar tarea | `PATCH /api/workshop/tasks/{id}/start` |
| Dejar pendiente | `PATCH /api/workshop/tasks/{id}/pending` |
| Finalizar tarea | `PATCH /api/workshop/tasks/{id}/complete` |
| Asignar tarea | `PATCH /api/workshop/tasks/{id}/assignment` |
| Reabrir tarea | `PATCH /api/workshop/tasks/{id}/reopen` |
| Historial del empleado | `GET /api/workshop/mi-historial` |
| Usuarios | `/api/users` |
| Estadísticas | `GET /api/statistics?year=2026&month=8` |
| Auditoría | `GET /api/audit` |
| Respaldos / exportación | `/api/backups` |

## Conexión con el frontend

El frontend debe apuntar a:

```text
http://IP-DE-LA-PC-SERVIDOR:8080/api
```

Ejemplo en la computadora que actúa como servidor:

```env
CORS_ORIGINS=http://localhost:3000,http://192.168.1.50:3000
```

La dirección configurada en `CORS_ORIGINS` debe coincidir con el origen real desde el que se abre el frontend.

## Persistencia y respaldos

PostgreSQL utiliza el volumen Docker `postgres_data`, por lo que los datos sobreviven a reconstrucciones normales de los contenedores.

El servicio `backup` ejecuta `pg_dump` al iniciar y luego cada 24 horas. Los archivos se guardan en:

```text
./backups
```

Los respaldos con más de 30 días se eliminan automáticamente.

Para restaurar una copia completa:

```bash
chmod +x scripts/restaurar-respaldo.sh
./scripts/restaurar-respaldo.sh backups/rectificadora-AAAAMMDD-HHMMSS.dump
```

La restauración reemplaza los datos actuales. Debe hacerse con el taller detenido y conservando previamente una copia reciente.

## Desarrollo y pruebas

Requisitos locales:

- Java 21.
- Maven.
- PostgreSQL, salvo que las pruebas utilicen la configuración incluida para test.

Ejecutar validación completa:

```bash
mvn --batch-mode verify
```

GitHub Actions ejecuta `mvn --batch-mode verify` automáticamente en cada pull request y en cada actualización de `main`.

## Producción

Antes de usar el sistema con información real:

- usar `SPRING_PROFILES_ACTIVE=prod`;
- reemplazar `DB_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD` por valores fuertes;
- limitar correctamente `CORS_ORIGINS`;
- no publicar directamente el puerto de PostgreSQL;
- utilizar HTTPS mediante un proxy inverso si el servidor queda expuesto fuera de la red local;
- conservar copias externas periódicas además del respaldo local automático;
- verificar regularmente que los archivos de respaldo puedan restaurarse.
