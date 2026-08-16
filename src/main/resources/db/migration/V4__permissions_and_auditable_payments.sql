create table user_permissions (
  user_id uuid not null references users(id) on delete cascade,
  permission varchar(60) not null,
  primary key (user_id, permission)
);

insert into user_permissions (user_id, permission)
select u.id, p.permission
from users u
cross join (values
  ('ORDENES_GESTIONAR'),
  ('CLIENTES_DATOS_BASICOS'),
  ('CLIENTES_VER_HISTORIAL'),
  ('CATALOGO_GESTIONAR'),
  ('PAGOS_REGISTRAR'),
  ('FINANZAS_VER'),
  ('ESTADISTICAS_VER'),
  ('AUDITORIA_VER'),
  ('USUARIOS_GESTIONAR'),
  ('RESPALDOS_GESTIONAR')
) as p(permission)
where u.role = 'ADMIN';

insert into user_permissions (user_id, permission)
select u.id, p.permission
from users u
cross join (values
  ('ORDENES_GESTIONAR'),
  ('CLIENTES_DATOS_BASICOS'),
  ('CATALOGO_GESTIONAR'),
  ('PAGOS_REGISTRAR')
) as p(permission)
where u.role = 'OPERADOR';

alter table payments add column cancelled_at timestamptz;
alter table payments add column cancelled_by varchar(180);
alter table payments add column cancellation_reason varchar(500);
