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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetadataMapperService {
    public ObjectMeta buildObjectMeta(Map<String,String> metaConf) throws TaskConfigurationException {

    /*
    + String name
    + String namespace
    + String uid
    + Map<String, String> labels
    + Map<String, String> annotations
    + List<OwnerReference> ownerReferences
    **/
        // names
        ObjectMetaBuilder objectMetaBuilder = new ObjectMetaBuilder()
                .withName(metaConf.get("name"))
                .withNamespace(metaConf.getOrDefault("namespace", "default"));

        addLabels(objectMetaBuilder, ConfUtil.extractConf(metaConf,"labels."));
        addAnnotations(objectMetaBuilder, ConfUtil.extractConf(metaConf,"annotations."));
        addListOwnerReference(objectMetaBuilder, ConfUtil.extractConf(metaConf,"ownerReferences."));
        return objectMetaBuilder.build();
    }

    private void addLabels(ObjectMetaBuilder objectMetaBuilder,Map<String, String> labels){
        if(!labels.isEmpty())
            objectMetaBuilder.withLabels(labels);
    }
    private void addAnnotations(ObjectMetaBuilder objectMetaBuilder,Map<String, String> annotations){
        if(!annotations.isEmpty())
            objectMetaBuilder.withAnnotations(annotations);
    }
    private void addListOwnerReference(ObjectMetaBuilder objectMetaBuilder,Map<String, String> ownerReferences) throws TaskConfigurationException {

        /*
        *   + String apiVersion
        + String kind
        + String name
        + String uid
        + Boolean controller
        + Boolean blockOwnerDeletion*/
            if(!ownerReferences.isEmpty()){
            Set<String> keyset = new HashSet<>();
            // get all keys
            for (String k: ownerReferences.keySet()){
                int dotIndex = k.indexOf(".");

                if (dotIndex != -1) {
                    String result = k.substring(0, dotIndex);
                    keyset.add(result);
                }else {
                    throw new TaskConfigurationException(String.format("Wrong key structure in k8s meta owner reference in key %s",k));
                }
            }
            //walk around keys
            List<OwnerReference> ownerReferenceList = new ArrayList<>();
            for (String k:keyset){
                Map<String,String> or = ConfUtil.extractConf(ownerReferences,k);
                if(!or.isEmpty()){
                    OwnerReference ownerReference =  new OwnerReference();
                    ownerReference.setKind(or.getOrDefault("kind",null));
                    ownerReference.setName(or.getOrDefault("name",null));
                    ownerReference.setUid(or.getOrDefault("uid",null));
                    ownerReference.setController(Boolean.valueOf(or.getOrDefault("controller",Boolean.toString(false))));
                    ownerReference.setBlockOwnerDeletion(Boolean.valueOf(or.getOrDefault("blockOwnerDeletion",Boolean.toString(false))));
                    ownerReferenceList.add(ownerReference);
                }
            }
            objectMetaBuilder.withOwnerReferences(ownerReferenceList);
        }
    }



}
