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
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class PodMapperService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MetadataMapperService metadataMapperService;
    private final PodSpecMapperService podSpecMapperService;
    public PodMapperService(
            MetadataMapperService metadataMapperService,
            PodSpecMapperService podSpecMapperService) {
        this.metadataMapperService = metadataMapperService;
        this.podSpecMapperService = podSpecMapperService;
    }

    public Pod buildPod(Map<String, String> manifestConf) throws TaskConfigurationException {
        Pod result = new PodBuilder()
                .withApiVersion(manifestConf.getOrDefault("apiVersion","v1"))
                .withKind(manifestConf.getOrDefault("kind","Pod"))
                .withMetadata(metadataMapperService.buildObjectMeta(ConfUtil.extractConf(manifestConf,"metadata.")))
                .withSpec(podSpecMapperService.fillPodSpec(ConfUtil.extractConf(manifestConf,"spec.")))
                .build();
        return result;
    }















}
