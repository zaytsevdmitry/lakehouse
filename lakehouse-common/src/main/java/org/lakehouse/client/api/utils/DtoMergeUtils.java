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
package org.lakehouse.client.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class DtoMergeUtils {

    private final ObjectMapper mapper;

    public DtoMergeUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Deeply merges a template object and a patch (refinement) object.
     * 
     * @param target the template object (source)
     * @param patch  the object with refinements (applied on top)
     * @param clazz  the class of the objects
     * @return a new merged instance of the class
     */
    public <T> T merge(T target, T patch, Class<T> clazz) {
        // Handle cases where one of the objects is null
        if (target == null) return patch;
        if (patch == null) return target;

        try {
            // Convert both objects to JSON trees for safe merging
            JsonNode targetNode = mapper.valueToTree(target);
            JsonNode patchNode = mapper.valueToTree(patch);

            // Execute deep node merging
            JsonNode mergedNode = mergeNodes(targetNode, patchNode);

            // Read the resulting tree back into an object of the target class
            return mapper.treeToValue(mergedNode, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform deep DTO merge", e);
        }
    }

    private JsonNode mergeNodes(JsonNode target, JsonNode patch) {
        // If the patch value is missing or explicitly null, keep the original
        if (patch == null || patch.isNull()) {
            return target;
        }

        // CASE 1: Both nodes are objects (nested POJOs or Maps)
        if (target.isObject() && patch.isObject()) {
            ObjectNode targetObj = (ObjectNode) target;
            ObjectNode patchObj = (ObjectNode) patch;

            Iterator<Map.Entry<String, JsonNode>> fields = patchObj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode patchValue = field.getValue();

                // If the field value in the patch is null, ignore it
                if (patchValue.isNull()) {
                    continue; 
                }

                if (targetObj.has(fieldName)) {
                    // If the field exists in both, recurse for a deep merge
                    JsonNode mergedValue = mergeNodes(targetObj.get(fieldName), patchValue);
                    targetObj.set(fieldName, mergedValue);
                } else {
                    // If the field does not exist in the template, simply add it from the patch
                    targetObj.set(fieldName, patchValue);
                }
            }
            return targetObj;
        }

        // CASE 2: Both nodes are arrays (Lists / Arrays) -> Combine them
        if (target.isArray() && patch.isArray()) {
            ArrayNode targetArr = (ArrayNode) target;
            ArrayNode patchArr = (ArrayNode) patch;
            
            // Append all elements from the patch to the end of the template array
            targetArr.addAll(patchArr);
            return targetArr;
        }

        // CASE 3: Primitive types (String, Number, Boolean) or mismatched types -> Patch wins
        return patch;
    }
}
