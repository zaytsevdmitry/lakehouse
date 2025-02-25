select date_trunc('dd', reg_date_time) as reg_date
     , client_name
     , sum(amount)     as sum_amount
     , sum(sum_amount * commission / 100) sum_commission
from transactions_dds t
where
   t.reg_date_time <= timestamp '${target-timestamp-tz}' and
   t.reg_date_time >  timestamp '${target-timestamp-tz}' + interval '1 day'