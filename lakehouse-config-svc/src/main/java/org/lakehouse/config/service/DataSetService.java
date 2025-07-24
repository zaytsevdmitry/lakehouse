package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.ColumnDTO;
import org.lakehouse.client.api.dto.configs.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataSetSourceDTO;
import org.lakehouse.config.entities.*;
import org.lakehouse.config.exception.DataSetNotFoundException;
import org.lakehouse.config.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final DataSetConstraintRepository dataSetConstraintRepository;
	private final DataSetScriptRepository dataSetScriptRepository;
	private final ScriptService scriptService;
	private final DataSetScriptService dataSetScriptService;

	public DataSetService(
            DataSetRepository dataSetRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            DataSetSourceRepository dataSetSourceRepository,
            DataSetSourcePropertyRepository dataSetSourcePropertyRepository,
            DataSetColumnRepository dataSetColumnRepository,
            ProjectRepository projectRepository,
            DataStoreRepository dataStoreRepository,
            ScriptRepository scriptRepository, DataSetConstraintRepository dataSetConstraintRepository, DataSetScriptRepository dataSetScriptRepository, ScriptService scriptService, DataSetScriptService dataSetScriptService) {
		this.dataSetRepository = dataSetRepository;
		this.dataSetPropertyRepository = dataSetPropertyRepository;
		this.dataSetSourceRepository = dataSetSourceRepository;
		this.dataSetSourcePropertyRepository = dataSetSourcePropertyRepository;
		this.dataSetColumnRepository = dataSetColumnRepository;
		this.projectRepository = projectRepository;
		this.dataStoreRepository = dataStoreRepository;
        this.dataSetConstraintRepository = dataSetConstraintRepository;
        this.dataSetScriptRepository = dataSetScriptRepository;
        this.scriptService = scriptService;
        this.dataSetScriptService = dataSetScriptService;
    }

	private DataSetDTO mapDataSetToDTO(DataSet dataSet) {
		DataSetDTO result = new DataSetDTO();
		result.setKeyName(dataSet.getKeyName());
		result.setDescription(dataSet.getDescription());
		result.setDataStoreKeyName(dataSet.getDataStore().getKeyName());
		result.setProject(dataSet.getProject().getKeyName());
		result.setFullTableName(dataSet.getFullTableName());

		result.setScripts(dataSetScriptService.findDataSetScriptDTOListByDataSetName(dataSet.getKeyName()));
		result.setConstraints(dataSetConstraintRepository.findByDataSetKeyName(dataSet.getKeyName()).stream().map(dataSetConstraint -> {
			DataSetConstraintDTO dataSetConstraintDTO = new DataSetConstraintDTO();
			dataSetConstraintDTO.setColumns(dataSetConstraint.getColumns());
			dataSetConstraintDTO.setType(dataSetConstraint.getType());
			dataSetConstraintDTO.setName(dataSetConstraint.getName());
			dataSetConstraintDTO.setEnabled(dataSetConstraint.isEnabled());
			dataSetConstraintDTO.setRuntimeLevelCheck(dataSetConstraint.isRuntimeLevelCheck());
			dataSetConstraintDTO.setConstructLevelCheck(dataSetConstraint.isConstructLevelCheck());
			return dataSetConstraintDTO;
		}).toList());
		result.setSources(dataSetSourceRepository.findByDataSetKeyName(dataSet.getKeyName()).stream().map(dataSetSource -> {
			DataSetSourceDTO dataSetSourceDTO = new DataSetSourceDTO();
			dataSetSourceDTO.setName(dataSetSource.getSource().getKeyName());
			Map<String, String> props = new HashMap<>();
			dataSetSourcePropertyRepository.findBySourceId(dataSetSource.getId()).forEach(dataSetSourceProperty -> props
					.put(dataSetSourceProperty.getKey(), dataSetSourceProperty.getValue()));
			dataSetSourceDTO.setProperties(props);
			return dataSetSourceDTO;
		}).toList());

		Map<String, String> properties = new HashMap<>();
		dataSetPropertyRepository.findByDataSetKeyName(dataSet.getKeyName())
				.forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(), dataStoreProperty.getValue()));
		result.setProperties(properties);
		logger.info("");
		result.setColumnSchema(
				dataSetColumnRepository.findBydataSetName(dataSet.getKeyName()).stream().map(dataSetColumn -> {
					ColumnDTO columnDTO = new ColumnDTO();
					columnDTO.setName(dataSetColumn.getName());
					columnDTO.setDataType(dataSetColumn.getDataType());
					columnDTO.setNullable(dataSetColumn.isNullable());
					columnDTO.setDescription(dataSetColumn.getComment());
					columnDTO.setOrder(dataSetColumn.getColumnOrder());
					return columnDTO;
				}).toList());
		return result;
	}

	private DataSet mapDataSetToEntity(DataSetDTO dataSetDTO) {
		DataSet result = new DataSet();
		result.setKeyName(dataSetDTO.getKeyName());
		result.setDescription(dataSetDTO.getDescription());
		result.setProject(projectRepository.getReferenceById(dataSetDTO.getProject()));
		result.setDataStore(dataStoreRepository.getReferenceById(dataSetDTO.getDataStoreKeyName()));
		result.setFullTableName(dataSetDTO.getFullTableName());
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
			column.setColumnOrder(columnDTO.getOrder());
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
		return dataSetRepository.findById(name).orElseThrow(() -> {
			logger.info("Can't get data set name: {}", name);
			return new DataSetNotFoundException(name);
		});
	}

	public List<DataSetDTO> findAll() {
		return dataSetRepository.findAll().stream().map(this::mapDataSetToDTO).toList();
	}

	@Transactional
	private DataSetDTO saveDataSet(DataSetDTO dataSetDTO) {
		logger.info("Saving dataSetDTO={}: cleanUp",dataSetDTO.getKeyName());

		dataSetColumnRepository.findBydataSetName(dataSetDTO.getKeyName())
				.forEach(dataSetColumnRepository::delete);
		dataSetPropertyRepository.findByDataSetKeyName(dataSetDTO.getKeyName())
				.forEach(dataSetPropertyRepository::delete);
		dataSetSourceRepository.findByDataSetKeyName(dataSetDTO.getKeyName())
				.forEach(dataSetSourceRepository::delete);
		dataSetScriptService.findDataSetScriptListByDataSetName(dataSetDTO.getKeyName())
				.forEach(dataSetScriptRepository::delete);
		dataSetConstraintRepository.findByDataSetKeyName(dataSetDTO.getKeyName())
				.forEach(dataSetConstraintRepository::delete);

		logger.info("Saving dataSetDTO={}",dataSetDTO.getKeyName());
		DataSet dataSet = dataSetRepository.save(mapDataSetToEntity(dataSetDTO));

		logger.info("Saving dataSetDTO={} columns",dataSetDTO.getKeyName());
		mapColumnEntities(dataSetDTO).forEach(dataSetColumnRepository::save);

		logger.info("Saving dataSetDTO={} properties",dataSetDTO.getKeyName());
		mapPropertyEntities(dataSetDTO).forEach(dataSetPropertyRepository::save);

		logger.info("Saving dataSetDTO={} sources",dataSetDTO.getKeyName());
		dataSetDTO.getSources().forEach(dataSetSourceDTO -> {
			DataSetSource dataSetSource = new DataSetSource();
			dataSetSource.setSource(dataSetRepository.findById(dataSetSourceDTO.getName()).orElseThrow(
					() -> new RuntimeException(String.format("DataSet %s not found", dataSetSourceDTO.getName()))));

			dataSetSource.setDataSet(dataSet);
			DataSetSource resultDataSetSource = dataSetSourceRepository.save(dataSetSource);
			dataSetSourceDTO.getProperties().entrySet().forEach(stringStringEntry -> {
				DataSetSourceProperty dataSetSourceProperty = new DataSetSourceProperty();
				dataSetSourceProperty.setDataSetSource(resultDataSetSource);
				dataSetSourceProperty.setKey(stringStringEntry.getKey());
				dataSetSourceProperty.setValue(stringStringEntry.getValue());
				dataSetSourcePropertyRepository.save(dataSetSourceProperty);
			});
		});

		logger.info("Saving dataSetDTO={} scripts",dataSetDTO.getKeyName());
		dataSetDTO.getScripts().stream().map(dataSetScriptDTO -> {
			DataSetScript dataSetScript = new DataSetScript();
			dataSetScript.setDataSet(dataSet);
			dataSetScript.setScript(scriptService.findScriptByKey(dataSetScriptDTO.getKey()));
			dataSetScript.setScriptOrder(dataSetScriptDTO.getOrder());
			return dataSetScript;
		}).forEach(dataSetScriptRepository::save);

		logger.info("Saving dataSetDTO={} constraints",dataSetDTO.getKeyName());
		dataSetDTO.getConstraints().stream().map(dataSetConstraintDTO -> {
			DataSetConstraint dataSetConstraint = new DataSetConstraint();
			dataSetConstraint.setDataSet(dataSet);
			dataSetConstraint.setColumns(dataSetConstraintDTO.getColumns());
			dataSetConstraint.setType(dataSetConstraintDTO.getType());
			dataSetConstraint.setName(dataSetConstraintDTO.getName());
			dataSetConstraint.setEnabled(dataSetConstraintDTO.isEnabled());
			dataSetConstraint.setConstructLevelCheck(dataSetConstraintDTO.isConstructLevelCheck());
			dataSetConstraint.setRuntimeLevelCheck(dataSetConstraintDTO.isRuntimeLevelCheck());
			return dataSetConstraint;
		}).forEach(dataSetConstraintRepository::save);

		logger.info("Saving dataSetDTO={} reload",dataSetDTO.getKeyName());
		return mapDataSetToDTO(dataSet);
	}

	public DataSetDTO save(DataSetDTO dataSetDTO) {
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
