package org.lakehouse.ui.modeller.service;

import com.fasterxml.jackson.annotation.JsonValue;
import org.lakehouse.client.api.constant.DatabaseProtocol;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.ui.modeller.dto.EnumOption;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of every enum that appears as a typed property of a configuration
 * DTO, keyed by the enum <strong>simple name</strong> (e.g.
 * {@code ConstraintType}, {@code ConstraintLevelCheck},
 * {@code ReferenceAction}, {@code DatabaseProtocol}). First registration wins;
 * duplicates are dropped with a WARN.
 * <p>
 * Dropdown options are computed <em>live</em> from {@code values()} on every
 * call, so a new enum constant shows up in the editor dropdown immediately,
 * without any schema or frontend change.
 * <p>
 * The canonical value of a constant (what is stored in the YAML and shown in
 * the dropdown) is resolved in this order:
 * <ol>
 *   <li>the result of a no-arg method annotated {@code @JsonValue} (e.g.
 *       {@code Types} enums annotate their {@code toString()} so
 *       {@code ConstraintType.PRIMARY} → {@code "primary"},
 *       {@code ReferenceAction.SET_NULL} → {@code "SET NULL"});</li>
 *   <li>a string member named {@code label} (accessor {@code label()},
 *       {@code getLabel()}, or public field);</li>
 *   <li>a string member named {@code protocolPrefix}
 *       ({@code DatabaseProtocol} → {@code "postgresql"}, {@code "s3"});</li>
 *   <li>fallback: {@code name()}.{@link String#toLowerCase()}</li>
 * </ol>
 */
public final class EnumOptionsService {

    private static final Map<String, Class<? extends Enum<?>>> REGISTRY = new LinkedHashMap<>();
    private static final System.Logger LOG = System.getLogger("modeller.enums");

    static {
        register(Types.ConstraintType.class);
        register(Types.ConstraintLevelCheck.class);
        register(Types.ReferenceAction.class);
        register(DatabaseProtocol.class);
    }

    private EnumOptionsService() {
    }

    /** Registers an enum under its simple name (first wins). */
    public static synchronized void register(Class<? extends Enum<?>> enumClass) {
        String name = enumClass.getSimpleName();
        Class<? extends Enum<?>> previous = REGISTRY.putIfAbsent(name, enumClass);
        if (previous != null && previous != enumClass)
            LOG.log(System.Logger.Level.WARNING,
                    "Enum name '{0}' already registered (keeping {1}, ignoring {2})",
                    name, previous.getName(), enumClass.getName());
    }

    /**
     * Live dropdown options for the enum registered under {@code name},
     * computed from {@code values()} on every call. Returns {@code null} when
     * the enum is unknown.
     */
    public static List<EnumOption> options(String name) {
        Class<? extends Enum<?>> enumClass = REGISTRY.get(name);
        if (enumClass == null)
            return null;
        return Arrays.stream(enumClass.getEnumConstants())
                .map(EnumOptionsService::option)
                .toList();
    }

    /** Simple names of all registered enums, in registration order. */
    public static List<String> names() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    /**
     * All registered enums as a name → {@link #options(String)} map,
     * in registration order.
     */
    public static Map<String, List<EnumOption>> all() {
        Map<String, List<EnumOption>> result = new LinkedHashMap<>();
        for (String name : REGISTRY.keySet())
            result.put(name, options(name));
        return result;
    }

    /** {@code true} when {@code value} is a known canonical value of the enum. */
    public static boolean contains(String name, String value) {
        List<EnumOption> options = options(name);
        return options != null && options.stream().anyMatch(o -> o.value().equals(value));
    }

    private static EnumOption option(Enum<?> constant) {
        String value = canonicalValue(constant);
        return new EnumOption(value, value);
    }

    private static String canonicalValue(Enum<?> constant) {
        Class<?> enumClass = constant.getDeclaringClass();
        String jsonValue = jsonValue(constant, enumClass);
        if (jsonValue != null)
            return jsonValue;
        String label = labelMember(constant, enumClass);
        if (label != null)
            return label;
        String prefix = protocolPrefix(constant, enumClass);
        if (prefix != null)
            return prefix;
        return constant.name().toLowerCase(Locale.ROOT);
    }

    /** Reads a no-arg method annotated {@code @JsonValue}. */
    private static String jsonValue(Enum<?> constant, Class<?> enumClass) {
        for (Method method : enumClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 || !method.isAnnotationPresent(JsonValue.class))
                continue;
            try {
                Object result = method.invoke(constant);
                if (result != null)
                    return result.toString();
            } catch (ReflectiveOperationException e) {
                LOG.log(System.Logger.Level.WARNING, "Cannot read @JsonValue on {0}", enumClass.getName());
            }
        }
        return null;
    }

    private static String labelMember(Enum<?> constant, Class<?> enumClass) {
        return memberString(constant, enumClass, "label");
    }

    private static String protocolPrefix(Enum<?> constant, Class<?> enumClass) {
        return memberString(constant, enumClass, "protocolPrefix");
    }

    /** Reads {@code member()} / {@code getMember()}, then a public field. */
    private static String memberString(Enum<?> constant, Class<?> enumClass, String member) {
        String capitalized = Character.toUpperCase(member.charAt(0)) + member.substring(1);
        for (String accessor : List.of(member, "get" + capitalized)) {
            try {
                Method method = enumClass.getMethod(accessor);
                if (method.getParameterCount() != 0)
                    continue;
                Object result = method.invoke(constant);
                if (result instanceof String string)
                    return string;
            } catch (NoSuchMethodException ignored) {
                // try the next accessor
            } catch (ReflectiveOperationException e) {
                LOG.log(System.Logger.Level.WARNING, "Cannot read {0} on {1}", accessor, enumClass.getName());
            }
        }
        try {
            var field = enumClass.getField(member);
            Object result = field.get(constant);
            return result instanceof String string ? string : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
