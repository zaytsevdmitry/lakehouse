package org.lakehouse.config.service.dataset;

import org.lakehouse.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.lakehouse.config.entities.dataset.DataSetConstraint;
import org.lakehouse.config.repository.dataset.ForeignKeyReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataSetForeignKeyReferenceService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ForeignKeyReferenceRepository foreignKeyReferenceRepository;

    public DataSetForeignKeyReferenceService(ForeignKeyReferenceRepository foreignKeyReferenceRepository) {
        this.foreignKeyReferenceRepository = foreignKeyReferenceRepository;
    }

    public void applyReference(DataSetConstraint dataSetConstraint, ForeignKeyReferenceDTO foreignKeyReferenceDTO){

    }
}
