package org.lakehouse.taskexecutor.test;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.K8sSparkOperatorClientUtils;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnableKubernetesMockClient(crud = true)
class K8sSparkOperatorClientUtilsTest {

    KubernetesClient client;
    K8sSparkOperatorClientUtils utils;

    private static final ResourceDefinitionContext CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup("sparkoperator.k8s.io")
            .withVersion("v1beta2")
            .withKind("SparkApplication")
            .withNamespaced(true)
            .build();

    @BeforeEach
    void setUp() {
        utils = new K8sSparkOperatorClientUtils();

        // Регистрация CRD в моке (чертеж)
        client.apiextensions().v1().customResourceDefinitions().load(new ByteArrayInputStream("""
                {
                  "apiVersion": "apiextensions.k8s.io/v1",
                  "kind": "CustomResourceDefinition",
                  "metadata": { "name": "sparkapplications.sparkoperator.k8s.io" },
                  "spec": {
                    "group": "sparkoperator.k8s.io",
                    "names": { "kind": "SparkApplication", "plural": "sparkapplications" },
                    "scope": "Namespaced",
                    "versions": [{ 
                        "name": "v1beta2", "served": true, "storage": true,
                        "schema": { "openAPIV3Schema": { "type": "object", "x-kubernetes-preserve-unknown-fields": true } } 
                    }]
                  }
                }""".getBytes())).create();
    }

    @Test
    @DisplayName("Should finish even if status is FAILED")
    void shouldHandleFailedStatus() {
        String jobName = "failed-job";
        String jsonSpec = String.format("""
                {
                  "apiVersion": "sparkoperator.k8s.io/v1beta2",
                  "kind": "SparkApplication",
                  "metadata": { "name": "%s", "namespace": "default" },
                  "spec": { "type": "Java" }
                }""", jobName);

        simulateOperator(jobName, "FAILED", 300);

        assertDoesNotThrow(() -> utils.submit(client, jsonSpec));
    }

    private void simulateOperator(String name, String targetState, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                var resOp = client.genericKubernetesResources(CONTEXT)
                        .inNamespace("default")
                        .withName(name);

                GenericKubernetesResource current = resOp.get();
                if (current != null) {
                    current.setAdditionalProperties(Map.of(
                            "status", Map.of("applicationState", Map.of("state", targetState))
                    ));
                    resOp.patchStatus(current);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    @Test
    void shouldSubmit() throws TaskFailedException {
        // 1. Создаем "видимость" установленного оператора (точнее его CRD)
        client.apiextensions().v1().customResourceDefinitions().load(new ByteArrayInputStream("""
                {
                           "apiVersion": "apiextensions.k8s.io/v1",
                           "kind": "CustomResourceDefinition",
                           "metadata": { "name": "sparkapplications.sparkoperator.k8s.io" },
                           "spec": {
                             "group": "sparkoperator.k8s.io",
                             "names": { "kind": "SparkApplication", "plural": "sparkapplications" },
                             "scope": "Namespaced",
                             "versions": [{ "name": "v1beta2", "served": true, "storage": true,\s
                                 "schema": { "openAPIV3Schema": { "type": "object", "x-kubernetes-preserve-unknown-fields": true } }\s
                             }]
                           }
                         }
        """.getBytes())).patch();
        String jsonSpec = """
        {
          "apiVersion": "sparkoperator.k8s.io/v1beta2",
          "kind": "SparkApplication",
          "metadata": { "name": "my-spark-job", "namespace": "default" },
          "spec": {
              "type": "Scala",
              "mode": "cluster",
              "image": "apache/spark:3.5.0",
              "mainClass": "org.apache.spark.examples.SparkPi",
              "mainApplicationFile": "local:///opt/spark/examples/jars/spark-examples_2.12-3.5.0.jar",
              "sparkVersion": "3.5.0",
              "sparkConf": {
                "spark.executor.extraJavaOptions": "-Duser.timezone=UTC",
                "spark.sql.shuffle.partitions": "100",
                "spark.hadoop.fs.s3a.impl": "org.apache.hadoop.fs.s3a.S3AFileSystem",
                "spark.kubernetes.allocation.batch.size": "10"
              },
              "driver": {
                "cores": 1,
                "memory": "512m",
                "serviceAccount": "spark"
              },
              "executor": {
                "cores": 1,
                "instances": 1,
                "memory": "512m"
              }
            }
        }""";
        // 3. Запускаем имитацию оператора (он сработает через 1 сек после старта)
        simulateOperator("my-spark-job", "COMPLETED", 1000);

        // 4. Запускаем основной метод
        // 2. Теперь клиент сможет найти метаданные
        utils.submit(client, jsonSpec);
    }

}
