package org.openjdk.jol.vm;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class LightVM extends HotspotUnsafe {
    private static Unsafe tryUnsafe() {
        return AccessController.doPrivileged(
                (PrivilegedAction<Unsafe>) () -> {
                    try {
                        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
                        unsafe.setAccessible(true);
                        return (Unsafe) unsafe.get(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
    }

    private static LightVM INSTANCE;

    public static LightVM current() {
        String name = System.getProperty("java.vm.name");
        if (!name.contains("HotSpot") && !name.contains("OpenJDK")) {
            throw new IllegalStateException("Only HotSpot/OpenJDK VMs are supported");
        }

        Unsafe u = tryUnsafe();
        if (u == null) {
            throw new IllegalStateException("Unsafe is not available.");
        }

        if (INSTANCE == null) {
            LightVM vm = new LightVM(u);
            try {
                Field field = VM.class.getDeclaredField("INSTANCE");
                field.setAccessible(true);
                field.set(null, vm);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
            INSTANCE = vm;
        }
        return INSTANCE;
    }

    public boolean isCompressedOopsEnabled() {
        return VMOptions.pollCompressedOops();
    }

    LightVM(Unsafe u) {
        super(u, null);
    }


}
