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

package org.lakehouse.jinja.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.functions.JinjavaDateTimeFunctions;
import org.lakehouse.jinja.java.functions.TaskProcessConfigExtractor;

import java.util.List;
import java.util.Map;

public class JinJavaFactory {
    public static JinJavaUtils getJinJavaUtils() {
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
                        "refCatSchema",
                        TaskProcessConfigExtractor.class,
                        "refCatSchema",
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

        return new JinJavaUtils(jinjava);
    }
    public static JinJavaUtils getJinJavaUtils(SourceConfDTO sourceConfDTO, ScheduledTaskDTO scheduledTaskDTO) throws JsonProcessingException {
       return getJinJavaUtils()
               .injectGlobalContext(ObjectMapping.asMap(sourceConfDTO))
               .injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

    }
}
