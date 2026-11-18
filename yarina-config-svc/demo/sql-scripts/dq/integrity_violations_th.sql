select *
from {{ qualityMetricsConfKeyName }}
where value > 0
