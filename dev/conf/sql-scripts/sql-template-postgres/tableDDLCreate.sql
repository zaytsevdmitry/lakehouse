{%set targetDataSet=dataSets[targetDataSetKeyName]%}CREATE TABLE {{ ref(targetDataSetKeyName) }} (
 {{ extractColumnsDDL(targetDataSet.columnSchema) }} 
) {{targetDataSet.partitionStmt}}
