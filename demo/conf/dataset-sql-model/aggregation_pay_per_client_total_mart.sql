select client_name
     , sum(amount)     as sum_amount
     , sum(commission) as sum_commission
from  ${source(transaction_dds)}
group by client_name
