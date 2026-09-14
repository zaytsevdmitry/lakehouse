package org.lakehouse.modeller.dto;

/**
 * One selectable option of an enum-typed configuration property.
 *
 * @param value canonical value as it is stored in the workspace YAML file and
 *              emitted back into the YAML on save (e.g. {@code "postgresql"},
 *              {@code "s3"}, {@code "SET NULL"}, {@code "primary"});
 * @param label human-readable label shown in the dropdown (currently equal to
 *              {@code value}).
 */
public record EnumOption(String value, String label) {
}
