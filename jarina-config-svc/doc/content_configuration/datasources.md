# Data sources
Used to define data sources.
## Object fields
| Field                                | Purpose                                                                                                                                                        |
|:-------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| keyName                              | Unique identifier                                                                                                                                              | 
| [service](#nested-service-object)    | Describes connection parameters for creating a session or sending commands                                                                                     |
| description                          | Description for documentation                                                                                                                                  | 
| databaseProtocol                     | Connection protocol to the source                                                                                                                              |
| dataSourceType                       | Source type. <br/>**database** - for databases,<br/> **[iceberg](https://iceberg.apache.org/)** - for tables in iceberg file format                             | 

## Nested service object
| Field       | Purpose                                                 |
|:------------|:--------------------------------------------------------|
| host        | Network address of the source node                      | 
| port        | Network port of the source                              | 
| urn         | Connection point. A possible database name, or other    |
| properties  | Other connection parameters as a key-value map          |



**Example**
```json
{
  "keyName": "processingdb",
  "service":
  {
    "host": "172.20.193.10",
    "port": "5432",
    "urn": "postgresDB",
    "properties": {
      "password": "postgresPW",
      "user": "postgresUser",
      "fetchSize": "10000"
    }
  },
  "description": "Remote datastore processingdb",
  "dataSourceType": "database",
  "databaseProtocol": "postgresql"
}


```

##  /v1_0/configs/datasources
GET - Returns a list of sources

##  /v1_0/configs/datasources/{keyName}                                                            

Manipulates a specific object by key