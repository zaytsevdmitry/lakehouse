select '{{ constraintName }}' as subMetricName
     , count(1) as value
     , '{{ constraintSeverity }}' as severity
from (
  select {% for c in sourceColumns %}cast(src.`{{ c }}` as {{ sourceCastTypes[loop.index0] }}) as `{{ c }}`{% if not loop.last %}, {% endif %}{% endfor %}
  from {{ refCat(targetDataSetKeyName) }} src
) src_values
left anti join (
  select {% for c in referenceColumns %}refs.`{{ c }}` as `{{ c }}`{% if not loop.last %}, {% endif %}{% endfor %}
  from {{ refCat(referenceDataSetKeyName) }} refs
) ref_values
  on {% for c in sourceColumns %}src_values.`{{ c }}` = ref_values.`{{ referenceColumns[loop.index0] }}`{% if not loop.last %} and {% endif %}{% endfor %}
