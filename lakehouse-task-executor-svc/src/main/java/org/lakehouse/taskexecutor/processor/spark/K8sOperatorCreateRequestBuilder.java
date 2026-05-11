package org.lakehouse.taskexecutor.processor.spark;

import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.jinja.java.JinJavaUtils;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @apiNote
 * https://github.com/kubeflow/spark-operator/blob/master/docs/api-docs.md
 * example
 * {
 *   "apiVersion": "sparkoperator.k8s.io/v1beta2",
 *   "kind": "SparkApplication",
 *   "metadata": {
 *     "name": "spark-pi-curl-direct"
 *   },
 *   "spec": {
 *     "type": "Scala",
 *     "mode": "cluster",
 *     "image": "apache/spark:3.5.0",
 *     "mainClass": "org.apache.spark.examples.SparkPi",
 *     "mainApplicationFile": "local:///opt/spark/examples/jars/spark-examples_2.12-3.5.0.jar",
 *     "sparkVersion": "3.5.0",
 *     "sparkConf": {
 *       "spark.executor.extraJavaOptions": "-Duser.timezone=UTC",
 *       "spark.sql.shuffle.partitions": "100",
 *       "spark.hadoop.fs.s3a.impl": "org.apache.hadoop.fs.s3a.S3AFileSystem",
 *       "spark.kubernetes.allocation.batch.size": "10"
 *     },
 *     "driver": {
 *       "cores": 1,
 *       "memory": "512m",
 *       "serviceAccount": "spark"
 *     },
 *     "executor": {
 *       "cores": 1,
 *       "instances": 1,
 *       "memory": "512m"
 *     }
 *   }
 * }
 *
 * */
public class K8sOperatorCreateRequestBuilder {

    public String getSpec(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {

        Map<String,String> mergedFlatConf = Coalesce.applyMergeNonNullValuesMap(
                filterConf(sourceConfDTO.getTargetDataSource().getService().getProperties()),
                filterConf(scheduledTaskDTO.getTaskProcessorArgs()));

        String json = ConfigFactory.parseMap(mergedFlatConf)
                .root()
                .render(ConfigRenderOptions.concise().setFormatted(true));

        System.out.println(json);

        return json;
    }

    private Map<String,String> filterConf(Map<String,String> conf){
        return conf
                .entrySet()
                .stream()
                .filter(e->e.getKey().startsWith(TaskProcessorArgKey.K8S_SPARK_OPERATOR.concat("spec.")))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
}
