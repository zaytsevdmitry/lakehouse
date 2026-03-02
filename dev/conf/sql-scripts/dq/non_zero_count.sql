select count(1) cnt
from {{ refCat(targetDataSetKeyName) }}
where reg_date_time >= timestamp '{{ intervalStartDateTime }}'
  and reg_date_time <  timestamp '{{ intervalEndDateTime }}'