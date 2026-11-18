select '{{ constraintName }}' as "subMetricName"
     , count(1) as "value"
     , '{{ constraintSeverity }}' as "severity"
from {{ refCat(targetDataSetKeyName) }}
where "{{ constraintName }}" is null
