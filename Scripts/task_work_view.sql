select si.id
, si.config_schedule_key_name                     -- schedule name
, si.target_execution_date_time                   -- schedule target datetime
, ssai."name"                     scenario         -- part of chedule, is an reference for dataset, schedule and tasks
, sti."name"                      task             -- task
, ssai.conf_data_set_key_name                     -- dataset
, si.status                       schedule_status
, ssai.status                     scenario_status
, sti.status                      task_status
, stiel.last_heart_beat_date_time
, stiel.id                        lock_id
, stiel.service_id
from lakehouse_scheduler.schedule_task_instance sti
join lakehouse_scheduler.schedule_scenario_act_instance ssai on ssai.id =sti.schedule_scenario_act_instance_id
join lakehouse_scheduler.schedule_instance si on si.id = ssai.schedule_instance_id
left join lakehouse_scheduler.schedule_task_instance_execution_lock stiel on stiel.schedule_task_instance_id  = sti.id
--where target_execution_date_time = '2024-07-29T22:00Z' --'2021-01-01 04:00:00.000 +0300'
where sti.status not in ('NEW', 'SUCCESS');
order by sti.id -- config_schedule_key_name,si.target_execution_date_time,actname,conf_data_set_key_name ,sti.status
;