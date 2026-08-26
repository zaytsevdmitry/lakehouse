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
package org.lakehouse.client.api.utils.conf;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.utils.DtoMergeUtils;

import java.util.HashMap;
import java.util.Map;

public class DtoMergeUtilsTest {

    private DtoMergeUtils mergeUtils;

    @BeforeEach
    public void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mergeUtils = new DtoMergeUtils(mapper);
    }

    @Test
    public void testDeepMergeTaskDto() {
        // 1. Arrange: Prepare Template (Source Configuration)
        TaskDTO template = new TaskDTO();
        template.setName("base-template-task");
        template.setImportance("LOW");
        template.setTaskProcessor("SparkProcessor");
        template.setDescription("Base description");
        template.setMaxRetries(5);

        Map<String, String> templateArgs = new HashMap<>();
        templateArgs.put("spark.master", "local[*]");
        templateArgs.put("spark.executor.memory", "2g");
        template.setTaskProcessorArgs(templateArgs);

        SQLTemplateDTO templateSql = new SQLTemplateDTO();
        templateSql.setDatabaseSchemaName("raw_zone");
        templateSql.setTableFullName("raw_zone.events");
        templateSql.setTableDDLCreate("CREATE TABLE raw_zone.events (id INT)");
        templateSql.setTableDDLDrop("DROP TABLE raw_zone.events");
        template.setSqlTemplate(templateSql);

        // 2. Arrange: Prepare Patch (Refinements to apply on top)
        TaskDTO patch = new TaskDTO();
        patch.setName(null); // Null field should not overwrite the template
        patch.setImportance("HIGH"); // Non-null field should overwrite the template
        patch.setDescription(""); // Empty string is non-null, should overwrite

        Map<String, String> patchArgs = new HashMap<>();
        patchArgs.put("spark.executor.memory", "4g"); // Should overwrite existing key
        patchArgs.put("spark.driver.memory", "1g");   // Should add a new key
        patch.setTaskProcessorArgs(patchArgs);

        TaskDTO patchWithMaxRetries = new TaskDTO();
        patchWithMaxRetries.setMaxRetries(10);
        TaskDTO resultOverride = mergeUtils.merge(template, patchWithMaxRetries, TaskDTO.class);
        Assertions.assertEquals(Integer.valueOf(10), resultOverride.getMaxRetries(), "Non-null patch maxRetries should overwrite template value");

        SQLTemplateDTO patchSql = new SQLTemplateDTO();
        patchSql.setDatabaseSchemaName(null); // Nested null should keep template value
        patchSql.setTableFullName("analytics_zone.events"); // Nested non-null should overwrite
        patchSql.setTableDDLCreate(null); // Nested null should keep template value
        patch.setSqlTemplate(patchSql);

        // 3. Act: Execute Merge
        TaskDTO result = mergeUtils.merge(template, patch, TaskDTO.class);

        // 4. Assert: Verify the merge result matches expectations
        Assertions.assertNotNull(result, "Merged result should not be null");

        // Verify top-level simple fields
        Assertions.assertEquals("base-template-task", result.getName(), "Null patch value should not overwrite template name");
        Assertions.assertEquals("HIGH", result.getImportance(), "Non-null patch value should overwrite template importance");
        Assertions.assertEquals("SparkProcessor", result.getTaskProcessor(), "Implicitly null patch field should retain template value");
        Assertions.assertEquals("", result.getDescription(), "Empty string in patch should overwrite template description");
        Assertions.assertEquals(Integer.valueOf(5), result.getMaxRetries(), "Null patch maxRetries should retain template value");

        // Verify Map merging logic
        Map<String, String> mergedArgs = result.getTaskProcessorArgs();
        Assertions.assertNotNull(mergedArgs, "Merged Map should not be null");
        Assertions.assertEquals("local[*]", mergedArgs.get("spark.master"), "Template key should be preserved if absent in patch");
        Assertions.assertEquals("4g", mergedArgs.get("spark.executor.memory"), "Patch key should overwrite template key value");
        Assertions.assertEquals("1g", mergedArgs.get("spark.driver.memory"), "New patch key should be added to the map");
        Assertions.assertEquals(3, mergedArgs.size(), "Map should contain exactly 3 aggregated entries");

        // Verify deeply nested object (SQLTemplateDTO) merging logic
        SQLTemplateDTO mergedSql = result.getSqlTemplate();
        Assertions.assertNotNull(mergedSql, "Merged nested SQLTemplateDTO should not be null");
        Assertions.assertEquals("raw_zone", mergedSql.getDatabaseSchemaName(), "Nested null in patch should preserve template schema name");
        Assertions.assertEquals("analytics_zone.events", mergedSql.getTableFullName(), "Nested non-null in patch should overwrite template table name");
        Assertions.assertEquals("CREATE TABLE raw_zone.events (id INT)", mergedSql.getTableDDLCreate(), "Nested null should preserve template DDL");
        Assertions.assertEquals("DROP TABLE raw_zone.events", mergedSql.getTableDDLDrop(), "Omitted nested field in patch should retain template DDL");
    }

    @Test
    public void testMergeWithNulls() {
        TaskDTO mockDto = new TaskDTO();
        mockDto.setName("isolated-task");

        // Test case when patch is null -> should return target
        TaskDTO result1 = mergeUtils.merge(mockDto, null, TaskDTO.class);
        Assertions.assertSame(mockDto, result1, "Should return template directly if patch is null");

        // Test case when target is null -> should return patch
        TaskDTO result2 = mergeUtils.merge(null, mockDto, TaskDTO.class);
        Assertions.assertSame(mockDto, result2, "Should return patch directly if template is null");

        // Test case when both are null -> should return null
        TaskDTO result3 = mergeUtils.merge(null, null, TaskDTO.class);
        Assertions.assertNull(result3, "Should return null if both arguments are null");
    }
}
