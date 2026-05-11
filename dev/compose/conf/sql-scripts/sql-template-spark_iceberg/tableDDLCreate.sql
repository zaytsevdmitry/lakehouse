{%set currentDataSet=dataSets[currentDataSetKeyName]%}CREATE TABLE IF NOT EXISTS {{ refCat(currentDataSetKeyName) }} (
 {{ extractColumnsDDL(currentDataSet.columnSchema) }} 
) USING iceberg  {{targetDataSet.partitionStmt}}