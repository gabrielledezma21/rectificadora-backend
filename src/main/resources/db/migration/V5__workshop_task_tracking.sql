alter table work_order_items add column task_status varchar(30) not null default 'DISPONIBLE';
alter table work_order_items add column assigned_employee_id uuid references users(id) on delete set null;
alter table work_order_items add column accepted_at timestamptz;
alter table work_order_items add column started_at timestamptz;
alter table work_order_items add column completed_at timestamptz;
alter table work_order_items add column technical_notes text;
alter table work_order_items add column version bigint not null default 0;

create table work_task_history (
  id uuid primary key,
  work_order_item_id uuid not null references work_order_items(id) on delete cascade,
  occurred_at timestamptz not null,
  action varchar(30) not null,
  actor varchar(180) not null,
  employee_id uuid,
  employee_name varchar(180),
  comment text
);

create index idx_work_items_status on work_order_items(task_status);
create index idx_work_items_employee on work_order_items(assigned_employee_id);
create unique index ux_work_items_employee_active on work_order_items(assigned_employee_id)
where assigned_employee_id is not null and task_status in ('ACEPTADA', 'EN_PROCESO');
create index idx_work_task_history_item on work_task_history(work_order_item_id, occurred_at);
