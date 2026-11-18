select 'reg_date_time_out_of_range' as subMetricName
     , count(1) as value
from {{ refCat(targetDataSetKeyName) }}
where reg_date_time >= timestamp '{{ intervalStartDateTime }}'
  and reg_date_time <  timestamp '{{ intervalEndDateTime }}'
  and (reg_date_time < timestamp '2000-01-01 00:00:00'
       or reg_date_time >= timestamp '2100-01-01 00:00:00')
