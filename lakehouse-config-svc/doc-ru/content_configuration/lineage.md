# Линковка данных (data lineage)

##  /v1_0/configs/lineage/datasets/{keyName}
GET - Вернет линковку датасета: направленный граф зависимостей датасета от других датасетов
и обратных ссылок (датасетов, зависящих от него). Проход выполняется по всем уровням глубины.

Возвращаемый объект [DataSetLineageDTO.java](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/configs/dataset/DataSetLineageDTO.java):
  
Поле     | Назначение                                  |
|:---------|:--------------------------------------------|
| vertices | Список ключей датасетов участвующих в графе |
| edges    | Список ребер графа (from -> to)             |

> Пример, где {keyName} = transaction_dds

```bash
curl -X GET http://localhost:8080/v1_0/configs/lineage/datasets/transaction_dds |jq
```

**Пример ответа**
```json
{
  "vertices": [
    "transaction_dds",
    "client_processing",
    "transaction_processing"
  ],
  "edges": [
    {
      "from": "client_processing",
      "to": "transaction_dds"
    },
    {
      "from": "transaction_processing",
      "to": "transaction_dds"
    }
  ]
}
```