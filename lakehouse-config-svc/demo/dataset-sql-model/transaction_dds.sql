select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from {{ source('DEMO', 'transaction_processing') }} t
join {{ source('DEMO', 'client_processing') }} c
  on t.client_id = c.id
 where
   t.reg_date_time >= timestamp '{{ targetIntervalStartTZ }}' and
   t.reg_date_time < timestamp '{{ targetIntervalEndTZ }}'
-- NB spark sql
