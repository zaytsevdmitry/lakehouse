package org.lakehouse.config.service.dataset;

import org.lakehouse.client.api.dto.configs.dataset.ReferenceDTO;
import org.lakehouse.config.entities.dataset.DataSetConstraint;
import org.lakehouse.config.repository.dataset.ReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataSetReferenceService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ReferenceRepository referenceRepository;

    public DataSetReferenceService(ReferenceRepository referenceRepository) {
        this.referenceRepository = referenceRepository;
    }

    public void applyReference(DataSetConstraint dataSetConstraint, ReferenceDTO referenceDTO){

    }
}
