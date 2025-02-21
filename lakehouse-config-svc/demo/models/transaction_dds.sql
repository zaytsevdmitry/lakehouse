select t.id id
     , reg_date_time
     , c.id client_id
     , c.client_name
     , provider_id
     , amount
     , commission
from ${source(transaction_processing).name} t
join ${source(client_processing).name} c
  on t.client_id = c.id
where  reg_date_time >= '${timestamp-lower-bound}'
where  reg_date_time <  '${timestamp-upper-bound}'
-- NB spark sql