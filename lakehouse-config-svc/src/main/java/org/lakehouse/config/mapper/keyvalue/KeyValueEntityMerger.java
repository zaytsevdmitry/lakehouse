/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
