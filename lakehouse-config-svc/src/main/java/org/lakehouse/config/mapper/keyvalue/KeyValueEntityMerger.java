/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
