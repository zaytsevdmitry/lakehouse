# Скрипт


##  /v1_0/configs/scripts

##  /v1_0/configs/scripts/{key}



##  /v1_0/configs/compound/dataset/model/script/dataset/{keyName}
Endpoint предназначен для использования в моменте применения модели.
GET - Соберет все скрипты модели датасета с учетом заданного порядка и выдаст их в виде одного скрипта. Между скриптами будет

```shell
 curl -X GET http://localhost:8080/v1_0/configs/compound/dataset/model/script/dataset/transaction_dds
select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from {{ refCat('transaction_processing') }} t   -- refCat returns table name with catalog
join {{ refCat('client_processing') }} c
  on t.client_id = c.id
 where
   t.reg_date_time >= timestamp '{{ intervalStartDateTime }}' and
   t.reg_date_time < timestamp '{{ intervalEndDateTime }}'
``` 
