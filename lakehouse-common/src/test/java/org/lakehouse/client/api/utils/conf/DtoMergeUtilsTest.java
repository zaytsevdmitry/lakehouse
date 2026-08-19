package org.lakehouse.client.api.utils.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.utils.DtoMergeUtils;

import java.util.HashMap;
import java.util.Map;

public class DtoMergeUtilsTest {

    private DtoMergeUtils mergeUtils;

    @Before
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
        Assert.assertEquals("Non-null patch maxRetries should overwrite template value", Integer.valueOf(10), resultOverride.getMaxRetries());

        SQLTemplateDTO patchSql = new SQLTemplateDTO();
        patchSql.setDatabaseSchemaName(null); // Nested null should keep template value
        patchSql.setTableFullName("analytics_zone.events"); // Nested non-null should overwrite
        patchSql.setTableDDLCreate(null); // Nested null should keep template value
        patch.setSqlTemplate(patchSql);

        // 3. Act: Execute Merge
        TaskDTO result = mergeUtils.merge(template, patch, TaskDTO.class);

        // 4. Assert: Verify the merge result matches expectations
        Assert.assertNotNull("Merged result should not be null", result);

        // Verify top-level simple fields
        Assert.assertEquals("Null patch value should not overwrite template name", "base-template-task", result.getName());
        Assert.assertEquals("Non-null patch value should overwrite template importance", "HIGH", result.getImportance());
        Assert.assertEquals("Implicitly null patch field should retain template value", "SparkProcessor", result.getTaskProcessor());
        Assert.assertEquals("Empty string in patch should overwrite template description", "", result.getDescription());
        Assert.assertEquals("Null patch maxRetries should retain template value", Integer.valueOf(5), result.getMaxRetries());

        // Verify Map merging logic
        Map<String, String> mergedArgs = result.getTaskProcessorArgs();
        Assert.assertNotNull("Merged Map should not be null", mergedArgs);
        Assert.assertEquals("Template key should be preserved if absent in patch", "local[*]", mergedArgs.get("spark.master"));
        Assert.assertEquals("Patch key should overwrite template key value", "4g", mergedArgs.get("spark.executor.memory"));
        Assert.assertEquals("New patch key should be added to the map", "1g", mergedArgs.get("spark.driver.memory"));
        Assert.assertEquals("Map should contain exactly 3 aggregated entries", 3, mergedArgs.size());

        // Verify deeply nested object (SQLTemplateDTO) merging logic
        SQLTemplateDTO mergedSql = result.getSqlTemplate();
        Assert.assertNotNull("Merged nested SQLTemplateDTO should not be null", mergedSql);
        Assert.assertEquals("Nested null in patch should preserve template schema name", "raw_zone", mergedSql.getDatabaseSchemaName());
        Assert.assertEquals("Nested non-null in patch should overwrite template table name", "analytics_zone.events", mergedSql.getTableFullName());
        Assert.assertEquals("Nested null should preserve template DDL", "CREATE TABLE raw_zone.events (id INT)", mergedSql.getTableDDLCreate());
        Assert.assertEquals("Omitted nested field in patch should retain template DDL", "DROP TABLE raw_zone.events", mergedSql.getTableDDLDrop());
    }

    @Test
    public void testMergeWithNulls() {
        TaskDTO mockDto = new TaskDTO();
        mockDto.setName("isolated-task");

        // Test case when patch is null -> should return target
        TaskDTO result1 = mergeUtils.merge(mockDto, null, TaskDTO.class);
        Assert.assertSame("Should return template directly if patch is null", mockDto, result1);

        // Test case when target is null -> should return patch
        TaskDTO result2 = mergeUtils.merge(null, mockDto, TaskDTO.class);
        Assert.assertSame("Should return patch directly if template is null", mockDto, result2);

        // Test case when both are null -> should return null
        TaskDTO result3 = mergeUtils.merge(null, null, TaskDTO.class);
        Assert.assertNull("Should return null if both arguments are null", result3);
    }
}
