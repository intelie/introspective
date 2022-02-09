package net.intelie.introspective.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class UnsafeGetter {
    private static final Unsafe unsafe = tryGetAccessible(Unsafe.class, "theUnsafe");
    private static final MethodHandle objectFieldOffset = tryGetRealMethodHandle(Unsafe.class,
            "theInternalUnsafe", "theUnsafe", "objectFieldOffset");

    public static Unsafe tryGetAccessible(Class<Unsafe> clazz, String fieldName) {
        return AccessController.doPrivileged((PrivilegedAction<Unsafe>) () -> {
                    try {
                        Field unsafe = clazz.getDeclaredField(fieldName);
                        unsafe.setAccessible(true);
                        return (Unsafe) unsafe.get(null);
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
    }

    public static MethodHandle tryGetRealMethodHandle(Class<Unsafe> clazz, String internalFieldName, String fieldName,
                                                      String methodName) {
        return AccessController.doPrivileged((PrivilegedAction<MethodHandle>) () -> {
            try {
                Field field;
                try {
                    field = clazz.getDeclaredField(internalFieldName);
                } catch (NoSuchFieldException e) {
                    field = clazz.getDeclaredField(fieldName);
                }
                field.setAccessible(true);
                Object object = field.get(null);

                return MethodHandles.lookup().findVirtual(object.getClass(), methodName,
                        MethodType.methodType(long.class, Field.class)).bindTo(object);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Unsafe get() {
        return unsafe;
    }

    public static MethodHandle getObjectFieldOffset() {
        return objectFieldOffset;
    }

    public static long objectFieldOffset(Field field) {
        try {
            return (long) objectFieldOffset.invokeExact(field);
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            throw new RuntimeException(throwable);
        }
    }
}
