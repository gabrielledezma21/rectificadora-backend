insert into catalog_tasks (id, name, category, price, active)
select gen_random_uuid(), v.name, v.category, v.price, true
from (values
  ('Encamisar','BLOCK',15000), ('Rectificar cilindros','BLOCK',12000), ('Bruñido','BLOCK',8000),
  ('Plano block','BLOCK',6000), ('Ajuste de bancadas','BLOCK',10000), ('Bujes de levas','BLOCK',7000),
  ('Taponeado','BLOCK',5000), ('Embujado','BLOCK',4500), ('Altura de camisas','BLOCK',3500),
  ('Cigüeñal para rectificar','BLOCK',18000), ('Verificación de fisuras','BLOCK',5500),
  ('Lavado de block','BLOCK',3000), ('Pulido de conductos','BLOCK',6500),
  ('Válvulas','REPUESTO',8000), ('Guías de válvulas','REPUESTO',6500), ('Retenes','REPUESTO',4000),
  ('Junta de tapa','REPUESTO',5500), ('Resortes','REPUESTO',7000), ('Árbol de levas','REPUESTO',25000),
  ('Metales','REPUESTO',12000), ('Bomba de aceite','REPUESTO',9000),
  ('Empaquetadura completa','REPUESTO',8500), ('Pistones','REPUESTO',18000), ('Anillos','REPUESTO',10000),
  ('Cojinetes de bancada','REPUESTO',11000),
  ('Desarme','TAPA',4000), ('Lavado de tapa','TAPA',3500), ('Plano de tapa','TAPA',7000),
  ('Soldaduras','TAPA',9000), ('Asientos de válvulas','TAPA',8500), ('Cambio de guías','TAPA',7500),
  ('Rectificación de válvulas','TAPA',6000), ('Armado de tapa','TAPA',5000), ('Regulado','TAPA',4500),
  ('Prueba de presión','TAPA',3000), ('Verificación de fisuras','TAPA',5000),
  ('Rectificación de muñones','CIGUENAL',14000), ('Rectificación de bancadas','CIGUENAL',13000),
  ('Pulido de cigüeñal','CIGUENAL',8000), ('Balanceo dinámico','CIGUENAL',12000),
  ('Verificación de fisuras','CIGUENAL',6000), ('Medición de conicidad','CIGUENAL',3500),
  ('Medición de ovalización','CIGUENAL',3500), ('Enderezado','CIGUENAL',15000),
  ('Soldadura y mecanizado','CIGUENAL',16000), ('Tratamiento térmico','CIGUENAL',11000)
) as v(name, category, price)
where not exists (
  select 1 from catalog_tasks existing where existing.name = v.name and existing.category = v.category
);
