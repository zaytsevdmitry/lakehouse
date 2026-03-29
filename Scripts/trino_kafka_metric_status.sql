
with tab as(
select metric_id
	 , catalog_name
	 , data_base_schema_name
	 , table_name
	 , target_date_time
	 ,interval_start_date_time
	 ,interval_end_date_time
	 , metric_key_name
	 , rank() over( partition by metric_id,catalog_name,data_base_schema_name,table_name,target_date_time,interval_start_date_time,interval_end_date_time,metric_key_name
           order by _timestamp desc) rn
     , last_value(status) over(partition by metric_id,catalog_name,data_base_schema_name,table_name,target_date_time,interval_start_date_time,interval_end_date_time,metric_key_name
           order by _timestamp ) status
 from kafka.default.metric_status t
)
select metric_id
     , catalog_name||'.'||data_base_schema_name||'.'||table_name as table_name
     , target_date_time
     , interval_start_date_time
     , interval_end_date_time
     , metric_key_name
     , status
     , interval_end_date_time - interval_start_date_time duration
from tab where rn =1
;
