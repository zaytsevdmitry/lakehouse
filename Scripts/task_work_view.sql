select si.id
, si.target_execution_date_time                   -- schedule target datetime
, si.config_schedule_key_name 
      || ' [' || case when ssai."name" = ssai.conf_data_set_key_name then ssai.conf_data_set_key_name
                 else ssai."name" || ' , ' || ssai.conf_data_set_key_name 
                 end
      || ' ][ ' || sti."name" || ' ]' task_fullname    
, sti.status ||' [' || sti.re_try_num || ']'  status         --schedule_status scenario_status task_status re_try_count
, stiel.last_heart_beat_date_time
, stiel.id                        lock_id
, stiel.service_id
, sti.service_id
, sti.id sti_id
, coalesce(stiel.last_heart_beat_date_time, sti.end_date_time) - sti.begin_date_time duration
, sti.causes
from lakehouse_scheduler.schedule_task_instance sti
join lakehouse_scheduler.schedule_scenario_act_instance ssai on ssai.id =sti.schedule_scenario_act_instance_id
join lakehouse_scheduler.schedule_instance si on si.id = ssai.schedule_instance_id
left join lakehouse_scheduler.schedule_task_instance_execution_lock stiel on stiel.schedule_task_instance_id  = sti.id
where ssai.status not in ('NEW', 'SUCCESS')
order by sti.id 
;