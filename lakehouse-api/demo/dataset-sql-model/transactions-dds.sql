-- spark sql syntax

-- just for example

cache table client_processing_c as (select * from client_processing);

--^^ preparatory queries delimited by ;
---------------------
-- main query every time last
select c.name as client_name
      , t.*
from transactions_processing t
  join client_processing c
where
   t.reg_date_time <= timestamp '${execution-target-date-time}' and
   t.reg_date_time >  timestamp '${execution-target-date-time}' - inteval '1 ${sql-target-dt-interval}'