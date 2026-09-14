package org.lakehouse.modeller.service;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.modeller.dto.FieldSchema;
import org.lakehouse.modeller.dto.KindSchema;
import org.lakehouse.modeller.yaml.ConfigKind;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reflection-driven form schema generator. For every {@link ConfigKind} it resolves the
 * corresponding {@code lakehouse-common} DTO (bean or record) into a {@link KindSchema}
 * tree of {@link FieldSchema}s so the frontend renders a dynamic editor without
 * hard-coding field lists.
 */
public class SchemaService {

    private static final Comparator<FieldSchema> FIELD_ORDER =
            Comparator.comparing((FieldSchema f) -> !f.keyName())
                    .thenComparing(f -> f.name().toLowerCase(Locale.ROOT));
    private static final Pattern ENTITY_PACKAGE = Pattern.compile("^org\\.lakehouse\\.client\\.api\\.dto(\\..*)?$");
    private static final int MAX_DEPTH = 4;

    /** Requested column order for {@code TaskDTO} (ScenarioActTemplate Tasks table). */
    private static final List<String> TASK_FIELD_ORDER = List.of(
            "name", "template", "taskProcessor", "taskProcessorBody", "taskExecutionServiceGroupName",
            "description", "driverKeyName", "importance", "maxRetries", "sqlTemplate", "taskProcessorArgs");

    private final Map<String, KindSchema> schemas = new LinkedHashMap<>();

    public SchemaService() {
        for (ConfigKind kind : ConfigKind.values())
            schemas.put(ConfigKind.normalize(kind.yamlValue()), build(kind));
    }

    public KindSchema schema(String kind) {
        return schemas.get(ConfigKind.normalize(kind));
    }

    public List<KindSchema> all() {
        return new ArrayList<>(schemas.values());
    }

    private KindSchema build(ConfigKind kind) {
        Class<?> dtoClass = load(kind.dtoClassName());
        List<FieldSchema> fields = orderFields(beanProperties(dtoClass).stream()
                .map(p -> mapProperty(p, dtoClass, 0, new HashSet<>()))
                .sorted(FIELD_ORDER)
                .toList(), dtoClass);
        return new KindSchema(kind.yamlValue(), dtoClass.getName(), kind.directory(), fields);
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("DTO class not found: " + className, e);
        }
    }

    // ------------------------------------------------------------------
    // property discovery (beans and records)
    // ------------------------------------------------------------------

    private record Property(String name, Type type, Class<?> raw) {
    }

    private static List<Property> beanProperties(Class<?> type) {
        List<Property> result = new ArrayList<>();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents())
                result.add(new Property(component.getName(), component.getGenericType(), component.getType()));
            return result;
        }
        try {
            BeanInfo info = Introspector.getBeanInfo(type, Object.class);
            for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
                if (descriptor.getReadMethod() == null || descriptor.getWriteMethod() == null)
                    continue;
                result.add(new Property(descriptor.getName(), descriptor.getReadMethod().getGenericReturnType(),
                        descriptor.getReadMethod().getReturnType()));
            }
        } catch (java.beans.IntrospectionException e) {
            throw new IllegalStateException("Cannot introspect " + type.getName(), e);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // type mapping
    // ------------------------------------------------------------------

    private FieldSchema mapProperty(Property property, Class<?> owner, int depth, Set<String> visited) {
        String name = property.name;
        String simple = property.raw().getSimpleName();
        boolean keyName = "keyName".equals(name)
                || (name.equals("name")
                && (owner.getSimpleName().equals("TaskDTO") || owner.getSimpleName().equals("TaskExecutionServiceGroupDTO")))
                || (name.equals("key") && owner.getSimpleName().equals("ScriptDTO"));
        boolean uniqueAcrossKind = keyName
                && owner.getSimpleName().equals("ScriptDTO");

        FieldSchema field;
        // "value" of ScriptDTO carries executable code rendered by the code editor.
        if (name.equals("value") && owner.getSimpleName().equals("ScriptDTO"))
            field = new FieldSchema(name, "code", keyName, null, null);
        else if (simple.equals("DagEdgeDTO"))
            field = new FieldSchema(name, "dag", keyName,
                    List.of(new FieldSchema("from", "string", false, null, null),
                            new FieldSchema("to", "string", false, null, null)), null);
        else if (property.raw().isEnum())
            field = new FieldSchema(name, "string", keyName, null, null);
        else if (Map.class.isAssignableFrom(property.raw()))
            field = new FieldSchema(name, "map", keyName, childrenOf(property, depth, visited), null);
        else if (Collection.class.isAssignableFrom(property.raw())) {
            String element = elementClassName(property.type());
            Class<?> elementClass = element == null ? null : classOrNull(element);
            if (elementClass != null && elementClass.getSimpleName().equals("DagEdgeDTO"))
                field = new FieldSchema(name, "dag", keyName,
                        List.of(new FieldSchema("from", "string", false, null, null),
                                new FieldSchema("to", "string", false, null, null)), null);
            else
                field = new FieldSchema(name, "list", keyName, null, itemSchema(elementClass, depth, visited));
        } else {
            String type = primitiveType(property.raw());
            if (type != null)
                field = new FieldSchema(name, type, keyName, uniqueAcrossKind, null, null);
            else if (isEntity(property.raw())) {
                if (depth >= MAX_DEPTH || visited.contains(owner.getName()))
                    field = new FieldSchema(name, "string", keyName, null, null);
                else {
                    Set<String> nextVisited = new HashSet<>(visited);
                    nextVisited.add(owner.getName());
                    List<FieldSchema> children = beanProperties(property.raw()).stream()
                            .map(p -> mapProperty(p, property.raw(), depth + 1, nextVisited))
                            .sorted(FIELD_ORDER)
                            .toList();
                    field = new FieldSchema(name, "object", keyName, orderFields(children, property.raw()), null);
                }
            } else {
                field = new FieldSchema(name, "string", keyName, null, null);
            }
        }
        return decorate(field, owner, name);
    }

    /**
     * Applies per-DTO editor metadata: enum dropdowns, read-only picker inputs
     * and fields visible only when a sibling property takes a specific value.
     */
    private static FieldSchema decorate(FieldSchema field, Class<?> owner, String name) {
        if (owner.getSimpleName().equals("DataSetDTO"))
            return switch (name) {
                case "nameSpaceKeyName" -> redescribe(field, true, "nameSpace", null, null, null, null, true);
                case "dataSourceKeyName" -> redescribe(field, true, "dataSource", null, null, null, null, false);
                default -> field;
            };
        if (owner.getSimpleName().equals("TaskDTO"))
            return switch (name) {
                case "template" -> redescribe(field, true, "task", null, null, null, null, false);
                case "taskExecutionServiceGroupName" ->
                        redescribe(field, true, "taskExecutionServiceGroup", null, null, null, null, false);
                case "driverKeyName" -> redescribe(field, true, "driver", null, null, null, null, true);
                default -> field;
            };
        if (owner.getSimpleName().equals("DataSetConstraintDTO")) {
            return switch (name) {
                case "type" -> redescribe(field, false, null,
                        enumValues(Types.ConstraintType.class), "primary", null, null, false);
                case "constraintLevelCheck" -> redescribe(field, false, null,
                        enumValues(Types.ConstraintLevelCheck.class), null, null, null, false);
                case "checkExpr" -> redescribe(field, false, null, null, null, "type", "check", false);
                case "reference" -> redescribe(field, false, null, null, null, "type", "foreign", false);
                case "columns" -> redescribe(field, true, "columns", null, null, null, null, false);
                default -> field;
            };
        }
        if (owner.getSimpleName().equals("ForeignKeyReferenceDTO")) {
            return switch (name) {
                case "dataSetKeyName" -> redescribe(field, true, "datasetConstraint", null, null, null, null, false);
                case "constraintName" -> redescribe(field, true, null, null, null, null, null, false);
                case "onDelete" -> redescribe(field, false, null, enumValues(Types.ReferenceAction.class), null, null, null, false);
                case "onUpdate" -> redescribe(field, false, null, enumValues(Types.ReferenceAction.class), null, null, null, false);
                default -> field;
            };
        }
        if (owner.getSimpleName().equals("ScriptReferenceDTO") && name.equals("key"))
            return redescribe(field, true, "scriptKey", null, null, null, null, true);
        if (owner.getSimpleName().equals("SQLTemplateDTO"))
            return redescribe(field, true, "scriptKey", field.enumValues(), field.enumDefault(),
                    field.visibleField(), field.visibleValue(), true);
        return field;
    }

    private static FieldSchema redescribe(FieldSchema f, boolean readOnly, String picker,
                                          List<String> enumValues, String enumDefault,
                                          String visibleField, String visibleValue,
                                          boolean clearable) {
        return new FieldSchema(f.name(), f.type(), f.label(), f.keyName(), f.required(), readOnly,
                f.description(), f.uniqueAcrossKind(), f.item(), f.children(),
                enumValues != null ? enumValues : f.enumValues(),
                enumDefault != null ? enumDefault : f.enumDefault(),
                picker != null ? picker : f.picker(),
                visibleField != null ? visibleField : f.visibleField(),
                visibleValue != null ? visibleValue : f.visibleValue(),
                clearable || f.clearable());
    }

    /**
     * Enum values in the form stored in the workspace YAML files: enum names are
     * lowercased and underscore-separated words are camel-cased, so for example
     * {@code DATA_QUALITY -> "dataQuality"} and {@code PRIMARY -> "primary"}.
     */
    private static List<String> enumValues(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> yamlEnumValue(e.name()))
                .toList();
    }

    private static String yamlEnumValue(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!name.contains("_"))
            return lower;
        String[] parts = lower.split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private FieldSchema itemSchema(Class<?> elementClass, int depth, Set<String> visited) {
        if (elementClass == null)
            return new FieldSchema("item", "string", false, null, null);
        if (isEntity(elementClass))
            return new FieldSchema("item", "object", false,
                    orderFields(beanProperties(elementClass).stream()
                            .map(p -> mapProperty(p, elementClass, depth, visited))
                            .sorted(FIELD_ORDER)
                            .toList(), elementClass), null);
        String type = primitiveType(elementClass);
        return new FieldSchema("item", type == null ? "string" : type, false, null, null);
    }

    /**
     * Reorders fields of a known DTO into its canonical column order; unknown
     * entities fall back to the default alphabetical sort.
     */
    private static List<FieldSchema> orderFields(List<FieldSchema> fields, Class<?> entityClass) {
        if (entityClass == null)
            return new ArrayList<>(fields);
        return switch (entityClass.getSimpleName()) {
            case "ServiceDTO" -> orderByName(fields, List.of("host", "port", "urn", "properties"));
            case "ScriptDTO" -> orderByName(fields, List.of("key", "value"));
            case "TaskDTO" -> orderByName(fields, TASK_FIELD_ORDER);
            case "DataSetConstraintDTO" -> orderByName(fields, List.of("constraintLevelCheck", "type", "columns",
                    "checkExpr", "reference", "enabled", "tableConstraintDDLCreateOverride", "tableConstraintDDLAddOverride"));
            case "ForeignKeyReferenceDTO" -> orderByName(fields, List.of("dataSetKeyName", "constraintName", "onDelete", "onUpdate"));
            case "ScriptReferenceDTO" -> orderByName(fields, List.of("key", "order"));
            case "ColumnDTO" -> orderByName(fields, List.of("name"));
            default -> new ArrayList<>(fields);
        };
    }

    private static List<FieldSchema> orderByName(List<FieldSchema> fields, List<String> canonical) {
        Map<String, FieldSchema> byName = new LinkedHashMap<>();
        for (FieldSchema field : fields)
            byName.put(field.name(), field);
        List<FieldSchema> ordered = new ArrayList<>();
        for (String name : canonical)
            if (byName.containsKey(name))
                ordered.add(byName.remove(name));
        ordered.addAll(byName.values());
        return ordered;
    }

    private List<FieldSchema> childrenOf(Property property, int depth, Set<String> visited) {
        List<FieldSchema> children = new ArrayList<>();
        if (property.type() instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments();
            if (args.length == 2) {
                Class<?> valueClass = classOrNull(args[1]);
                if (valueClass != null && isEntity(valueClass)) {
                    Set<String> nextVisited = new HashSet<>(visited);
                    nextVisited.add(property.raw().getName());
                    children = orderFields(beanProperties(valueClass).stream()
                            .map(p -> mapProperty(p, valueClass, depth + 1, nextVisited))
                            .sorted(FIELD_ORDER)
                            .toList(), valueClass);
                } else {
                    String type = valueClass == null ? "string" : valueOrDefault(primitiveType(valueClass));
                    children = List.of(new FieldSchema("value", type, false, null, null));
                }
            }
        }
        return children;
    }

    private static String valueOrDefault(String value) {
        return value == null ? "string" : value;
    }

    /** Resolves the single element type argument of a {@code Collection<T>} property. */
    private static String elementClassName(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> clazz)
                return clazz.getName();
        }
        return null;
    }

    private static Class<?> classOrNull(Type type) {
        try {
            if (type instanceof Class<?> clazz)
                return clazz;
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static Class<?> classOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String primitiveType(Class<?> raw) {
        String simple = raw.getSimpleName();
        if (simple.equals("String"))
            return "string";
        if (simple.equals("Integer") || simple.equals("int") || simple.equals("Long") || simple.equals("long")
                || simple.equals("Short") || simple.equals("short"))
            return "integer";
        if (simple.equals("Double") || simple.equals("double") || simple.equals("Float") || simple.equals("float")
                || simple.equals("BigDecimal"))
            return "double";
        if (simple.equals("Boolean") || simple.equals("boolean"))
            return "boolean";
        if (simple.contains("Date") || simple.contains("Time") || simple.contains("Instant"))
            return "datetime";
        return null;
    }

    private static boolean isEntity(Class<?> raw) {
        return ENTITY_PACKAGE.matcher(raw.getName()).matches();
    }
}