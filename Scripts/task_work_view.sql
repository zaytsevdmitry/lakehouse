select si.id
, si.config_schedule_key_name                     -- schedule name
, si.target_execution_date_time                   -- schedule target datetime
, ssai."name"                     scenario         -- part of chedule, is an reference for dataset, schedule and tasks
, sti."name"                      task             -- task
, ssai.conf_data_set_key_name                     -- dataset
, si.status                       schedule_status
, ssai.status                     scenario_status
, sti.status                      task_status
, sti.re_try_count
, sti.causes
, stiel.last_heart_beat_date_time
, stiel.id                        lock_id
, stiel.service_id
, sti.service_id
, sti.id sti_id
from lakehouse_scheduler.schedule_task_instance sti
join lakehouse_scheduler.schedule_scenario_act_instance ssai on ssai.id =sti.schedule_scenario_act_instance_id
join lakehouse_scheduler.schedule_instance si on si.id = ssai.schedule_instance_id
left join lakehouse_scheduler.schedule_task_instance_execution_lock stiel on stiel.schedule_task_instance_id  = sti.id
;