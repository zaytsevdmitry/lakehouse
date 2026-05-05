{%set targetDataSet=dataSets[targetDataSetKeyName]%}select case when exists (
	select * 
	from pg_catalog.pg_class pc 
	where relnamespace = (select oid from pg_catalog.pg_namespace where nspname = '{{ targetDataSet.databaseSchemaName }}')
 and relname='{{ targetDataSet.tableName}}'
) then 1 else 0 end  as result