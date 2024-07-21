package lakehouse.api.controller;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.DataSetDTO;
import lakehouse.api.entities.DataSet;
import lakehouse.api.entities.DataSetColumn;
import lakehouse.api.entities.DataSetProperty;
import lakehouse.api.repository.DataSetColumnRepository;
import lakehouse.api.repository.DataSetPropertyRepository;
import lakehouse.api.repository.DataSetRepository;
import lakehouse.api.repository.DataStoreRepository;
import lakehouse.api.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DataSetController {
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());
    private final DataSetRepository dataSetRepository;
    private final DataSetPropertyRepository dataSetPropertyRepository;
    private final DataSetColumnRepository dataSetColumnRepository;
    private final ProjectRepository projectRepository;
    private final DataStoreRepository dataStoreRepository;

    public DataSetController(
            DataSetRepository dataSetRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            DataSetColumnRepository dataSetColumnRepository,
            ProjectRepository projectRepository,
            DataStoreRepository dataStoreRepository) {
        this.dataSetRepository = dataSetRepository;
        this.dataSetPropertyRepository = dataSetPropertyRepository;
        this.dataSetColumnRepository = dataSetColumnRepository;
        this.projectRepository = projectRepository;
        this.dataStoreRepository = dataStoreRepository;
    }

    private DataSetDTO mapDataSetToDTO(DataSet dataSet){
        DataSetDTO result = new DataSetDTO();
        result.setKey(dataSet.getKey());
        result.setComment(dataSet.getComment());
        result.setDataStore(dataSet.getDataStore().getKey());
        result.setProject(dataSet.getProject().getKey());
        result.setSources(dataSet.getSources());
        Map<String,String> properties = new HashMap<>();
        dataSetPropertyRepository
                .findByDataSet(dataSet)
                .forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(),dataStoreProperty.getValue()));
        result.setProperties(properties);
        return result;
    }

    private DataSet mapDataSetToEntity(DataSetDTO dataSetDTO) {
        DataSet result = new DataSet();
        result.setKey(dataSetDTO.getKey());
        result.setComment(dataSetDTO.getComment());
        result.setProject( projectRepository.getReferenceById( dataSetDTO.getProject()));
        result.setDataStore(dataStoreRepository.getReferenceById(dataSetDTO.getDataStore()));
        result.setSources(dataSetDTO.getSources());
        return result;
    }

    private List<DataSetColumn> getColumnEntities(DataSetDTO dataSetDTO){
        DataSet dataSet = mapDataSetToEntity(dataSetDTO);
        return  dataSetDTO.getColumnSchema().stream().map(columnDTO -> {

            DataSetColumn column = new DataSetColumn();
            column.setDataSet(dataSet);
            column.setNullable(columnDTO.isNullable());
            column.setDataType(columnDTO.getDataType());
            column.setName(columnDTO.getName());
            column.setComment(columnDTO.getComment());
            return column;
        }).toList();
    }

    private List<DataSetProperty> getPropertyEntities(DataSetDTO dataSetDTO){
        DataSet dataSet = mapDataSetToEntity(dataSetDTO);
        return dataSetDTO.getProperties().entrySet().stream().map(stringStringEntry -> {
            DataSetProperty dataSetProperty = new DataSetProperty();
            dataSetProperty.setDataSet(dataSet);
            dataSetProperty.setKey(stringStringEntry.getKey());
            dataSetProperty.setValue(stringStringEntry.getValue());
            return dataSetProperty;
        }).toList();

    }

    private lakehouse.api.entities.DataSet findByKey(String key){
        return dataSetRepository
                .findById(key)
                .orElseThrow(()-> {
                    logger.info("Can't get key: %s", key);
                    return new RuntimeException("Can't get key");});
    }

    @GetMapping("/datasets")
    List<DataSetDTO> all() {
        return dataSetRepository.findAll().stream().map(this::mapDataSetToDTO).toList();
    }

    @PostMapping("/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DataSetDTO newDataSet(@RequestBody DataSetDTO dataSetDTO ) {

        getColumnEntities(dataSetDTO).forEach(dataSetColumnRepository::save);
        getPropertyEntities(dataSetDTO).forEach(dataSetPropertyRepository::save);

        return mapDataSetToDTO(dataSetRepository.save(mapDataSetToEntity(dataSetDTO)));
    }


    @GetMapping("/datasets/{key}")
    DataSetDTO one(@PathVariable String key) {
        return mapDataSetToDTO( findByKey(key));
    }


    @DeleteMapping("/datasets/{key}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    void deleteDataSet(@PathVariable String key) {
        /*if (dataStoreRepository.findById(key).isPresent())
            dataStorePropertyRepository
                    .findByDataSet(findByKey(key))
                    .forEach(dataStoreProperty -> dataStorePropertyRepository
                            .deleteById(dataStoreProperty.getId()));*/
        dataSetRepository.deleteById(key);
    }
}
