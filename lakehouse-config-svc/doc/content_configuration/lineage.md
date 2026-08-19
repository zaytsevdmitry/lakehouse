# Data lineage

##  /v1_0/configs/lineage/datasets/{keyName}
GET - Returns the dataset lineage: the directed graph of the dataset dependencies on other datasets
and back references (datasets that depend on it). The traversal is performed at all depth levels.

The returned object [DataSetLineageDTO.java](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/configs/dataset/DataSetLineageDTO.java):
  
Field     | Purpose                                  |
|:---------|:-----------------------------------------|
| vertices | List of dataset keys participating in the graph |
| edges    | List of graph edges (from -> to)          |

> Example, where {keyName} = transaction_dds

```bash
curl -X GET http://localhost:8080/v1_0/configs/lineage/datasets/transaction_dds |jq
```

**Example response**
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