/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.taskexecutor.processor.spark.k8snative;

import io.fabric8.kubernetes.api.model.*;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 * Settings passed via spark.kubernetes.* parameters that are required during
 * the pre-launch phase of the spark context must be moved to the manifest.
 * These settings only need to be specified in one place: either in the manifest
 * or in spark.kubernetes.* . Specifying them in both places will overwrite the original manifest
 * setting.
 * https://spark.apache.org/docs/3.5.8/running-on-kubernetes.html
 *
 * spark.kubernetes.driver.pod.name == allways rewrite
 * Unsupported properties
 * spark.kubernetes.{driver,executor}.label.*
 * spark.kubernetes.{driver,executor}.annotation.*
 * spark.kubernetes.{driver,executor}.volumes.[VolumeType].[VolumeName].mount.path
 * </p>
 */
@Service
public class PodUtilService {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String restConfUrl;
    private final String restSchedulerUrl;

    public PodUtilService(
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl) {
        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
    }

    public void fixDriverPod(
            Pod pod,
            Map<String, String> taskConf,
            Map<String, String> sparkConf,
            Map<String, String> appConf,
            String masterUrl,
            ScheduledTaskDTO scheduledTaskDTO
    ) throws TaskConfigurationException {
        Container driverContainer;
        List<String> args = new ArrayList<>();
        if (pod.getSpec() == null) {
            pod.setSpec(new PodSpecBuilder().build());
        }
        if (pod.getSpec().getContainers() == null ||
                pod.getSpec().getContainers().isEmpty()) {
            driverContainer = new Container();
        } else {
            driverContainer = pod.getSpec().getContainers().get(0);
            args.addAll(driverContainer.getArgs());
        }
        // names
        String driverPodName = getFixedTaskName(scheduledTaskDTO, pod);
        pod.getMetadata().setName(driverPodName);
        String  taskFullName = scheduledTaskDTO.buildTaskFullName();

        pod.getMetadata().getAnnotations().put("lakehouse-management-task", taskFullName);

        pod.getMetadata().setNamespace(
                sparkConf.getOrDefault(
                        "spark.kubernetes.namespace",
                        pod.getMetadata().getNamespace() == null ? "default" : pod.getMetadata().getNamespace()));


        if (pod.getSpec().getRestartPolicy() == null || "".equals(pod.getSpec().getRestartPolicy()))
            pod.getSpec().setRestartPolicy("never");

        String pullSecretsConfig = sparkConf.getOrDefault("spark.kubernetes.container.image.pullSecrets", "");
        if (!"".equals(pullSecretsConfig)) {
            List<LocalObjectReference> imagePullSecrets = new ArrayList<>(pod.getSpec().getImagePullSecrets());
            imagePullSecrets.addAll(
                    Arrays
                    .stream(pullSecretsConfig.split(","))
                    .filter(s -> {
                        if (pod.getSpec().getImagePullSecrets() == null || pod.getSpec().getImagePullSecrets().isEmpty())
                            return false;
                        else
                            for (LocalObjectReference r : pod.getSpec().getImagePullSecrets())
                                if (r.getName().equals(s))
                                    return true;
                        return false;
                    })
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(name -> new LocalObjectReferenceBuilder().withName(name).build())
                    .toList());
            pod.getSpec().setImagePullSecrets(imagePullSecrets);
        }
        String serviceAccountName = sparkConf.getOrDefault("spark.kubernetes.authenticate.driver.serviceAccountName", "");
        if (!"".equals(serviceAccountName)) {
            pod.getSpec().setServiceAccount(serviceAccountName);// back capability
            pod.getSpec().setServiceAccountName(serviceAccountName);
        }

        args.addAll(extractAppArguments(taskConf, sparkConf, appConf, masterUrl, scheduledTaskDTO,driverPodName));
        pod.getSpec().getContainers().clear();
        pod.getSpec().getContainers().add(
                new ContainerBuilder(driverContainer)
                        .withResources(translateSparkConfToResources(sparkConf))
                        .withImage(resolveImageName(driverContainer.getImage(), sparkConf))
                        .withImagePullPolicy(sparkConf.getOrDefault("spark.kubernetes.container.image.pullPolicy", driverContainer.getImagePullPolicy()))
                        .withArgs(args)
                        .withName("spark-driver")
                        .withCommand(taskConf.getOrDefault("command", "/opt/bin/spark-submit"))
                        .addNewEnv()
                        .withName("POD_IP")
                        .withNewValueFrom()
                        .withNewFieldRef()
                        .withFieldPath("status.podIP")
                        .endFieldRef()
                        .endValueFrom()
                        .endEnv()
                        .build());
    }

    String resolveImageName(
            String image,
            Map<String, String> sparkConf) throws TaskConfigurationException {
        String driverImageKey = "spark.kubernetes.driver.container.image";
        String containerImageKey = "spark.kubernetes.container.image";
        String foundedImage;
        if (sparkConf.containsKey(driverImageKey))
            foundedImage = sparkConf.get(driverImageKey);
        else if (sparkConf.containsKey(containerImageKey))
            foundedImage = sparkConf.get(containerImageKey);
        else if (image == null || image.isBlank())
            throw new TaskConfigurationException("Container image name not found");
        else foundedImage = image;

        logger.info("Used image name: {}", foundedImage);
        return foundedImage;
    }


    /**
     * //DNS RFC 1123  Kubernetes
     * //todo This value needs some work for Kubernetes. Additionally, it is expected that the subname cannot exceed 63 characters.
     * //todo next time need create getTaskFullName function in jijava  ".metadata.name" : {{ k8s_dns_rfc_1123(scheduledTask)}}
     */
    String getFixedTaskName(
            ScheduledTaskDTO scheduledTaskDTO,
            Pod k8SSparkApplicationConf) {
        String rawName = null;
        if (k8SSparkApplicationConf != null && k8SSparkApplicationConf.getMetadata() != null) {
            rawName = k8SSparkApplicationConf.getMetadata().getName();
        }
        if (rawName == null || rawName.isBlank()) {
            rawName = String.format("task-%d-%d-%d", scheduledTaskDTO.getId(), scheduledTaskDTO.getTryNum(), DateTimeUtils.now().hashCode());
        }
        String cleanName = rawName.replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");

        int targetLength = Math.min(cleanName.length(), 63);
        String fixedTaskName = cleanName.substring(0, targetLength);

        if (fixedTaskName.endsWith("-")) {
            fixedTaskName = fixedTaskName.substring(0, fixedTaskName.length() - 1);
        }
        if (fixedTaskName.startsWith("-")) {
            fixedTaskName = fixedTaskName.substring(1);
        }
        return fixedTaskName;
    }

    List<String> extractAppArguments(
            Map<String, String> taskConf,
            Map<String, String> sparkConf,
            Map<String, String> appConf,
            String masterUrl,
            ScheduledTaskDTO scheduledTaskDTO,
            String podNamePrefix
    ) throws TaskConfigurationException {

        List<String> resultList = new ArrayList<>();

        resultList.add("--master");
        resultList.add("k8s://" + masterUrl);

        resultList.add("--name"); // for spark-history ui
        resultList.add(scheduledTaskDTO.buildTaskFullName());

        Map<String,String> sparkConfOverrided = new HashMap<>(sparkConf);
        sparkConfOverrided.put("spark.kubernetes.executor.podNamePrefix", podNamePrefix);


        //todo next time need create getTaskFullName function in jijava  "spark.app.name" : {{ taskFullName(scheduledTask)}}
        // use prefix
        //sparkConfOverrided.put("spark.app.name", scheduledTaskDTO.buildTaskFullName()); //The Human-Readable Name
        sparkConfOverrided.forEach((key, value) -> {
            resultList.add("--conf");
            resultList.add(String.format("%s=%s", key, value));
        });


        if (taskConf.containsKey("mainClass")) {
            resultList.add("--class");
            resultList.add(taskConf.get("mainClass"));
        }

        // internal class in classpath
        resultList.add(taskConf.getOrDefault("appResource", "spark-internal"));

        // task args outside
        Map<String,String> argsMap = new HashMap<>(appConf);
        argsMap.put("scheduledTaskId", String.valueOf(scheduledTaskDTO.getId()));
        argsMap.put(ConfigRestClientConstants.restConfKey,restConfUrl);
        argsMap.put(SchedulerRestClientConstants.restSchedulerKey,restSchedulerUrl);

        resultList.addAll(argsMap
                .entrySet()
                .stream()
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .toList());
        return resultList;
    }

    /**
     * Spark-> Pod (Fabric8)
     */
    public ResourceRequirements translateSparkConfToResources(Map<String, String> sparkConf) {
        String coresStr = sparkConf.getOrDefault("spark.driver.cores", "1");
        Quantity cpuQuantity = new Quantity(coresStr);

        String rawMemory = sparkConf.getOrDefault("spark.driver.memory", "1g");
        long memoryBytes = parseSparkMemoryToBytes(rawMemory);


        long overheadBytes = parseSparkMemoryToBytes(
                sparkConf.getOrDefault(
                        "spark.driver.memoryOverhead",
                        String.valueOf(Math.max((long) (memoryBytes * 0.10), 384L * 1024 * 1024))));

        long totalMemoryBytes = memoryBytes + overheadBytes;
        long totalMemoryMi = totalMemoryBytes / (1024 * 1024);
        Quantity memoryQuantity = new Quantity(totalMemoryMi + "Mi");

        return new ResourceRequirementsBuilder()
                .addToRequests("cpu", cpuQuantity)
                .addToRequests("memory", memoryQuantity)
                .addToLimits("cpu", cpuQuantity)
                .addToLimits("memory", memoryQuantity)
                .build();
    }

    long parseSparkMemoryToBytes(String memoryStr) {
        if (memoryStr == null || memoryStr.isBlank()) {
            return 1024L * 1024 * 1024; // 1GB по умолчанию
        }

        String cleanStr = memoryStr.trim().toLowerCase();
        long multiplier = 1;
        String numberStr = cleanStr;

        if (cleanStr.endsWith("g") || cleanStr.endsWith("gb")) {
            multiplier = 1024L * 1024 * 1024;
            numberStr = cleanStr.replaceAll("[gb]", "");
        } else if (cleanStr.endsWith("m") || cleanStr.endsWith("mb")) {
            multiplier = 1024L * 1024;
            numberStr = cleanStr.replaceAll("[mb]", "");
        } else if (cleanStr.endsWith("k") || cleanStr.endsWith("kb")) {
            multiplier = 1024L;
            numberStr = cleanStr.replaceAll("[kb]", "");
        }

        try {
            return Long.parseLong(numberStr) * multiplier;
        } catch (NumberFormatException e) {
            logger.info("Check spark resource values. Try to use 1 GB when parsing error {}", e.getMessage());
            return 1024L * 1024 * 1024; // when error try  1GB
        }
    }
}
