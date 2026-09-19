package org.lakehouse.ui.modeller.dto;

import java.util.List;

/**
 * One editable property of a configuration kind.
 * <p>
 * {@code type} values: string | integer | double | boolean | datetime | map |
 * object | list | dag | code. {@code keyName=true} marks the property that
 * uniquely identifies a config file and drives the target file path.
 * <p>
 * Optional editor metadata:
 * <ul>
 *   <li>{@code enumValues}/{@code enumDefault} render the property as a dropdown
 *       (values use the camelCase/lowercase convention stored in the YAML);</li>
 *   <li>{@code picker} turns a read-only property into a picker-backed input
 *       ({@code "datasetConstraint"}, {@code "columns"}, {@code "scriptKey"},
 *       {@code "nameSpace"}, {@code "dataSource"}, {@code "task"}, {@code
 *       "taskExecutionServiceGroup"} or {@code "driver"});</li>
 *   <li>{@code clearable} adds a "×" button next to a picker field that clears
 *       the value after a confirmation;</li>
 *   <li>{@code visibleField}/{@code visibleValue} hide the property unless a
 *       sibling property equals the expected value.</li>
 * </ul>
 */
public record FieldSchema(
        String name,
        String type,
        String label,
        boolean keyName,
        boolean required,
        boolean readOnly,
        String description,
        boolean uniqueAcrossKind,
        FieldSchema item,
        List<FieldSchema> children,
        List<String> enumValues,
        String enumDefault,
        String picker,
        String visibleField,
        String visibleValue,
        boolean clearable) {

    public FieldSchema(String name, String type, boolean keyName, List<FieldSchema> children, FieldSchema item) {
        this(name, type, humanize(name), keyName, false, false, null, false, item, children,
                null, null, null, null, null, false);
    }

    public FieldSchema(String name, String type, boolean keyName, boolean uniqueAcrossKind,
                       List<FieldSchema> children, FieldSchema item) {
        this(name, type, humanize(name), keyName, false, false, null, uniqueAcrossKind, item, children,
                null, null, null, null, null, false);
    }

    private static String humanize(String name) {
        if (name == null || name.isEmpty())
            return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
                continue;
            }
            if (Character.isUpperCase(c))
                sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }
}