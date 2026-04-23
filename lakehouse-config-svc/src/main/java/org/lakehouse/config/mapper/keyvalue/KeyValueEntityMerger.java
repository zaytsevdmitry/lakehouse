package org.lakehouse.config.mapper.keyvalue;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.exception.KeyValueEntityMergeException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyValueEntityMerger {
    private final KeyValueEntitySpecifier keyValueEntitySpecifier;

    public KeyValueEntityMerger(
            KeyValueEntitySpecifier keyValueEntitySpecifier){
        this.keyValueEntitySpecifier = keyValueEntitySpecifier;
    }

    public void mergeAbstractKeyValues(
            List<KeyValueAbstract> currentProperties,
            Map<String,String> newProperties){

        PropertiesIUDCase propertiesIUDCase = splitAbstractKeyValues(currentProperties, newProperties);

        keyValueEntitySpecifier.getJpaRepository().deleteAll(
                propertiesIUDCase
                        .getToBeDeleted());

        keyValueEntitySpecifier.getJpaRepository().saveAll(
                propertiesIUDCase
                        .getToBeSaved());
    }

    public PropertiesIUDCase splitAbstractKeyValues(
            List<KeyValueAbstract> currentProperties,
            Map<String,String> newProperties
    )throws KeyValueEntityMergeException {

        PropertiesIUDCase result = new PropertiesIUDCase();
        // delete and update
        currentProperties.forEach(currentProperty -> {
            if (newProperties.containsKey(currentProperty.getKey())) {
                currentProperty.setValue(newProperties.get(currentProperty.getKey()));
                result.getToBeUpdated().add(currentProperty);
            }
            else {
                result.getToBeDeleted().add(currentProperty);
            }
        });

        Set<String> keysForUpdate = result.getToBeUpdated().stream().map(KeyValueAbstract::getKey).collect(Collectors.toSet());

        // properties for insert
        for (Map.Entry<String,String> stringStringEntry : newProperties.entrySet()
                .stream()
                .filter(stringStringEntry -> !keysForUpdate.contains(stringStringEntry.getKey())).toList()
        ){
            try {
                KeyValueAbstract newProperty = keyValueEntitySpecifier.getEntityClass().newInstance();
                newProperty.setKey(stringStringEntry.getKey());
                newProperty.setValue(stringStringEntry.getValue());
                newProperty = keyValueEntitySpecifier.entityFeel(newProperty);
                result.getToBeInserted().add(newProperty);
            } catch ( InstantiationException | IllegalAccessException e) {
                throw new KeyValueEntityMergeException(stringStringEntry.getKey(),"Create class instace error", e);
            }
        }
        return result;
    }
}
