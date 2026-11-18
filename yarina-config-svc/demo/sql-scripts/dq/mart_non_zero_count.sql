select '' as subMetricName
     , count(1) as value
from {{ refCat(targetDataSetKeyName) }}
where reg_date >= date '{{ intervalStartDate }}'
  and reg_date <  date '{{ intervalEndDate }}'
