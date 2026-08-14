create sequence work_order_number_seq start with 1 increment by 1;

select setval(
  'work_order_number_seq',
  greatest((select count(*) + 1 from work_orders), 1),
  false
);
