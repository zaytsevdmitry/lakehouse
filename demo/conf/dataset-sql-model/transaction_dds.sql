select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from ${source(transaction_processing)} t
join ${source(client_processing)} c
  on t.client_id = c.id
where  t.reg_date_time >= '{{ targetDateTimeTZ }}'
and  t.reg_date_time <  '{{ targetDateTimeTZ }}' + interval '1 day'
-- NB spark sql
