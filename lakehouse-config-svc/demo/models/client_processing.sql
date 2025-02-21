select 1 id, 'one' name, to_timestamp('${target-time-stamp}','YYYY-MM-DD HH:MI:SS') as reg_date_time union all
select 2 id, 'two' name, to_timestamp('${target-time-stamp}','YYYY-MM-DD HH:MI:SS') as reg_date_time union all
select 3 id, 'three' name, to_timestamp('${target-time-stamp}','YYYY-MM-DD HH:MI:SS') as reg_date_time union all
select 4 id, 'four' name, to_timestamp('${target-time-stamp}','YYYY-MM-DD HH:MI:SS') as reg_date_time
