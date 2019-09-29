package net.intelie.introspective.util;

import sun.misc.Unsafe;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class UnsafeGetter {
    private static Unsafe unsafe = tryGetAccessible(Unsafe.class, "theUnsafe");

    public static Unsafe tryGetAccessible(Class<Unsafe> clazz, String fieldName) {
        return AccessController.doPrivileged((PrivilegedAction<Unsafe>) () -> {
                    try {
                        java.lang.reflect.Field unsafe = clazz.getDeclaredField(fieldName);
                        unsafe.setAccessible(true);
                        return (Unsafe) unsafe.get(null);
                    } catch (Throwable e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
    }


    public static Unsafe get() {
        return unsafe;
    }
}
