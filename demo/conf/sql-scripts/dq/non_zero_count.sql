select '' subMetricName
     , count(1) value
from {{ refCat(targetDataSetKeyName) }}
where reg_date_time >= timestamp '{{ intervalStartDateTime }}'
  and reg_date_time <  timestamp '{{ intervalEndDateTime }}'