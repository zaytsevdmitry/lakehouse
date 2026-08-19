# Script references
A script is the dynamic part of the service that allows adapting and customizing functionality. Most often used in [dataset](datasets.md) models,
[sql templates](sqlTemplate.md), test sets of [data quality metrics](qualityMetric.md) in the form of a special object - a reference.
Using references instead of direct script text allows:
* script reuse
* composite scripts. 
* large scripts without the need to escape text control characters.


## Object fields
| Field    | Purpose                                                                                                                                       |
|:---------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| key      | Unique identifier, name or file path. When using a file path, "/" must be replaced with another character, e.g. "."                            | 
| order    | Tells the executing code in what order the script should be placed, e.g. when using a [script collection](#script-collection)                   |

> Note: the collection sorting is performed by the configuration service API, but the method and the final way of applying remains with the executing engine. (processor or processorBody)

[ScriptReferenceDTO.java](../../../lakehouse-common/src/main/java/org/lakehouse/client/api/dto/configs/ScriptReferenceDTO.java)


# Script collection
Can be used to form a composite script where each script is used as a fragment. A fragment can be reused for many composite scripts