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
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class VolumeMapperService {

    // Public constants for volume configuration properties
    public static final String VOLUME_NAME = "name";
    
    // Types of volumes as prefixes
    public static final String CONFIG_MAP_PREFIX = "configMap.";
    public static final String SECRET_PREFIX = "secret.";
    public static final String PVC_PREFIX = "persistentVolumeClaim.";
    public static final String HOST_PATH_PREFIX = "hostPath.";

    /**
     * Builds a single Volume object from a clean configuration map.
     * Expects keys without volume index prefixes, e.g., "name", "configMap.name".
     */
    public Volume fillVolume(Map<String, String> volumeConf) {
        VolumeBuilder volumeBuilder = new VolumeBuilder();

        // Every volume must have a name
        String volumeName = volumeConf.getOrDefault(VOLUME_NAME, "app-volume");
        volumeBuilder.withName(volumeName);

        // 1. ConfigMap Volume Mapping
        Map<String, String> configMapConf = ConfUtil.extractConf(volumeConf, CONFIG_MAP_PREFIX);
        if (!configMapConf.isEmpty()) {
            ConfigMapVolumeSourceBuilder cmBuilder = new ConfigMapVolumeSourceBuilder();
            if (configMapConf.containsKey("name")) {
                cmBuilder.withName(configMapConf.get("name"));
            }
            volumeBuilder.withConfigMap(cmBuilder.build());
        }

        // 2. Secret Volume Mapping
        Map<String, String> secretConf = ConfUtil.extractConf(volumeConf, SECRET_PREFIX);
        if (!secretConf.isEmpty()) {
            SecretVolumeSourceBuilder secretBuilder = new SecretVolumeSourceBuilder();
            if (secretConf.containsKey("secretName")) {
                secretBuilder.withSecretName(secretConf.get("secretName"));
            }
            volumeBuilder.withSecret(secretBuilder.build());
        }

        // 3. PersistentVolumeClaim Volume Mapping
        Map<String, String> pvcConf = ConfUtil.extractConf(volumeConf, PVC_PREFIX);
        if (!pvcConf.isEmpty()) {
            PersistentVolumeClaimVolumeSourceBuilder pvcBuilder = new PersistentVolumeClaimVolumeSourceBuilder();
            if (pvcConf.containsKey("claimName")) {
                pvcBuilder.withClaimName(pvcConf.get("claimName"));
            }
            volumeBuilder.withPersistentVolumeClaim(pvcBuilder.build());
        }

        // 4. HostPath Volume Mapping
        Map<String, String> hostPathConf = ConfUtil.extractConf(volumeConf, HOST_PATH_PREFIX);
        if (!hostPathConf.isEmpty()) {
            HostPathVolumeSourceBuilder hostPathBuilder = new HostPathVolumeSourceBuilder();
            if (hostPathConf.containsKey("path")) {
                hostPathBuilder.withPath(hostPathConf.get("path"));
            }
            if (hostPathConf.containsKey("type")) {
                hostPathBuilder.withType(hostPathConf.get("type"));
            }
            volumeBuilder.withHostPath(hostPathBuilder.build());
        }

        return volumeBuilder.build();
    }
}
