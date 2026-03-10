package org.lakehouse.config.service.dataset;

import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetColumn;
import org.lakehouse.config.repository.dataset.DataSetColumnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSetColumnService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSetColumnRepository dataSetColumnRepository;

    public DataSetColumnService(DataSetColumnRepository dataSetColumnRepository) {
        this.dataSetColumnRepository = dataSetColumnRepository;
    }
    public void applyColumns(DataSet dataSet, List<ColumnDTO> columnDTOList) {

        List<DataSetColumn> currentColumns = dataSetColumnRepository.findByDataSetName(dataSet.getKeyName());

        List<DataSetColumn> newColumns = mapColumnEntities(dataSet, columnDTOList).stream().map(dataSetColumn -> {
            List<DataSetColumn> found = currentColumns.stream().filter(c-> c.getName().equals(dataSetColumn.getName())).toList();
            if (!found.isEmpty()){
                dataSetColumn.setId(found.get(0).getId());
                logger.info("Column {} will be updated. metricId is {}", dataSetColumn.getName(), dataSetColumn.getId());
            }else {
                logger.info("Column {} will be added. metricId is null", dataSetColumn.getName());
            }
            return dataSetColumn;
        }).toList();

        for (DataSetColumn dataSetColumn:currentColumns){
            List<DataSetColumn> found = newColumns.stream().filter(c-> c.getName().equals(dataSetColumn.getName())).toList();
            if (found.isEmpty()){
                logger.info("Column {} will be deleted. metricId is {}", dataSetColumn.getName(), dataSetColumn.getId());
                dataSetColumnRepository.delete(dataSetColumn);
            }
        }
        dataSetColumnRepository.saveAll(newColumns);
    }

    public List<ColumnDTO> mapColumnDTOList(List<DataSetColumn> dataSetColumns){
        return dataSetColumns.stream().map(dataSetColumn -> {
            ColumnDTO columnDTO = new ColumnDTO();
            columnDTO.setName(dataSetColumn.getName());
            columnDTO.setDataType(dataSetColumn.getDataType());
            columnDTO.setNullable(dataSetColumn.isNullable());
            columnDTO.setDescription(dataSetColumn.getComment());
            columnDTO.setOrder(dataSetColumn.getColumnOrder());
            return columnDTO;
        }).toList();
    }

    public List<DataSetColumn> getDataSetColumns(String dataSetKeyName){
        return dataSetColumnRepository.findByDataSetName(dataSetKeyName);
    }

    private List<DataSetColumn> mapColumnEntities(DataSet dataSet, List<ColumnDTO> columnDTOList) {

        return columnDTOList.stream().map(columnDTO -> {
            DataSetColumn column = new DataSetColumn();
            column.setDataSet(dataSet);
            column.setNullable(columnDTO.isNullable());
            column.setDataType(columnDTO.getDataType());
            column.setName(columnDTO.getName());
            column.setComment(columnDTO.getDescription());
            column.setColumnOrder(columnDTO.getOrder());
            return column;
        }).toList();
    }
}
