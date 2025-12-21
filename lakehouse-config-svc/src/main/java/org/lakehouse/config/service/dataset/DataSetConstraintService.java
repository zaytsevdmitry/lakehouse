package org.lakehouse.config.service.dataset;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.ReferenceDTO;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetConstraint;
import org.lakehouse.config.entities.dataset.Reference;
import org.lakehouse.config.exception.DataSetConstraintNotFoundException;
import org.lakehouse.config.exception.DataSetConstraintReferenceConfigNotFoundException;
import org.lakehouse.config.repository.dataset.DataSetConstraintRepository;
import org.lakehouse.config.repository.dataset.ReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSetConstraintService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSetConstraintRepository dataSetConstraintRepository;
    private final ReferenceRepository referenceRepository;
    public DataSetConstraintService(DataSetConstraintRepository dataSetConstraintRepository, ReferenceRepository referenceRepository) {
        this.dataSetConstraintRepository = dataSetConstraintRepository;
        this.referenceRepository = referenceRepository;
    }
    public void applyConstraints(DataSet dataSet, List<DataSetConstraintDTO> dataSetConstraintDTOList) throws DataSetConstraintReferenceConfigNotFoundException{
        List<DataSetConstraint> current = dataSetConstraintRepository.findByDataSetKeyName(dataSet.getKeyName());
        List<DataSetConstraint> newConst = mapListDTOsToEntities(dataSet,dataSetConstraintDTOList).stream().map(dataSetConstraint -> {
            List<DataSetConstraint> found = current.stream().filter(c-> c.getName().equals(dataSetConstraint.getName())).toList();
            if (!found.isEmpty()){
                dataSetConstraint.setId(found.get(0).getId());
                logger.info("Constraint {} will be updated. Id is {}", dataSetConstraint.getName(), dataSetConstraint.getId());
            }else {
                logger.info("Constrain {} will be added. Id is null", dataSetConstraint.getName());
            }
            return dataSetConstraint;
        }).toList();

        for (DataSetConstraint dataSetConstraint:current){
            List<DataSetConstraint> found = newConst.stream().filter(c-> c.getName().equals(dataSetConstraint.getName())).toList();
            if (found.isEmpty()){
                logger.info("Constraint {} will be deleted. Id is {}", dataSetConstraint.getName(), dataSetConstraint.getId());
                dataSetConstraintRepository.delete(dataSetConstraint);
            }
        }


        for (DataSetConstraint dataSetConstraint: newConst){
            dataSetConstraintRepository.save(dataSetConstraint);
            if (dataSetConstraint.getType().equals(Types.Constraint.foreign)) {
                Reference newReference = findReference(dataSetConstraint,dataSetConstraintDTOList);
                if(dataSetConstraint.getId() != null && dataSetConstraint.getId() > 0){
                    referenceRepository.findByConstraintId(dataSetConstraint.getId()).ifPresent(reference -> newReference.setId(reference.getId()));
                }
                referenceRepository.save(newReference);
            }
        }


    }
    private List<DataSetConstraint> mapListDTOsToEntities( DataSet dataSet, List<DataSetConstraintDTO> dataSetConstraintDTOList){
        return dataSetConstraintDTOList.stream().map(dataSetConstraintDTO -> mapDTOtoEntity(dataSet,dataSetConstraintDTO)).toList();
    }

    private DataSetConstraint mapDTOtoEntity(DataSet dataSet, DataSetConstraintDTO dataSetConstraintDTO){
        DataSetConstraint result = new DataSetConstraint();
        result.setDataSet(dataSet);
        result.setColumns(dataSetConstraintDTO.getColumns());
        result.setType(dataSetConstraintDTO.getType());
        result.setName(dataSetConstraintDTO.getName());
        result.setEnabled(dataSetConstraintDTO.isEnabled());
        result.setConstructLevelCheck(dataSetConstraintDTO.isConstructLevelCheck());
        result.setRuntimeLevelCheck(dataSetConstraintDTO.isRuntimeLevelCheck());
        return result;
    }
    private Reference findReference(DataSetConstraint dataSetConstraint, List<DataSetConstraintDTO> dataSetConstraintDTOList){
        List<DataSetConstraintDTO> found = dataSetConstraintDTOList
                .stream()
                .filter(dataSetConstraintDTO -> dataSetConstraintDTO.getName().equals( dataSetConstraint.getName())).toList();
        if (found.isEmpty()){
           throw  new DataSetConstraintReferenceConfigNotFoundException(dataSetConstraint.getName(),dataSetConstraint.getDataSet().getKeyName());
        }else {
            ReferenceDTO referenceDTO =  found.get(0).getReference();
            Reference result = new Reference();
            result.setDataSetConstraint(dataSetConstraint);
            DataSetConstraint ref = dataSetConstraintRepository.findByDataSetKeyNameAndName(
                            referenceDTO.getDataSetKeyName(),
                            referenceDTO.getConstraintName())
                    .orElseThrow(() -> new DataSetConstraintNotFoundException(
                            referenceDTO.getConstraintName(),
                            referenceDTO.getDataSetKeyName()));
            result.setRefDataSetConstraint(ref);
            return result;
        }
    }
    public List<DataSetConstraintDTO> mapDataSetConstraintsToDTOList(String dataSetKeyName){
        return dataSetConstraintRepository
                .findByDataSetKeyName(dataSetKeyName)
                .stream()
                .map(this::mapEntityToDataSetConstraintDTO)
                .toList();
    }

    private DataSetConstraintDTO mapEntityToDataSetConstraintDTO(DataSetConstraint dataSetConstraint){
            DataSetConstraintDTO result = new DataSetConstraintDTO();
            result.setColumns(dataSetConstraint.getColumns());
            result.setType(dataSetConstraint.getType());
            result.setName(dataSetConstraint.getName());
            result.setEnabled(dataSetConstraint.isEnabled());
            result.setRuntimeLevelCheck(dataSetConstraint.isRuntimeLevelCheck());
            result.setConstructLevelCheck(dataSetConstraint.isConstructLevelCheck());
            if (dataSetConstraint.getType().equals(Types.Constraint.foreign)) {
                ReferenceDTO referenceDTO = new ReferenceDTO();
                referenceRepository.findByConstraintId(dataSetConstraint.getId()).ifPresent(reference -> {
                    referenceDTO.setDataSetKeyName(reference.getDataSetConstraint().getDataSet().getKeyName());
                    referenceDTO.setConstraintName(reference.getDataSetConstraint().getName());
                    referenceDTO.setOnDelete(reference.getOnDelete());
                    referenceDTO.setOnUpdate(reference.getOnUpdate());
                    result.setReference(referenceDTO);
                });
            }
            return result;
    }
}
