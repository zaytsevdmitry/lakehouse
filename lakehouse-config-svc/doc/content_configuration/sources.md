# Data description and their sources

##  /v1_0/configs/compound/sources/dataset/{keyName}
GET - collects a slice of metadata related to the dataset, including model and construct dependencies
> Example, where {keyName} = transaction_dds

> |jq to output the structure in a readable form

```bash
curl -X GET http://localhost:8080/v1_0/configs/compound/sources/dataset/transaction_dds |jq 
```
The returned object [SourceConfDTO.java](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/task/SourceConfDTO.java)