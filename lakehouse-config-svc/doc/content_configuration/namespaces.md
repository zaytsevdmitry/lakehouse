# Пространство имен (namespace)
Служит для привязки [датасетов](https://github.com/zaytsevdmitry/Data-engineering-theories/blob/main/DataManagement/AbstractEntities/DataSet.MD). Может использоваться в качестве определения проекта, разделения по источникам в mesh подходе и тд
## Поля объекта
| Поле        | Назначение                    |
|:------------|:------------------------------|
| keyName     | Уникальный идентификатов      | 
| description | Описание для документирования | 


**Пример**
```json
{
  "keyName": "DEMO",
  "description": "Demo space"
}
```


##  /v1_0/configs/nameSpaces
Предоставит все объекты списком
##  /v1_0/configs/nameSpaces/{keyName}
Манипуляция конкретным объектом по ключу