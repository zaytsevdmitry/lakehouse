select 'sum_amount_mismatch' as subMetricName
     , count(1) as value
from (
  select m.reg_date
       , m.client_name
       , m.sum_amount
       , f.fact_amount
  from {{ refCat('aggregation_pay_per_client_daily_mart') }} m
  join (
    select cast(reg_date_time as date) as reg_date
         , client_name
         , sum(cast(amount as decimal(18, 2))) as fact_amount
    from {{ refCat('transaction_dds') }}
    where reg_date_time >= timestamp '{{ intervalStartDateTime }}'
      and reg_date_time <  timestamp '{{ intervalEndDateTime }}'
    group by cast(reg_date_time as date), client_name
  ) f
    on m.reg_date = f.reg_date
   and m.client_name = f.client_name
  where m.reg_date >= date '{{ intervalStartDate }}'
    and m.reg_date <  date '{{ intervalEndDate }}'
    and abs(m.sum_amount - f.fact_amount) > 0.01
) t
