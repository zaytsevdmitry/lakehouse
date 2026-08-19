# Data quality metric

## Object fields
| Field                     | Purpose                                                                 |
|:--------------------------|:-----------------------------------------------------------------------|
| keyName                   | Unique metric identifier                                               |
| dataSetKeyName            | Reference to the [dataset](datasets.md) the metric is bound to          |
| description               | Description for documentation                                          |
| enabled                   | Enabled - true                                                         |
| save                      | Whether to save the check result                                       |
| dqThresholdViolationLevel | Quality threshold violation level                                      |
| sources                   | References to the [source datasets](datasets.md#sources) for checking  |
| testSets                  | Test data sets                                                        |
| thresholds                | Metric threshold values                                                |

##  /v1_0/configs/quality/metrics
List of all metrics
##  /v1_0/configs/quality/metrics/dataset/{keyName}
Returns the metric configuration by dataset key
##  /v1_0/configs/quality/metrics/{keyName}  
Returns the metric configuration by metric key