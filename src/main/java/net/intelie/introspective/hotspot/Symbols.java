package net.intelie.introspective.hotspot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Symbols {
    private static final Method findNative;
    private static final ClassLoader classLoader;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String vmName = System.getProperty("java.vm.name");
            String dll = vmName.contains("Client VM") ? "/bin/client/jvm.dll" : "/bin/server/jvm.dll";
            try {
                System.load(System.getProperty("java.home") + dll);
            } catch (UnsatisfiedLinkError e) {
                throw new IllegalStateException("Cannot find jvm.dll. Unsupported JVM?");
            }
            classLoader = Symbols.class.getClassLoader();
        } else {
            classLoader = null;
        }


        System.out.println(os);
       try {
            findNative = ClassLoader.class.getDeclaredMethod("findNative", ClassLoader.class, String.class);
            findNative.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Method ClassLoader.findNative not found");
        }
    }

    public static long lookup(String name) {
        try {
            return (Long) findNative.invoke(null, classLoader, name);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
