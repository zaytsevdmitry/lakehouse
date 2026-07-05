/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.processor.spark.k8snative.mappers;

import io.fabric8.kubernetes.api.model.*;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.taskexecutor.processor.spark.k8snative.manifestkeys.PodKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;

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
public class SparkDriverContainerMapperService {

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String SPARK_DRIVER_NAME = "lakehouse-spark-driver";

    public void applySparkDriverContainer(
            Pod pod,
            Map<String, String> taskConf,
            Map<String, String> sparkConf,
            Map<String, String> appConf,
            String masterUrl
    ) throws TaskConfigurationException {

        Container driverContainer = prepareDriverContainer(pod);

        driverContainer.setResources(translateSparkConfToResources(sparkConf));
        driverContainer.setImage(resolveImageName(driverContainer.getImage(), sparkConf));
        driverContainer.setImagePullPolicy(sparkConf.getOrDefault("spark.kubernetes.container.image.pullPolicy", driverContainer.getImagePullPolicy()));
        driverContainer.setArgs(extractAppArguments(driverContainer.getArgs(),taskConf,sparkConf,appConf,masterUrl,pod.getMetadata().getAnnotations().get(PodKeys.METADATA_ANNOTATIONS_TASK_NAME_KEY)));
        driverContainer.setCommand(Arrays.asList(taskConf.getOrDefault("command", "/opt/bin/spark-submit")));
        List<EnvVar> envVars = driverContainer.getEnv() != null ?
                new ArrayList<>(driverContainer.getEnv()) : new ArrayList<>();
        boolean hasPodIp = envVars.stream().anyMatch(e -> "POD_IP".equals(e.getName()));
        if (!hasPodIp) {
            envVars.add(new EnvVarBuilder()
                    .withName("POD_IP")
                    .withNewValueFrom()
                    .withNewFieldRef()
                    .withFieldPath("status.podIP")
                    .endFieldRef()
                    .endValueFrom()
                    .build());
        }
        driverContainer.setEnv(envVars);
        driverContainer.setEnv(envVars);
    }

    private Container prepareDriverContainer(Pod pod){
        Container result;
        if (pod.getSpec().getContainers() == null){
            pod.getSpec().setContainers(new ArrayList<>());
        }
        // search configured driver-container
        Optional<Container> oc = pod.getSpec()
                .getContainers()
                .stream()
                .filter(c-> c.getName().equals(SPARK_DRIVER_NAME))
                .findFirst();
        if(oc.isPresent()){
            result = oc.get();
        }else {
            result = new ContainerBuilder().withName(SPARK_DRIVER_NAME).build();
            pod.getSpec().getContainers().add(result);
        }
        if (result.getArgs() == null) result.setArgs(new ArrayList<>());
        return result;
    }

    List<String> extractAppArguments(
            List<String> inputArgs,
            Map<String, String> taskConf,
            Map<String, String> sparkConf,
            Map<String, String> appConf,
            String masterUrl,
            String appName
    )  {

        List<String> resultList = new ArrayList<>(inputArgs);

        resultList.add("--master");
        resultList.add("k8s://" + masterUrl);

        resultList.add("--name"); // for spark-history ui
        resultList.add(appName);

        // Insert --properties-file parameter at the beginning of the spark-submit arguments list
        String propertiesFilePath = taskConf.getOrDefault(TaskProcessorArgKey.K8S_NATIVE_PROPERTY_FILE, "");
        if (StringUtils.hasText(propertiesFilePath)) {
            resultList.add("--properties-file");
            resultList.add(taskConf.get("propertiesFile"));
        }

        sparkConf.forEach((key, value) -> {
            resultList.add("--conf");
            resultList.add(String.format("%s=%s", key, value));
        });

        if (taskConf.containsKey("mainClass")) {
            resultList.add("--class");
            resultList.add(taskConf.get("mainClass"));
        }

        // internal class in classpath
        resultList.add(taskConf.getOrDefault("appResource", "spark-internal"));


        resultList.addAll(appConf
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

    public long parseSparkMemoryToBytes(String memoryStr) {
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


}