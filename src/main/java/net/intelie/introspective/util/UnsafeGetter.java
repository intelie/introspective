package net.intelie.introspective.util;

import sun.misc.Unsafe;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UnsafeGetter {
    private static Unsafe unsafe = AccessController.doPrivileged((PrivilegedAction<Unsafe>) () -> {
                try {
                    java.lang.reflect.Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    unsafe.setAccessible(true);
                    return (Unsafe) unsafe.get(null);
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
    );

    public static Unsafe get() {
        return unsafe;
    }
}
