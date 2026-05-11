{%set targetDataSet=dataSets[targetDataSetKeyName]%}
INSERT INTO  {{ refCat(targetDataSetKeyName) }} ({{ extractColumnsCS(targetDataSet) }})
SELECT {{ extractColumnsCS(targetDataSet)}}
FROM ({{scripts[targetDataSet.scripts[0].key]}})