{%set catalog_name=dataSources[dataSets[targetDataSetKeyName].dataSourceKeyName].catalogKeyName  %}
CALL {{catalog_name}}.system.rewrite_data_files(
  table => '{{ refCat(targetDataSetKeyName) }}',
  options => map(
    'target-file-size-bytes', '134217728', -- 128 MB
    'max-concurrent-file-group-rewrites', '5'
  )
)