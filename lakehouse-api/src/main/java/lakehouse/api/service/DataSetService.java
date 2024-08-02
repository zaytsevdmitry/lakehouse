package lakehouse.api.service;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.ColumnDTO;
import lakehouse.api.dto.DataSetDTO;
import lakehouse.api.dto.DataSetSourceDTO;
import lakehouse.api.entities.*;
import lakehouse.api.exception.DataSetNotFoundException;
import lakehouse.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataSetService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSetRepository dataSetRepository;
    private final DataSetPropertyRepository dataSetPropertyRepository;
    private final DataSetSourceRepository dataSetSourceRepository;
    private final DataSetSourcePropertyRepository dataSetSourcePropertyRepository;
    private final DataSetColumnRepository dataSetColumnRepository;
    private final ProjectRepository projectRepository;
    private final DataStoreRepository dataStoreRepository;

    public DataSetService(
            DataSetRepository dataSetRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            DataSetSourceRepository dataSetSourceRepository,
            DataSetSourcePropertyRepository dataSetSourcePropertyRepository,
            DataSetColumnRepository dataSetColumnRepository,
            ProjectRepository projectRepository,
            DataStoreRepository dataStoreRepository) {
        this.dataSetRepository = dataSetRepository;
        this.dataSetPropertyRepository = dataSetPropertyRepository;
        this.dataSetSourceRepository = dataSetSourceRepository;
        this.dataSetSourcePropertyRepository = dataSetSourcePropertyRepository;
        this.dataSetColumnRepository = dataSetColumnRepository;
        this.projectRepository = projectRepository;
        this.dataStoreRepository = dataStoreRepository;
    }

    private DataSetDTO mapDataSetToDTO(DataSet dataSet) {
        DataSetDTO result = new DataSetDTO();
        result.setName(dataSet.getName());
        result.setDescription(dataSet.getDescription());
        result.setDataStore(dataSet.getDataStore().getName());
        result.setProject(dataSet.getProject().getName());

        result.setSources(
                dataSetSourceRepository.findByDataSetName(dataSet.getName()).stream().map(dataSetSource -> {
                    DataSetSourceDTO dataSetSourceDTO = new DataSetSourceDTO();
                    dataSetSourceDTO.setName(dataSetSource.getSource().getName());
                    Map<String, String> props = new HashMap<>();
                    dataSetSourcePropertyRepository
                            .findBySourceId(dataSetSource.getId())
                            .forEach(dataSetSourceProperty -> props.put(
                                    dataSetSourceProperty.getName(),
                                    dataSetSourceProperty.getValue()));
                    dataSetSourceDTO.setProperties(props);
                    return dataSetSourceDTO;
                }).toList());

        Map<String, String> properties = new HashMap<>();
        dataSetPropertyRepository
                .findByDataSetName(dataSet.getName())
                .forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(), dataStoreProperty.getValue()));
        result.setProperties(properties);
        result.setColumnSchema(
                dataSetColumnRepository.findBydataSetName(dataSet.getName()).stream().map(dataSetColumn -> {
                    ColumnDTO columnDTO = new ColumnDTO();
                    columnDTO.setName(dataSetColumn.getName());
                    columnDTO.setDataType(dataSetColumn.getDataType());
                    columnDTO.setNullable(dataSetColumn.isNullable());
                    columnDTO.setDescription(dataSetColumn.getComment());
                    return columnDTO;
                }).toList()
        );
        return result;
    }

    private DataSet mapDataSetToEntity(DataSetDTO dataSetDTO) {
        DataSet result = new DataSet();
        result.setName(dataSetDTO.getName());
        result.setDescription(dataSetDTO.getDescription());
        result.setProject(projectRepository.getReferenceById(dataSetDTO.getProject()));
        result.setDataStore(dataStoreRepository.getReferenceById(dataSetDTO.getDataStore()));
        return result;
    }

    private List<DataSetColumn> mapColumnEntities(DataSetDTO dataSetDTO) {
        DataSet dataSet = mapDataSetToEntity(dataSetDTO);
        return dataSetDTO.getColumnSchema().stream().map(columnDTO -> {
            DataSetColumn column = new DataSetColumn();
            column.setDataSet(dataSet);
            column.setNullable(columnDTO.isNullable());
            column.setDataType(columnDTO.getDataType());
            column.setName(columnDTO.getName());
            column.setComment(columnDTO.getDescription());
            return column;
        }).toList();
    }

    private List<DataSetProperty> mapPropertyEntities(DataSetDTO dataSetDTO) {
        DataSet dataSet = mapDataSetToEntity(dataSetDTO);
        return dataSetDTO.getProperties().entrySet().stream().map(stringStringEntry -> {
            DataSetProperty dataSetProperty = new DataSetProperty();
            dataSetProperty.setDataSet(dataSet);
            dataSetProperty.setKey(stringStringEntry.getKey());
            dataSetProperty.setValue(stringStringEntry.getValue());
            return dataSetProperty;
        }).toList();

    }

    private DataSet findByName(String name) {
        return dataSetRepository
                .findById(name)
                .orElseThrow(() -> {
                    logger.info("Can't get data set name: %s", name);
                    return new DataSetNotFoundException(name);
                });
    }

    public List<DataSetDTO> findAll() {
        return dataSetRepository.findAll().stream().map(this::mapDataSetToDTO).toList();
    }
    @Transactional
    private DataSet saveDataSet(DataSetDTO dataSetDTO) {
        Optional<DataSet> currentDataSet = dataSetRepository.findById(dataSetDTO.getName());
        if (currentDataSet.isPresent()) {
            // need merge
            DataSetDTO currentDataSetDTO = mapDataSetToDTO(currentDataSet.get());

            if (currentDataSetDTO.equals(dataSetDTO)) {
                return currentDataSet.get();
            } else {
                dataSetColumnRepository.findBydataSetName(dataSetDTO.getName()).forEach(dataSetColumnRepository::delete);
                dataSetPropertyRepository.findByDataSetName(dataSetDTO.getName()).forEach(dataSetPropertyRepository::delete);
                dataSetSourceRepository.findByDataSetName(dataSetDTO.getName()).forEach(dataSetSourceRepository::delete);
            }
        }

        DataSet dataSet = dataSetRepository.save(mapDataSetToEntity(dataSetDTO));

        mapColumnEntities(dataSetDTO).forEach(dataSetColumnRepository::save);
        mapPropertyEntities(dataSetDTO).forEach(dataSetPropertyRepository::save);

        dataSetDTO.getSources().forEach(dataSetSourceDTO -> {
            DataSetSource dataSetSource = new DataSetSource();
            dataSetSource.setSource(
                    dataSetRepository
                            .findById(dataSetSourceDTO.getName())
                            .orElseThrow(() -> new RuntimeException(
                                    String.format("DataSet %s not found", dataSetSourceDTO.getName()))));

            dataSetSource.setDataSet(dataSet);
            dataSetSourceDTO.getProperties().entrySet().forEach(stringStringEntry -> {
                DataSetSourceProperty dataSetSourceProperty = new DataSetSourceProperty();
                dataSetSourceProperty.setDataSetSource(dataSetSourceRepository.save(dataSetSource));
                dataSetSourceProperty.setName(stringStringEntry.getKey());
                dataSetSourceProperty.setValue(stringStringEntry.getValue());
                dataSetSourcePropertyRepository.save(dataSetSourceProperty);
            });
        });
        return dataSet;
    }

    public DataSetDTO save(DataSetDTO dataSetDTO) {
        DataSet dataSet = saveDataSet(dataSetDTO);
        return mapDataSetToDTO(dataSet);
    }

    public DataSetDTO findById(String name) {
        return mapDataSetToDTO(findByName(name));
    }

    @Transactional
    public void deleteById(String name) {
        dataSetRepository.deleteById(name);
    }
}
