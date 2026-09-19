package org.lakehouse.ui.modeller.dto;

import java.util.List;

/**
 * Schema of an editable configuration kind, generated from the corresponding
 * {@code lakehouse-common} DTO and rendered as a dynamic form by the frontend.
 */
public record KindSchema(
        String kind,
        String dtoClass,
        String directory,
        List<FieldSchema> fields) {
}