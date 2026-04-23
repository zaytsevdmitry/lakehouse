# Описание данных и их источников

##  /v1_0/configs/compound/sources/dataset/{keyName}
GET - соберет срез метаданных связанных с датасетом, в том числе зависимости модели и конструктивов
> Пример, где {keyName} = transaction_dds

> |jq для вывода структуры в удобно читаемом виде

```bash
curl -X GET http://localhost:8080/v1_0/configs/compound/sources/dataset/transaction_dds |jq 
```
Возвращаемый объект [SourceConfDTO.java](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/task/SourceConfDTO.java)
