select date_trunc('dd', reg_date_time) as reg_date
     , client_name
     , sum(sum_amount) as sum_amount
     , sum(sum_amount * commission / 100) sum_commission
 from
 -- NB spark sql