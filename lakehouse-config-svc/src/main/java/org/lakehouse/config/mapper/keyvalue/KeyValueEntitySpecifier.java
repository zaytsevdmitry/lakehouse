package org.lakehouse.config.mapper.keyvalue;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyValueEntitySpecifier  {
    KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract);
    Class<? extends KeyValueAbstract> getEntityClass();
    JpaRepository getJpaRepository();
}
