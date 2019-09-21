package net.intelie.introspective.reflect;


import java.lang.reflect.Field;

public class FastFieldAccessor {
    private final Field field;

    public FastFieldAccessor(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    public Object get(Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public String name() {
        return field.getName();
    }
}
