select t.id id
     , reg_date_time
     , c.id client_id
     , c.client_name
     , provider_id
     , amount
     , commission
from ${source(transaction_processing)} t
join ${source(client_processing)} c
  on t.client_id = c.id
where  reg_date_time >= '${target-timestamp-tz}'
where  reg_date_time <  '${target-timestamp-tz}'
-- NB spark sql