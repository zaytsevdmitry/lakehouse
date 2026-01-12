
select * from (
select 1 id, 'one' name,  TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}' as reg_date_time union all
select 2 id, 'two' name,  TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}' as reg_date_time union all
select 3 id, 'three' name,  TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}' as reg_date_time union all
select 4 id, 'four' name,  TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}' as reg_date_time
)c1
where  not exists(select * from {{ ref('client_processing') }}  c2 where c2.id = c1.id)