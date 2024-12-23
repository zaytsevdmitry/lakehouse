alter table schedule_task_instance 
drop CONSTRAINT schedule_task_instance_status_check;

alter table schedule_task_instance add
CONSTRAINT schedule_task_instance_status_check 
CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'SUCCESS'::character varying, 'QUEUED'::character varying, 'RUNNING'::character varying, 'FAILED'::character varying])::text[])));
commit;

select * from schedule_task_instance sti 
where status  in ( 'FAILED','QUEUED', 'SUCCESS');
select * from schedule_instance sti 
where status  in ( 'FAILED','QUEUED', 'SUCCESS');

select * from schedule_scenario_act_instance  
where status  in ( 'FAILED','QUEUED', 'SUCCESS','RUNNING');

select  sti.*
	from schedule_instance_running sir
	join schedule_instance si                on si.id = sir.schedule_instance_id 
	join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id 
	join schedule_task_instance sti          on sti.schedule_scenario_act_instance_id = ssai.id and  sti.status = 'NEW'
	where not exists (
		select id 
			from schedule_task_instance_dependency stid 
			where stid.schedule_task_instance_id = sti.id 
				and not stid.satisfied
	)
;



select  stid.*
	from schedule_instance_running sir
	join schedule_instance si                on si.id = sir.schedule_instance_id 
	join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id 
	join schedule_task_instance sti          on sti.schedule_scenario_act_instance_id = ssai.id and  sti.status = 'SUCCESS'
	 join schedule_task_instance_dependency stid 
			on stid.depends_id = sti.id
		and stid.satisfied = false ;

			select * from  schedule_instance_running sir;

			select * from task_template tt ;
		
select distinct execution_module 
from schedule_task_instance sti 
order by 1
;

update schedule_task_instance 
set status = 'NEW'
where status != 'NEW'; 
select  ssai.*
    from schedule_instance_running sir
    join schedule_instance si                on si.id = sir.schedule_instance_id 
    join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id 
    join schedule_task_instance sti          on sti.schedule_scenario_act_instance_id = ssai.id and  sti.status = 'NEW'
    ;
select  ssaid.id , coalesce( ssaid.from_id, ssai.id) from_id, ssaid.to_id,ssai."name",ssai.data_set_name  ,ssai.schedule_instance_id,ssaid.satisfied  
    from schedule_instance_running sir
    join schedule_instance si                on si.id = sir.schedule_instance_id 
    join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id and ssai.status  = 'NEW'
    left join schedule_scenario_act_instance_dependency ssaid on ssaid.to_id = ssai.id
    
;
/*drop table schedule_instance_running cascade;
drop table schedule_instance_last cascade;
drop table schedule_task_instance_execution_lock cascade;
drop table schedule_task_instance_dependency cascade;
drop table schedule_task_instance cascade;
drop table schedule_scenario_act_instance_dependency  cascade;
drop table schedule_scenario_act_instance  cascade;
drop table schedule_instance  cascade;*/
select * from schedule_instance;


select  ssai.*
    from schedule_instance_running sir
    join schedule s                          on s.name  = sir.schedule_name and s.enabled 
    join schedule_instance si                on si.id = sir.schedule_instance_id  and si.status = 'RUNNING'
    join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id  and ssai.status = 'NEW'
        where not exists (
        select id 
            from schedule_scenario_act_instance_dependency ssaid 
            where ssaid.to_id = ssai.id 
                and not ssaid.satisfied
    )

;

select * from schedule s 





select  ssai.*
from schedule_instance_running sir
join schedule s                          on s.name  = sir.schedule_name and s.enabled 
join schedule_instance si                on si.id = sir.schedule_instance_id  and si.status = 'RUNNING'
join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id  and ssai.status = 'RUNNING'
where not exists (select 1
                    from schedule_task_instance sti 
                    where ssai.id = sti.schedule_scenario_act_instance_id 
                    and sti.status != 'SUCCESS');

                
                
                
--  schedule_instance ready to success                
select  sir.*
from schedule_instance_running sir
join schedule s                          on s.name  = sir.schedule_name and s.enabled 
join schedule_instance si                on si.id = sir.schedule_instance_id  and si.status = 'RUNNING'
where not exists (select 1
                    from schedule_scenario_act_instance ssai 
                    where  ssai.schedule_instance_id  = si.id 
                    and ssai.status != 'SUCCESS');                
                
                
                
select  *
from schedule_instance_running sir
join schedule s                          on s.name  = sir.schedule_name and s.enabled 
join schedule_instance si                on si.id = sir.schedule_instance_id -- and si.status = 'RUNNING'
join schedule_scenario_act_instance ssai on ssai.schedule_instance_id  = si.id -- and ssai.status = 'RUNNING'
join schedule_task_instance sti 
                    on ssai.id = sti.schedule_scenario_act_instance_id 
                    --and sti.status != 'SUCCESS')
;
select * from schedule_task_instance_execution_lock stiel 
