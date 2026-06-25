select row_number() over() +  (EXTRACT(EPOCH FROM now()))::bigint as id
     , t.*
from (
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}' as reg_date_time , 1 client_id ,2 provider_id,1000 amount,2 commission union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 2,3,2000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 3,4,3000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 1,1,4000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 2,2,1000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 3,3,3000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 1,4,45000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 3,1,100,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 1,3,4000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 4,4,1000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 1,3,33000,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 3,2,1300,2 union all
select TIMESTAMP WITH TIME ZONE '{{ targetDateTime }}', 4,2,1400,2
) t