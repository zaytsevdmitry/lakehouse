{%set targetDataSet=dataSets[targetDataSetKeyName]%}
INSERT INTO  {{ ref(targetDataSetKeyName) }} ({{ extractColumnsCS(targetDataSet) }})
SELECT {{ extractColumnsCS(targetDataSet)}}
FROM ({{ script }})