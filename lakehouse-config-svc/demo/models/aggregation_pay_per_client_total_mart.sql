select client_name
     , sum(sum_amount) as sum_amount
     , sum(sum_amount * commission / 100) sum_commission
 from ${source(transaction_dds).name}
 -- NB spark sql