package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import org.lakehouse.config.entities.DataStore;
import org.lakehouse.config.entities.DataStoreProperty;
import org.lakehouse.config.exception.DataStoreNotFoundException;
import org.lakehouse.config.repository.DataStorePropertyRepository;
import org.lakehouse.config.repository.DataStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DataStoreService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final DataStoreRepository dataStoreRepository;
	private final DataStorePropertyRepository dataStorePropertyRepository;

	public DataStoreService(DataStoreRepository dataStoreRepository,
			DataStorePropertyRepository dataStorePropertyRepository) {
		this.dataStoreRepository = dataStoreRepository;
		this.dataStorePropertyRepository = dataStorePropertyRepository;
	}

	private DataStoreDTO mapDataStoreToDTO(DataStore dataStore) {
		DataStoreDTO result = new DataStoreDTO();
		result.setName(dataStore.getName());
		result.setDescription(dataStore.getDescription());
		result.setInterfaceType(dataStore.getInterfaceType());
		result.setVendor(dataStore.getVendor());
		result.setUrl(dataStore.getUrl());
		Map<String, String> properties = new HashMap<>();
		dataStorePropertyRepository.findByDataStoreName(dataStore.getName())
				.forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(), dataStoreProperty.getValue()));
		result.setProperties(properties);
		return result;
	}

	private DataStore mapDataStoreToEntity(DataStoreDTO dataStore) {
		DataStore result = new DataStore();
		result.setName(dataStore.getName());
		result.setDescription(dataStore.getDescription());
		result.setVendor(dataStore.getVendor());
		result.setInterfaceType(dataStore.getInterfaceType());
		result.setUrl(dataStore.getUrl());
		return result;
	}

	public List<DataStoreDTO> findAll() {
		return dataStoreRepository.findAll().stream().map(this::mapDataStoreToDTO).toList();
	}

	@Transactional
	public DataStoreDTO save(DataStoreDTO dataStoreDTO) {
		DataStore dataStore = dataStoreRepository.save(mapDataStoreToEntity(dataStoreDTO));
		List<DataStoreProperty> dataStorePropertiesBeforeChange = dataStorePropertyRepository
				.findByDataStoreName(dataStore.getName());

		dataStorePropertiesBeforeChange.forEach(dataStoreProperty -> {
			if (!dataStoreDTO.getProperties().containsKey(dataStoreProperty.getKey())) {
				dataStorePropertyRepository.delete(dataStoreProperty);
			}
		});
		dataStoreDTO.getProperties().entrySet().stream().map(stringStringEntry -> {
			Optional<DataStoreProperty> optionalDataStoreProperty = dataStorePropertyRepository
					.findByKeyAndDataStoreName(stringStringEntry.getKey(), dataStoreDTO.getName());
			if (optionalDataStoreProperty.isPresent()) {
				if (!optionalDataStoreProperty.get().getValue().equals(stringStringEntry.getValue())) {
					return optionalDataStoreProperty.get(); // dataStorePropertyRepository.save(optionalDataStoreProperty.get());
				} else {
					DataStoreProperty dataStoreProperty = optionalDataStoreProperty.get();
					dataStoreProperty.setValue(stringStringEntry.getValue());
					return dataStoreProperty;
				}
			} else {
				DataStoreProperty dataStoreProperty = new DataStoreProperty();
				dataStoreProperty.setDataStore(dataStore);
				dataStoreProperty.setKey(stringStringEntry.getKey());
				dataStoreProperty.setValue(stringStringEntry.getValue());
				return dataStoreProperty;
			}
		}).toList().forEach(dataStorePropertyRepository::save);
		return mapDataStoreToDTO(dataStore);
	}

	public DataStoreDTO findById(String name) {
		return mapDataStoreToDTO(
				dataStoreRepository.findById(name).orElseThrow(() -> new DataStoreNotFoundException(name)));
	}

	@Transactional
	public void deleteById(String name) {
		dataStoreRepository.deleteById(name);
	}
}
