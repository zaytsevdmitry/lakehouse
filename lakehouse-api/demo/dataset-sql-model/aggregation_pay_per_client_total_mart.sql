select client_name
     , sum(amount)     as sum_amount
     , sum(commission) as sum_commission
from transactions_dds