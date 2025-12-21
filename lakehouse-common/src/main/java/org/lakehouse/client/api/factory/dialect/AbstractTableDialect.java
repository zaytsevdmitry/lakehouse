package org.lakehouse.client.api.factory.dialect;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.exception.DDLDIalectConstraintException;
import org.lakehouse.client.api.utils.Coalesce;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractTableDialect implements TableDialect , SplitConstraint{
    private final DataSetDTO dataSetDTO;
    private final Map<String, DataSetDTO> foreignDataSetDTOMap;
    private final DataSetConstraintDTO primaryKeyConstraintDTO;
    private final List<DataSetConstraintDTO>  foreignKeyConstraintDTOs;
    private final List<DataSetConstraintDTO>  uniqueKeyConstraintDTOs;
    private final List<DataSetConstraintDTO> checkKeyConstraintDTOs;

    public AbstractTableDialect(TableDialectParameter tableDialectParameter) throws DDLDIalectConstraintException {
        this.dataSetDTO = tableDialectParameter.getDataSetDTO();
        this.foreignDataSetDTOMap = tableDialectParameter.getForeignDataSetDTOSet();
        primaryKeyConstraintDTO = fillPk();
        foreignKeyConstraintDTOs = filterDataSetConstraintDTOS(Types.Constraint.foreign);
        uniqueKeyConstraintDTOs = filterDataSetConstraintDTOS(Types.Constraint.unique);
        checkKeyConstraintDTOs = filterDataSetConstraintDTOS(Types.Constraint.check);
    }


    public DataSetDTO getDataSetDTO() {
        return dataSetDTO;
    }

    public Map<String,DataSetDTO> getForeignDataSetDTOMap() {
        return foreignDataSetDTOMap;
    }

    private List<DataSetConstraintDTO> filterDataSetConstraintDTOS(Types.Constraint type){
        return
                getDataSetDTO()
                        .getConstraints()
                        .stream()
                        .filter(dataSetConstraintDTO -> dataSetConstraintDTO.getType().equals(type))
                        .toList();
    }
    private DataSetConstraintDTO fillPk() throws DDLDIalectConstraintException {
        List<DataSetConstraintDTO> contraints = filterDataSetConstraintDTOS(Types.Constraint.primary);
        if (contraints.size() == 1){
            return contraints.get(0);
        }if(contraints.isEmpty()){
            return null;
        }else
            throw new DDLDIalectConstraintException("Table can't more than one primary key");
    }

    @Override
    public DataSetConstraintDTO getPrimaryKeyConstraintDTO() {
        return primaryKeyConstraintDTO;
    }

    @Override
    public List<DataSetConstraintDTO> getForeignKeyConstraintDTOs() {
        return foreignKeyConstraintDTOs;
    }

    @Override
    public List<DataSetConstraintDTO> getUniqueKeyConstraintDTOs() {
        return uniqueKeyConstraintDTOs;
    }

    @Override
    public List<DataSetConstraintDTO> getCheckKeyConstraintDTOs() {
        return checkKeyConstraintDTOs;
    }

    @Override
    public String getColumnsDDL() {
        return getDataSetDTO()
                .getColumnSchema()
                .stream()
                .map(columnDTO -> String.format("%s %s", columnDTO.getName() , columnDTO.getDataType() ))
                .collect(Collectors.joining(", "));
    }

    @Override
    public String getColumnsCastDML() {
        return getDataSetDTO().getColumnSchema().stream()
                .map(col -> String.format("t.%s = q.%s\n", col.getName(),col.getName()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public String getColumnsComaSeparated() {
        return getDataSetDTO()
                .getColumnSchema()
                .stream()
                .map(ColumnDTO::getName)
                .collect(Collectors.joining(", "));
    }
    @Override
    public String getMergeOnDML(){
        //todo aliases of table queryed in merge - t it's target q is a model script
        return Arrays.stream(
                getPrimaryKeyConstraintDTO()
                        .getColumns().split(","))
                        .map(key->
                            String.format("t.".concat(key).concat(" = q.").concat(key)))
                .collect(Collectors.joining(" and "));

    }
@Override
    public String getColumnsUpdateSetDML(){
        //todo aliases of table queryed in merge - t it's target q is a model script
        return getDataSetDTO()
                .getColumnSchema()
                .stream()
                .map(col -> String.format("t.%s = q.%s\n", col.getName(),col.getName()))
                .collect(Collectors.joining(", "));
    }
    @Override
    public String getColumnsMergeInsertValuesDML(){
        //todo aliases of table queryed in merge - t it's target q is a model script
        return getDataSetDTO()
                .getColumnSchema()
                .stream()
                .map(col -> String.format("q.%s", col.getName()))
                .collect(Collectors.joining(", "));
    }

@Override
    public List<String> getAllConstraintsDDLAdd(){
        List<String> result = new ArrayList<>();
        result.add(getPrimaryKeyDDLAdd());
        result.addAll(getForeignKeysDDLAdd());
        result.addAll(getUniqueKeysDDLAdd());
        result.addAll(getCheckKeysDDLAdd());
        return result.stream().filter(s -> !(s==null||s.isBlank())).toList();
    }
    @Override
    public List<String> getAllConstraintsDDLDrop(){
        List<String> result = new ArrayList<>();
        result.add(getPrimaryKeyDDLAdd());
        result.addAll(getForeignKeysDDLDrop());
        result.addAll(getUniqueKeysDDLDrop());
        result.addAll(getCheckKeysDDLDrop());
        return result.stream().filter(s -> !(s==null||s.isBlank())).toList();
    }
    @Override
    public List<String> getAllConstraintsDDL(){
        List<String> result = new ArrayList<>();
        result.add(getPrimaryKeyDDL());
        result.addAll(getForeignKeysDDL());
        result.addAll(getUniqueKeysDDL());
        result.addAll(getCheckKeysDDL());
        return result.stream().filter(s -> !(s==null||s.isBlank())).toList();
    }

    @Override
    public String getPrimaryKeyDDL() {
        if (getPrimaryKeyConstraintDTO() != null) {
            String template = "PRIMARY KEY (%s)";
            return String.format(template, getPrimaryKeyConstraintDTO().getColumns());
        }else return "";
    }

    @Override
    public String getPrimaryKeyDDLAdd() {
        if (getPrimaryKeyConstraintDTO() != null) {
            String template = "ALTER TABLE %s ADD CONSTRAINT %s PRIMARY KEY (%s)";
            return String.format(
                    template,
                    getDataSetDTO().getFullTableName(),
                    getPrimaryKeyConstraintDTO().getName(),
                    getPrimaryKeyConstraintDTO().getColumns());
        }else return "";
    }

    String alterTableAdd = "ALTER TABLE child_table\n ADD %s";
    @Override
    public List<String> getForeignKeysDDL() {
        String template = "CONSTRAINT %s\n" + //fk_name
                "   FOREIGN KEY(%s)\n" + //fk_columns
                "   REFERENCES %s(%s)\n" + // parent_table (parent_key_columns)
                "   %s\n" + //ON DELETE delete_action
                "   %s"; //ON UPDATE update_action
        return getForeignKeyConstraintDTOs()
                .stream()
                .map(dataSetConstraintDTO -> {
                    DataSetDTO foreignDataSet = getForeignDataSetDTOMap().get( dataSetConstraintDTO.getReference().getDataSetKeyName());
                    DataSetConstraintDTO foreignConstraint = foreignDataSet
                            .getConstraints()
                            .stream()
                            .filter(constraint -> Objects.equals(constraint.getName(), dataSetConstraintDTO.getReference().getConstraintName()))
                            .toList()
                            .get(0);

                    String onDelete = "";
                    if (dataSetConstraintDTO.getReference().getOnDelete() != null)
                    {
                        onDelete = "ON DELETE " + dataSetConstraintDTO.getReference().getOnDelete().getValue();
                    }
                    String onUpdate = "";
                    if (dataSetConstraintDTO.getReference().getOnDelete() != null)
                    {
                        onDelete = "ON UPDATE " + dataSetConstraintDTO.getReference().getOnUpdate().getValue();
                    }

                    return String.format(
                                template,
                                dataSetConstraintDTO.getName(),
                                dataSetConstraintDTO.getColumns(),
                                foreignDataSet.getFullTableName(),
                                foreignConstraint.getColumns(),
                                onDelete,
                                onUpdate);

                } )
                .toList();
    }

    @Override
    public List<String> getForeignKeysDDLAdd() {
        return getForeignKeysDDL().stream().map(s -> String.format(alterTableAdd, s)).toList();
    }

    @Override
    public List<String> getUniqueKeysDDL() {
        String template = "CONSTRAINT %s\n" + // name
                "UNIQUE(%s)" ; // columns
        // todo index not supported yet
              //  "%s";  // using index name
        //  String usingIndexTemplate = "USING INDEX %s"; // index name

        return getUniqueKeyConstraintDTOs()
                .stream()
                .map(dataSetConstraintDTO ->
                        String.format(
                                template,
                                dataSetConstraintDTO.getName(),
                                dataSetConstraintDTO.getColumns()))
                .toList();
    }

    @Override
    public List<String> getUniqueKeysDDLAdd() {
        return getUniqueKeysDDL().stream().map(s ->String.format( alterTableAdd, s)).toList();
    }

    private  String dropConstraintDDL(DataSetConstraintDTO dataSetConstraintDTO){
        String template = "ALTER TABLE %s DROP CONSTRAINT %s";
        return String.format(template, getDataSetDTO().getFullTableName(), dataSetConstraintDTO.getName());
    }

    @Override
    public String getPrimaryKeyDDLDrop() {
        return dropConstraintDDL(getPrimaryKeyConstraintDTO());
    }

    @Override
    public List<String> getForeignKeysDDLDrop() {
        return getForeignKeyConstraintDTOs().stream().map(this::dropConstraintDDL).toList();
    }

    @Override
    public List<String> getUniqueKeysDDLDrop() {
        return getUniqueKeyConstraintDTOs().stream().map(this::dropConstraintDDL).toList();
    }

    @Override
    public List<String> getCheckKeysDDLDrop() {
        return getUniqueKeyConstraintDTOs().stream().map(this::dropConstraintDDL).toList();
    }
    @Override
    public List<String> getCheckKeysDDL() {
        return List.of();
    }

    @Override
    public List<String> getCheckKeysDDLAdd() {
        return List.of();
    }

    @Override
    public String getTableDDL(){
        return String.format(
                "CREATE TABLE %s\n (%s, %s)",
                       // "\n %s",
                getDataSetDTO().getFullTableName(),
                getColumnsDDL(),
                // todo storage parameters ?
                String.join(", ", getAllConstraintsDDL())/*,
                properties
                        .entrySet()
                        .stream()
                        .map(sse -> String.format("%s %s",sse.getKey(),sse.getValue()*/
        );
    }

    @Override
    public String getDatabaseSchemaName() {
        String[] tableNameStruct = getDataSetDTO().getFullTableName().split("\\.");

        if (tableNameStruct.length == 2) {
            return tableNameStruct[0];
        }else
            return null;
    }

    @Override
    public String getTableName() {
        String[] tableNameStruct = getDataSetDTO().getFullTableName().split("\\.");

        if (tableNameStruct.length > 0) {
            return tableNameStruct[tableNameStruct.length-1];
        }else
            return null;
    }
}
