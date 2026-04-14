package com.sni.bokaticowork.core.audit.util;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class AuditDiffUtil {

    public static Map<String, Object> diff(Object before, Object after) {
        Map<String, Object> changes = new LinkedHashMap<>();

        for (Field f : before.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Object b = f.get(before);
                Object a = f.get(after);
                if (!Objects.equals(b, a)) {
                    changes.put(f.getName(), Map.of("before", b, "after", a));
                }
            } catch (IllegalAccessException ignored) {}
        }
        return changes;
    }
}
