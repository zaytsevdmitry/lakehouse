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
where  t.reg_date_time >= '${target-timestamp-tz}'
and  t.reg_date_time <  '${target-timestamp-tz}' + interval '1 day'
-- NB spark sql
