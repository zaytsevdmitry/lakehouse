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
