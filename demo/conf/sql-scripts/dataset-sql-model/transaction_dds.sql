select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from {{ refCat('transaction_processing') }} t   -- refCat returns table name with catalog
join {{ refCat('client_processing') }} c
  on t.client_id = c.id
 where
   t.reg_date_time >= timestamp '{{ intervalStartDateTime }}' and
   t.reg_date_time < timestamp '{{ intervalEndDateTime }}'
-- NB spark sql
