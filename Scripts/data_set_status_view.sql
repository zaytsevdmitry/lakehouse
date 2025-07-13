select data_set_key_name
	, interval_start_date_time
	, interval_end_date_time
	, status
	, lock_source
	, interval_end_date_time - interval_start_date_time distance
	from lakehouse_state.data_set_state dss
where 1=1
--interval_start_date_time >= '2024-02-01 00:00:00.000 +0000'
--interval_end_date_time ='2024-02-01 00:00:00.000 +0000'
-- data_set_key_name ='transaction_processing' and
order by interval_start_date_time , data_set_key_name
;