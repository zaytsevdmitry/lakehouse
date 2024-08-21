select date_trunc('dd', reg_date_time) as reg_date
     , client_name
     , sum(amount)     as sum_amount
     , sum(commission) as sum_commission
from transactions_dds t
where
   t.reg_date_time <= timestamp '${execution-target-date-time}' and
   t.reg_date_time >  timestamp '${execution-target-date-time}' - inteval '1 ${sql-target-dt-interval}'