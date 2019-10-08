package net.intelie.introspective.reflect;

import net.intelie.introspective.util.UnsafeGetter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastFieldAccessor {
    private static final Unsafe U = UnsafeGetter.get();
    private final Deque<Supplier<Accessor>> accessors;
    private final String name;
    private final long offset;
    private final int declarationOrder;
    private Accessor currentAccessor;

    public FastFieldAccessor(int declarationOrder, Field field) {
        this(declarationOrder, field, true);
    }

    public FastFieldAccessor(int declarationOrder, Field field, boolean allowUnsafe) {
        this.name = field.getName();
        this.accessors = new ArrayDeque<>();
        this.declarationOrder = declarationOrder;

        if (allowUnsafe && U != null) {
            offset = U.objectFieldOffset(field);
            this.accessors.addLast(() -> unsafeAccessor(field, offset));
        } else {
            offset = 0;
        }
        this.accessors.addLast(() -> reflectionAccessor(field));
        this.accessors.addLast(() -> obj -> null);

        moveToNextAccessor();
    }

    private static Accessor unsafeAccessor(Field field, long offset) {
        Class<?> type = field.getType();
        if (byte.class.equals(type))
            return x -> U.getByte(x, offset);
        if (short.class.equals(type))
            return x -> U.getShort(x, offset);
        if (int.class.equals(type))
            return x -> U.getInt(x, offset);
        if (long.class.equals(type))
            return x -> U.getLong(x, offset);
        if (float.class.equals(type))
            return x -> U.getFloat(x, offset);
        if (double.class.equals(type))
            return x -> U.getDouble(x, offset);
        if (boolean.class.equals(type))
            return x -> U.getBoolean(x, offset);
        if (char.class.equals(type))
            return x -> U.getChar(x, offset);
        return x -> U.getObject(x, offset);
    }

    private static Accessor reflectionAccessor(Field field) {
        field.setAccessible(true);
        return x -> field.get(x);
    }

    public long offset() {
        return offset;
    }

    public int declarationOrder() {
        return declarationOrder;
    }

    private void moveToNextAccessor() {
        while (!accessors.isEmpty()) {
            try {
                currentAccessor = Objects.requireNonNull(accessors.pollFirst()).get();
                return;
            } catch (Throwable e) {
                Logger.getLogger(FastFieldAccessor.class.getName())
                        .log(Level.INFO, "Error getting accessor for field '" + name + "'", e);
            }
        }
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

    private interface Accessor {
        Object get(Object obj) throws Exception;
    }
}
