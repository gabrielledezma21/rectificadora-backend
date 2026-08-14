create table users (
  id uuid primary key, name varchar(150) not null, email varchar(180) not null unique,
  password_hash varchar(255) not null, role varchar(30) not null, active boolean not null default true,
  created_at timestamptz not null
);
create table clients (
  id uuid primary key, name varchar(180) not null, phone varchar(60), email varchar(180), address varchar(255), created_at timestamptz not null
);
create table vehicles (
  id uuid primary key, client_id uuid not null references clients(id) on delete cascade,
  description varchar(180) not null, engine_number varchar(120), license_plate varchar(30)
);
create table catalog_tasks (
  id uuid primary key, name varchar(180) not null, category varchar(30) not null,
  price numeric(14,2) not null check(price >= 0), active boolean not null default true
);
create table work_orders (
  id uuid primary key, order_number varchar(40) not null unique, created_at timestamptz not null,
  promised_date date, client_id uuid not null references clients(id), vehicle_id uuid references vehicles(id),
  status varchar(30) not null, cylinders integer, final_measure varchar(80), reception_description text,
  notes text, total numeric(14,2) not null check(total >= 0), paid numeric(14,2) not null check(paid >= 0), version bigint not null default 0
);
create table work_order_items (
  id uuid primary key, work_order_id uuid not null references work_orders(id) on delete cascade,
  catalog_task_id uuid references catalog_tasks(id), description varchar(255) not null, category varchar(30) not null,
  unit_price numeric(14,2) not null check(unit_price >= 0), quantity integer not null check(quantity > 0)
);
create table payments (
  id uuid primary key, work_order_id uuid not null references work_orders(id) on delete cascade,
  paid_at timestamptz not null, amount numeric(14,2) not null check(amount > 0), method varchar(30) not null,
  details varchar(500), registered_by varchar(180) not null
);
create table audit_logs (
  id uuid primary key, occurred_at timestamptz not null, username varchar(180) not null,
  action varchar(80) not null, entity_type varchar(80) not null, entity_id varchar(80), detail text
);
create index idx_orders_created_at on work_orders(created_at desc);
create index idx_orders_status on work_orders(status);
create index idx_orders_client on work_orders(client_id);
create index idx_payments_order on payments(work_order_id);
create index idx_audit_occurred_at on audit_logs(occurred_at desc);
