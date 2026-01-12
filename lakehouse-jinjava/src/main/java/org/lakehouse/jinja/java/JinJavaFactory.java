package org.lakehouse.jinja.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.functions.JinjavaDateTimeFunctions;
import org.lakehouse.jinja.java.functions.TaskProcessConfigExtractor;

import java.util.List;
import java.util.Map;

public class JinJavaFactory {
    public static Jinjava getJinjava() {
        Jinjava jinjava = new Jinjava();
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "adddays",
                        JinjavaDateTimeFunctions.class,
                        "addDaysISO",
                        String.class, Integer.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "addmonths",
                        JinjavaDateTimeFunctions.class,
                        "addMonthsISO",
                        String.class, Integer.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "ref",
                        TaskProcessConfigExtractor.class,
                        "ref",
                        String.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "refCat",
                        TaskProcessConfigExtractor.class,
                        "refCat",
                        String.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "extractColumnsDDL",
                        TaskProcessConfigExtractor.class,
                        "extractColumnsDDL",
                        List.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "extractMergeOn",
                        TaskProcessConfigExtractor.class,
                        "extractMergeOn",
                        Map.class,String.class,String.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "extractMergeUpdate",
                        TaskProcessConfigExtractor.class,
                        "extractMergeUpdate",
                        Map.class,String.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "extractMergeInsertValues",
                        TaskProcessConfigExtractor.class,
                        "extractMergeInsertValues",
                        Map.class,String.class));
        jinjava.getGlobalContext().registerFunction(
                new ELFunctionDefinition(
                        "",
                        "extractColumnsCS",
                        TaskProcessConfigExtractor.class,
                        "extractColumnsCS",
                        Map.class));

        return jinjava;
    }
    public static Jinjava getJinjava(TaskProcessorConfigDTO taskProcessorConfigDTO) throws JsonProcessingException {
        Jinjava jinjava = getJinjava();
        jinjava.getGlobalContext().putAll(ObjectMapping.asMap(taskProcessorConfigDTO));
        return jinjava;
    }
}
