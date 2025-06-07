select date_trunc('dd', reg_date_time) as reg_date
     , client_name
     , sum(amount)     as sum_amount
     , sum(amount * commission / 100) sum_commission
from ${source(transaction_dds)} t
where
   t.reg_date_time <= timestamp '{{ targetDateTimeTZ }}' and
   t.reg_date_time >  timestamp '{{ targetDateTimeTZ }}' + interval '1 day'
 group by client_name
        , date_trunc('dd', reg_date_time)
