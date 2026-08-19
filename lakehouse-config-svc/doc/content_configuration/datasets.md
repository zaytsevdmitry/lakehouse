# Dataset
Abstraction for defining a data object. 

## Object fields
| Field                                              | Purpose                                                                                                                   |
|:---------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------|
| keyName                                            | Unique identifier                                                                                                         | 
| nameSpaceKeyName                                   | Belonging to a [namespace](namespaces.md)                                                                                 |
| dataSourceKeyName                                  | Points to the [data source](datasources.md) where the dataset is located                                                  |
| databaseSchemaName                                 | Name of the schema where the table is located                                                                             |
| tableName                                          | Table name                                                                                                                |
| description                                        | Description for documentation                                                                                             | 
| [scripts](scriptsReference.md#script-collection)   | List of script references used as fragments for building the dataset model                                                 | 
| [sources](#sources)                                | Map - a reference to another dataset. The key is the name of the dataset it depends on                                     |
| [columnSchema](#columnschema)                      | Nested list of column descriptions                                                                                        |
| [constraints](#constraints)                        | Nested list/map of table constructs, where the key is the construct name and the value is the settings description         |
| properties                                         | Nested key-value map of additional parameters                                                                             |
| partitionStmt                                      | Expression that defines table partitioning                                                                                |

## sources
Nested list of dependent datasets

| Field       | Purpose                                                                   |
|:------------|:--------------------------------------------------------------------------|
| properties  | Set of properties of the dependency dataset that override its own         | 

## columnSchema
Nested list of column descriptions

| Field        | Purpose                                                                                                                          |
|:-------------|:---------------------------------------------------------------------------------------------------------------------------------|
| name         | Column name in the storage table                                                                                                 | 
| description  | Description for documentation                                                                                                    |
| dataType     | Data type                                                                                                                        |
| nullable     | Whether empty value is allowed. true/false                                                                                       |
| order        | Ordinal number of the column position. Optional. Applicability depends on the executing engine and storage capabilities          |

## constraints

Nested list of table constructs

**Example**

```json
{
  "keyName": "transaction_dds",
  "nameSpaceKeyName": "DEMO",
  "dataSourceKeyName": "lakehousestorage",
  "databaseSchemaName": "default",
  "tableName": "transaction_dds",
  "description": "Details",
  "scripts": [
    {
      "key": "dataset-sql-model.transaction_dds.sql"
    }
  ],
  "sources": {
    "client_processing": {
      "properties": {
        "fetchSize": "10000"
      }
    },
    "transaction_processing": {
      "properties": {
        "fetchSize": "10000"
      }
    }
  },
  "columnSchema": [
    {
      "name": "client_name",
      "description": "Client name",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "id",
      "description": "tx id",
      "dataType": "bigint",
      "nullable": false,
      "order": 0
    },
    {
      "name": "reg_date_time",
      "description": "Transaction registration",
      "dataType": "timestamp",
      "nullable": false
    },
    {
      "name": "client_id",
      "description": "from client",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "provider_id",
      "description": "To provider",
      "dataType": "string",
      "nullable": false
    },
    {
      "name": "amount",
      "description": "Amount paid by the client",
      "dataType": "decimal",
      "nullable": false
    },
    {
      "name": "commission",
      "description": "Commission due to us",
      "dataType": "string",
      "nullable": false
    }
  ],
  "constraints": {
    "transaction_dds_pk": {
      "type": "primary",
      "columns": "id",
      "constraintLevelCheck": "dataQuality"
    }
  },
  "properties": {
   
  }
  
}
```

##  /v1_0/configs/datasets
GET - Returns the full list of dataset configurations
POST - Accepts a dataset in the body for saving
```shell
curl -X GET http://localhost:8080/v1_0/configs/datasets  |jq
```
GET - Returns the dataset in the body
DELETE - Deletes the specified dataset (when there are no dependencies)
##  /v1_0/configs/datasets/{keyName}
```shell
curl -X GET http://localhost:8080/v1_0/configs/datasets/transaction_dds  |jq
```
##  /v1_0/configs/lineage/datasets/{keyName}
Returns the dataset [lineage](lineage.md): the graph of its dependencies on other datasets and back references