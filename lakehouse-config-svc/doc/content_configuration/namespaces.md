# Namespace
Used to bind [datasets](https://github.com/zaytsevdmitry/Data-engineering-theories/blob/main/DataManagement/AbstractEntities/DataSet.MD). Can be used to define a project, separate by source in a mesh approach, etc.
## Object fields
| Field       | Purpose                       |
|:------------|:------------------------------|
| keyName     | Unique identifier             | 
| description | Description for documentation | 


**Example**
```json
{
  "keyName": "DEMO",
  "description": "Demo space"
}
```


##  /v1_0/configs/nameSpaces
Returns all objects as a list
##  /v1_0/configs/nameSpaces/{keyName}
Manipulates a specific object by key