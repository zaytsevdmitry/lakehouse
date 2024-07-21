package lakehouse.api.controller;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.DataStoreDTO;
import lakehouse.api.entities.DataStore;
import lakehouse.api.repository.DataStorePropertyRepository;
import lakehouse.api.repository.DataStoreRepository;
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
public class DataStoreController {
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());
    private final DataStoreRepository dataStoreRepository;
    private final DataStorePropertyRepository dataStorePropertyRepository;


    public DataStoreController(
            DataStoreRepository dataStoreRepository,
            DataStorePropertyRepository dataStorePropertyRepository) {
        this.dataStoreRepository = dataStoreRepository;
        this.dataStorePropertyRepository = dataStorePropertyRepository;
    }

    private DataStoreDTO mapDataStoreToDTO(DataStore dataStore){
        DataStoreDTO result = new DataStoreDTO();
        result.setKey(dataStore.getKey());
        result.setComment(dataStore.getComment());
        result.setInterfaceType(dataStore.getInterfaceType());
        result.setVendor(dataStore.getVendor());

        Map<String,String> properties = new HashMap<>();
        dataStorePropertyRepository
                .findByDataStore(dataStore)
                .forEach(dataStoreProperty -> properties.put(dataStoreProperty.getKey(),dataStoreProperty.getValue()));
        result.setProperties(properties);
        return result;
    }

    private lakehouse.api.entities.DataStore mapDataStoreToEntity(DataStoreDTO dataStore) {
        lakehouse.api.entities.DataStore result = new lakehouse.api.entities.DataStore();
        result.setKey(dataStore.getKey());
        result.setComment(dataStore.getComment());
        result.setVendor(dataStore.getVendor());
        result.setInterfaceType(dataStore.getInterfaceType());
        return result;
    }

    private lakehouse.api.entities.DataStore findByKey(String key){
        return dataStoreRepository
                .findById(key)
                .orElseThrow(()-> {
                    logger.info("Can't get key: %s", key);
                    return new RuntimeException("Can't get key");});
    }

    @GetMapping("/datastores")
    List<DataStoreDTO> all() {
        return dataStoreRepository.findAll().stream().map(this::mapDataStoreToDTO).toList();
    }

    @PostMapping("/datastores")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    DataStoreDTO newDataStore(@RequestBody DataStoreDTO dataStore ) {
        lakehouse.api.entities.DataStore ds =  dataStoreRepository.save(mapDataStoreToEntity(dataStore));

        return mapDataStoreToDTO(dataStoreRepository.save(mapDataStoreToEntity(dataStore)));
    }


    @GetMapping("/datastores/{key}")
    DataStoreDTO one(@PathVariable String key) {
        return mapDataStoreToDTO( findByKey(key));
    }


    @DeleteMapping("/datastores/{key}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    void deleteDataStore(@PathVariable String key) {
        /*if (dataStoreRepository.findById(key).isPresent())
            dataStorePropertyRepository
                    .findByDataStore(findByKey(key))
                    .forEach(dataStoreProperty -> dataStorePropertyRepository
                            .deleteById(dataStoreProperty.getId()));*/
        dataStoreRepository.deleteById(key);
    }
}
