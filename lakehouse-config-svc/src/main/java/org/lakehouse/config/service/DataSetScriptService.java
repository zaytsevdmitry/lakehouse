package org.lakehouse.config.service;

import org.lakehouse.client.api.dto.configs.DataSetScriptDTO;
import org.lakehouse.config.entities.DataSetScript;
import org.lakehouse.config.repository.DataSetScriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataSetScriptService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSetScriptRepository dataSetScriptRepository;

    public DataSetScriptService(
            DataSetScriptRepository dataSetScriptRepository) {
        this.dataSetScriptRepository = dataSetScriptRepository;
    }

    /**
     *
     * @param dataSetName
     * @return
     *
     * Note: @Transactional used by cause // Caused by: org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
     */
    @Transactional
    public List<DataSetScriptDTO> findDataSetScriptDTOListByDataSetName(String dataSetName){

       return dataSetScriptRepository.findByDataSetName(dataSetName).stream().map(dataSetScript -> {
            DataSetScriptDTO dataSetScriptDTO = new DataSetScriptDTO();
            dataSetScriptDTO.setKey(dataSetScript.getScript().getKey());
            dataSetScriptDTO.setOrder(dataSetScript.getScriptOrder());
            return dataSetScriptDTO;
        }).toList();

    }
    @Transactional
    public List<DataSetScript> findDataSetScriptListByDataSetName(String dataSetName){
        return dataSetScriptRepository.findByDataSetName(dataSetName);
    }

}
