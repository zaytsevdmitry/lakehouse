package org.lakehouse.taskexecutor.processor.spark;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.K8sSparkOperatorClientUtils;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.SparkK8sOperatorConfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class SparkK8sOperatorTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long STARTUP_TIMEOUT_MINUTES = 2;
    private static final long BUSINESS_TIMEOUT_MINUTES = 180;

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils)
            throws TaskFailedException,
                   TaskConfigurationException {

        Map<String, String> taskParams = SparkK8sOperatorConfUtil.extractK8sOperatorConf(sourceConfDTO,scheduledTaskDTO);

        taskParams.forEach((s, s2) -> logger.info("Task parameter {} -> {}", s,s2));

        String masterUrl = SparkK8sOperatorConfUtil.extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtils);
        logger.info("MasterUrl is {}", masterUrl);

        String namespace = taskParams.getOrDefault("metadata.namespace", "default").toString();
        logger.info("Namespace is {}", namespace);

        K8sSparkOperatorClientUtils utils = new K8sSparkOperatorClientUtils();
        KubernetesClient kubernetesClient = utils.buildKubernetesClient(
                masterUrl,
                namespace);

        String json = null;
        //todo next time need create getTaskFullName function in jijava  "spark.app.name" : {{ taskFullName(scheduledTask)}}
        // todo 2 it isn't work? because operator rewrite this value  "spark.app.name" = name of driver pod
        sourceConfDTO.getTargetDataSource().getService().getProperties().put("spark.app.name", scheduledTaskDTO.getTaskFullName());


        json = SparkK8sOperatorConfUtil.extractAppConfJson(sourceConfDTO,scheduledTaskDTO);
        long startupTimeoutMinutes = SparkK8sOperatorConfUtil.getLongByKey(
                SparkK8sOperatorConfUtil.castToStringMap(taskParams),
                "startupTimeoutMinutes",
                STARTUP_TIMEOUT_MINUTES);
        long businessTimeoutMinutes  = SparkK8sOperatorConfUtil
                .getLongByKey(
                        SparkK8sOperatorConfUtil.castToStringMap(taskParams),
                        "businessTimeoutMinutes",
                        STARTUP_TIMEOUT_MINUTES);

        utils.submit(kubernetesClient, json , startupTimeoutMinutes ,businessTimeoutMinutes);
    }
}
