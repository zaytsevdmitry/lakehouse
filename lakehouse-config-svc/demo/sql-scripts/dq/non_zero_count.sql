select count(1) cnt
from {{ dataSet.fullTableName }}
where reg_date_time >= {{intervalStart}}
  and reg_date_time <  {{intervalEnd}}