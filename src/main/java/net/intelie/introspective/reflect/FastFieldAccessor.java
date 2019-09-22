package net.intelie.introspective.reflect;

import net.intelie.introspective.hotspot.JVM;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastFieldAccessor {
    private final Supplier<Accessor>[] accessors;
    private final String name;
    private int nextAccessor;
    private Accessor currentAccessor;

    public FastFieldAccessor(Field field) {
        this.name = field.getName();
        this.accessors = new Supplier[]{
                () -> unsafeAccessor(field),
                () -> reflectionAccessor(field),
                () -> (Accessor) (obj -> null)
        };

        nextAccessor = 0;
        moveToNextAccessor();
    }

    private void moveToNextAccessor() {
        try {
            currentAccessor = accessors[nextAccessor++].get();
        } catch (Throwable e) {
            Logger.getLogger(FastFieldAccessor.class.getName())
                    .log(Level.INFO, "Error getting accessor for field '" + name + "'", e);
        }
    }


    private static Accessor unsafeAccessor(Field field) {
        long offset = JVMPrimitives.getFieldOffset(field);
        return x -> JVMPrimitives.getFieldObject(x, offset);
    }

    private static Accessor reflectionAccessor(Field field) {
        field.setAccessible(true);
        return x -> {
            try {
                return field.get(x);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };

    }

    public Object get(Object target) {
        do {
            try {
                return currentAccessor.get(target);
            } catch (Throwable e) {
                Logger.getLogger(FastFieldAccessor.class.getName())
                        .log(Level.INFO, "Error using accessor for field '" + name + "'", e);
                moveToNextAccessor();
            }
        } while (true);
    }

    public String name() {
        return name;
    }

    private static interface Accessor {
        Object get(Object obj) throws Exception;
    }
}
