package org.lakehouse.config.service.dataset;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetProperty;
import org.lakehouse.config.entities.dataset.DataSetScript;
import org.lakehouse.config.exception.DataSetNotFoundException;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.NameSpaceRepository;
import org.lakehouse.config.repository.dataset.DataSetPropertyRepository;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetScriptRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
import org.lakehouse.config.service.ScriptService;
import org.lakehouse.config.service.dataset.source.DataSetSourceService;
import org.lakehouse.config.service.datasource.SQLTemplateService;
import org.lakehouse.config.specifier.DataSetPropertyKeyValueEntitySpecifier;
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
    private final DataSetSourceService dataSetSourceService;
    private final NameSpaceRepository nameSpaceRepository;
    private final DataSourceRepository dataSourceRepository;
    private final DataSetScriptRepository dataSetScriptRepository;
    private final ScriptService scriptService;
    private final DataSetScriptService dataSetScriptService;
    private final DataSetColumnService dataSetColumnService;
    private final DataSetConstraintService dataSetConstraintService;
    private final SQLTemplateService sqlTemplateService;
    public DataSetService(
            DataSetRepository dataSetRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            DataSetSourceService dataSetSourceService,
            NameSpaceRepository nameSpaceRepository,
            DataSourceRepository dataSourceRepository,
            DataSetScriptRepository dataSetScriptRepository,
            ScriptService scriptService,
            DataSetScriptService dataSetScriptService,
            DataSetColumnService dataSetColumnService, DataSetConstraintService dataSetConstraintService, SQLTemplateService sqlTemplateService) {
        this.dataSetRepository = dataSetRepository;
        this.dataSetPropertyRepository = dataSetPropertyRepository;
        this.dataSetSourceService = dataSetSourceService;
        this.nameSpaceRepository = nameSpaceRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSetScriptRepository = dataSetScriptRepository;
        this.scriptService = scriptService;
        this.dataSetScriptService = dataSetScriptService;
        this.dataSetColumnService = dataSetColumnService;
        this.dataSetConstraintService = dataSetConstraintService;
        this.sqlTemplateService = sqlTemplateService;
    }

    private DataSetDTO mapDataSetToDTO(DataSet dataSet) {
        DataSetDTO result = new DataSetDTO();
        result.setKeyName(dataSet.getKeyName());
        result.setDescription(dataSet.getDescription());
        result.setDataSourceKeyName(dataSet.getDataSource().getKeyName());
        result.setNameSpaceKeyName(dataSet.getNameSpace().getKeyName());
        result.setDatabaseSchemaName(dataSet.getDatabaseSchemaName());
        result.setTableName(dataSet.getTableName());
        result.setScripts(dataSetScriptService.findDataSetScriptDTOListByDataSetName(dataSet.getKeyName()));
        result.setConstraints(dataSetConstraintService.mapDataSetConstraintsToDTOList(dataSet.getKeyName()));
        result.setSources(dataSetSourceService.getDataSetSourceDTOsByDataSetKeyName(dataSet.getKeyName()));
        Map<String, String> properties = new HashMap<>();
        dataSetPropertyRepository.findByDataSetKeyName(dataSet.getKeyName())
                .forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(), dataStoreProperty.getValue()));
        result.setProperties(properties);
        logger.info("");
        result.setColumnSchema(dataSetColumnService.mapColumnDTOList( dataSetColumnService.getDataSetColumns(dataSet.getKeyName())));
        result.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(dataSet));
        return result;
    }

    private DataSet mapDataSetToEntity(DataSetDTO dataSetDTO) {
        DataSet result = new DataSet();
        result.setKeyName(dataSetDTO.getKeyName());
        result.setDescription(dataSetDTO.getDescription());
        result.setNameSpace(nameSpaceRepository.getReferenceById(dataSetDTO.getNameSpaceKeyName()));
        result.setDataSource(dataSourceRepository.getReferenceById(dataSetDTO.getDataSourceKeyName()));
        result.setDatabaseSchemaName(dataSetDTO.getDatabaseSchemaName());
        result.setTableName(dataSetDTO.getTableName());
        return result;
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
        return dataSetRepository.findById(name).orElseThrow(() -> {
            logger.info("Can't get data set name: {}", name);
            return new DataSetNotFoundException(name);
        });
    }

    public List<DataSetDTO> findAll() {
        return dataSetRepository.findAll().stream().map(this::mapDataSetToDTO).toList();
    }

    private void saveProperties(DataSet dataSet, Map<String,String> properties){
        new KeyValueEntityMerger(
                new DataSetPropertyKeyValueEntitySpecifier(dataSetPropertyRepository,dataSet))
                .mergeAbstractKeyValues(
                        dataSetPropertyRepository.findByDataSetKeyName(dataSet.getKeyName())
                                .stream()
                                .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                                .toList(),
                        properties);
    }
    @Transactional
    private DataSetDTO saveDataSet(DataSetDTO dataSetDTO) {

        logger.info("Saving dataSetDTO={}: cleanUp", dataSetDTO.getKeyName());


        dataSetScriptService.findDataSetScriptListByDataSetName(dataSetDTO.getKeyName())
                .forEach(dataSetScriptRepository::delete);

        logger.info("Saving dataSetDTO={}", dataSetDTO.getKeyName());
        DataSet dataSet = dataSetRepository.save(mapDataSetToEntity(dataSetDTO));

        logger.info("Saving dataSetDTO={} columns", dataSetDTO.getKeyName());
        dataSetColumnService.applyColumns(dataSet,dataSetDTO.getColumnSchema());

        logger.info("Saving dataSetDTO={} properties", dataSetDTO.getKeyName());
        saveProperties(dataSet,dataSetDTO.getProperties());

        dataSetSourceService.save(dataSet, dataSetDTO.getSources());
        logger.info("Saving dataSetDTO={} scripts", dataSetDTO.getKeyName());
        dataSetDTO.getScripts().stream().map(dataSetScriptDTO -> {
            DataSetScript dataSetScript = new DataSetScript();
            dataSetScript.setDataSet(dataSet);
            dataSetScript.setScript(scriptService.findScriptByKey(dataSetScriptDTO.getKey()));
            dataSetScript.setScriptOrder(dataSetScriptDTO.getOrder());
            return dataSetScript;
        }).forEach(dataSetScriptRepository::save);

        logger.info("Saving dataSetDTO={} constraints", dataSetDTO.getKeyName());
        dataSetConstraintService.applyConstraints(dataSet,dataSetDTO.getConstraints());

        sqlTemplateService.save(dataSet,dataSetDTO.getSqlTemplate());

        logger.info("Saving dataSetDTO={} reload", dataSetDTO.getKeyName());
        return mapDataSetToDTO(dataSet);
    }

    public DataSetDTO save(DataSetDTO dataSetDTO) {
        Optional<DataSet> oldDataSet = dataSetRepository.findById(dataSetDTO.getKeyName());
        if (oldDataSet.isPresent()){
            DataSetDTO old = mapDataSetToDTO(oldDataSet.get());
            if (dataSetDTO.equals(old))
            {
                return dataSetDTO;
            }
        }
        return saveDataSet(dataSetDTO);
    }

    public DataSetDTO findById(String name) {
        return mapDataSetToDTO(findByName(name));
    }

    @Transactional
    public void deleteById(String name) {
        dataSetRepository.deleteById(name);
    }
}
