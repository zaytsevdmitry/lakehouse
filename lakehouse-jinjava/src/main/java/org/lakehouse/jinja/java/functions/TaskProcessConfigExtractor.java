package org.lakehouse.jinja.java.functions;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.NameDescriptionAbstract;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  {{source(dataSetKey)}} returns table
 */
public class TaskProcessConfigExtractor {
    private static final Logger logger = LoggerFactory.getLogger(TaskProcessConfigExtractor.class);
    public static String ref(String dataSet) {
        if (dataSet == null || dataSet.isBlank()){
            logger.error("error in Jinjava function. Attribute 'dataSet' can not be blank");
            throw new IllegalArgumentException("Attribute 'dataSet' can not be blank");
        }
        else {

            String result =
                    "{% if dataSets['" + dataSet + "'].databaseSchemaName is defined %}" +
                            "{{ dataSets['" + dataSet + "'].databaseSchemaName~'.'~dataSets['" + dataSet + "'].tableName }}" +
                            "{% else %}" +
                            "{{ dataSets['" + dataSet + "'].tableName }}\n" +
                            "{% endif %}";
            System.out.println(result);
            return result;
        }
    }

    public static String refCat(String dataSet) {
        if (dataSet == null || dataSet.isBlank())
            throw new IllegalArgumentException("Attribute 'dataSet' can not be blank");
        try {

            String result = "{{ dataSets['" + dataSet + "'].dataSourceKeyName~'.'~dataSets['" + dataSet + "'].databaseSchemaName~'.'~dataSets['" + dataSet + "'].tableName }}";
            System.out.println(result);
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public static String extractColumnsDDL(List<LinkedHashMap<String,String>> columnList) {
        if (columnList == null || columnList.isEmpty())
            throw new IllegalArgumentException("Attribute 'columnDTOS' can not be blank");
        try {
            List<String> resultList = new ArrayList<>();
            List<ColumnDTO> columnDTOS = columnList
                    .stream()
                    .map(s -> {
                        try {
                            return ObjectMapping.stringToObject(ObjectMapping.asJsonString(s),ColumnDTO.class);
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            columnDTOS
                    .stream().map(columnDTO ->  columnDTO.getName() + " " + columnDTO.getDataType())
                            //columnDTO.get("name") + " " + columnDTO.get("dataType"))
                    .forEach(resultList::add);

            String result = String.join(",\n", resultList);

            System.out.println(result);
            return result;
        } catch (Exception e) {
           logger.error(e.getMessage());
            throw e;
        }

    }

    private static Optional<Map.Entry<String,DataSetConstraintDTO>> findByType(
            DataSetDTO dataSetDTO,
            Types.Constraint constraintType){
        Optional<Map.Entry<String,DataSetConstraintDTO>> result = dataSetDTO
                .getConstraints()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getType().equals(constraintType))
                .findFirst();
        return result;
    }
    private static List<String> getKeyColumnNames(
        DataSetDTO dataSetDTO){
    return Arrays.stream(
            findByType(dataSetDTO,Types.Constraint.primary)
            .orElseGet(() ->
                    findByType(dataSetDTO,Types.Constraint.unique)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            String.format("Attribute 'dataSetDTO'='%s' must contain primary or unique constraint",dataSetDTO.getKeyName()))))
            .getValue()
            .getColumns()
            .split(","))
            .map(String::trim)
            .toList();
    }
    public static String extractMergeOn(Map<String,Object> dataSetMap, String targetAlias, String queryAlias) {
        if (dataSetMap == null)
            throw new IllegalArgumentException("Attribute 'dataSetDTO' can not be null");
        try {
            DataSetDTO dataSetDTO =
                    ObjectMapping.stringToObject(ObjectMapping.asJsonString(dataSetMap),DataSetDTO.class);
            List<String> keyColNames = getKeyColumnNames(dataSetDTO);

            return dataSetDTO
                    .getColumnSchema()
                    .stream()
                    .filter(columnDTO -> keyColNames.contains(columnDTO.getName()))
                    .map(columnDTO ->
                            targetAlias + "." + columnDTO.getName() + " = "
                                    + queryAlias + "." + columnDTO.getName())
                    .collect(Collectors.joining("\n AND "));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String extractMergeUpdate(Map<String,Object> dataSetMap,  String queryAlias) {
        if (dataSetMap == null)
            throw new IllegalArgumentException("Attribute 'dataSetDTO' can not be null");
        if (queryAlias == null || queryAlias.isBlank())
            throw new IllegalArgumentException("Attribute 'queryAlias' can not be blank");

        try {
            DataSetDTO dataSetDTO =
                    ObjectMapping.stringToObject(ObjectMapping.asJsonString(dataSetMap),DataSetDTO.class);

            List<String> keyColNames = getKeyColumnNames(dataSetDTO);

            return dataSetDTO
                    .getColumnSchema()
                    .stream()
                    .filter(columnDTO -> !keyColNames.contains(columnDTO.getName()))
                    .map(columnDTO ->
                            "\t\t\t" +  columnDTO.getName() + " = "
                                    + queryAlias + "." + columnDTO.getName())
                    .collect(Collectors.joining(",\n"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public static String extractMergeInsertValues(Map<String,Object> dataSetMap, String queryAlias) {
        if (dataSetMap == null)
            throw new IllegalArgumentException("Attribute 'dataSetDTO' can not be null");
        try {
            DataSetDTO dataSetDTO =
                    ObjectMapping.stringToObject(ObjectMapping.asJsonString(dataSetMap),DataSetDTO.class);

            return dataSetDTO
                    .getColumnSchema()
                    .stream()
                    .map(columnDTO -> queryAlias + "." + columnDTO.getName())
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public static String extractColumnsCS(Map<String,Object> dataSetMap) {
        if (dataSetMap == null)
            throw new IllegalArgumentException("Attribute 'dataSetDTO' can not be null");
        try {
            DataSetDTO dataSetDTO =
                    ObjectMapping.stringToObject(ObjectMapping.asJsonString(dataSetMap),DataSetDTO.class);

            return dataSetDTO
                    .getColumnSchema()
                    .stream()
                    .map(NameDescriptionAbstract::getName)
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
