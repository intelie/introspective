package net.intelie.introspective.hotspot;

public class VMThread {
    private static final java.lang.reflect.Field eetop;

    static {
        try {
            eetop = Thread.class.getDeclaredField("eetop");
            eetop.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Thread.eetop field not found");
        }
    }

    public static long of(Thread javaThread) {
        try {
            return eetop.getLong(javaThread);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
