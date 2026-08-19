# Script


##  /v1_0/configs/scripts

##  /v1_0/configs/scripts/{key}



##  /v1_0/configs/compound/dataset/model/script/dataset/{keyName}
This endpoint is intended for use at the moment of applying the model.
GET - Collects all scripts of the dataset model in the given order and returns them as a single script. Between the scripts there will be

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