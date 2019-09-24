package net.intelie.introspective.reflect;

import net.intelie.introspective.util.UnsafeGetter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JVMPrimitives {
    private static final int objectHeaderSize;
    private static final long oopSize;
    private static final Unsafe U = UnsafeGetter.get();

    private static final int byteOffset;
    private static final int shortOffset;
    private static final int intOffset;
    private static final int longOffset;
    private static final int floatOffset;
    private static final int doubleOffset;
    private static final int booleanOffset;
    private static final int charOffset;
    private static final int objectOffset;

    private static final StringFastPath stringFastPath;

    static {
        Experiments experiments = Experiments.get();
        oopSize = experiments.computeOopSize();
        objectHeaderSize = experiments.computeObjectHeaderSize();
        byteOffset = experiments.computeArrayBaseOffset(byte[].class);
        shortOffset = experiments.computeArrayBaseOffset(short[].class);
        intOffset = experiments.computeArrayBaseOffset(int[].class);
        longOffset = experiments.computeArrayBaseOffset(long[].class);
        floatOffset = experiments.computeArrayBaseOffset(float[].class);
        doubleOffset = experiments.computeArrayBaseOffset(double[].class);
        booleanOffset = experiments.computeArrayBaseOffset(boolean[].class);
        charOffset = experiments.computeArrayBaseOffset(char[].class);
        objectOffset = experiments.computeArrayBaseOffset(Object[].class);
        stringFastPath = new StringFastPath();
    }

    public static long getFieldOffset(Field field) {
        return U.objectFieldOffset(field);
    }

    public static long getPrimitive(Class<?> clazz) {
        if (clazz == byte.class) return 1;
        if (clazz == short.class) return 2;
        if (clazz == int.class) return 4;
        if (clazz == long.class) return 8;
        if (clazz == float.class) return 4;
        if (clazz == double.class) return 8;
        if (clazz == boolean.class) return 1;
        if (clazz == char.class) return 2;
        return oopSize;
    }


    public static long getArrayBaseOffset(Class<?> clazz) {
        if (clazz == byte[].class) return byteOffset;
        if (clazz == short[].class) return shortOffset;
        if (clazz == int[].class) return intOffset;
        if (clazz == long[].class) return longOffset;
        if (clazz == float[].class) return floatOffset;
        if (clazz == double[].class) return doubleOffset;
        if (clazz == boolean[].class) return booleanOffset;
        if (clazz == char[].class) return charOffset;
        return objectOffset;
    }

    public static long getObjectHeaderSize() {
        return objectHeaderSize;
    }

    public static long getOppSize() {
        return oopSize;
    }

    public static long getFastPath(Class<?> clazz, Object obj) {
        if (clazz == String.class) return stringFastPath.size((String) obj);
        if (clazz == Byte.class) return 12L + 1;
        if (clazz == Short.class) return 12L + 2;
        if (clazz == Integer.class) return 12L + 4;
        if (clazz == Long.class) return 12L + 8;
        if (clazz == Float.class) return 12L + 4;
        if (clazz == Double.class) return 12L + 8;
        if (clazz == Boolean.class) return 12L + 1;
        if (clazz == Character.class) return 12L + 2;
        return -1;
    }

    public static long align(long v) {
        return v + ((8 - (v & 7)) & 7);
    }

    private static class Experiments {
        private static final Experiments INSTANCE = new Experiments();

        public static Experiments get() {
            return INSTANCE;
        }

        public int computeOopSize() {
            return guessOopSize(8);
        }

        public int computeObjectHeaderSize() {
            return 12;
        }

        public int computeArrayBaseOffset(Class<?> klass) {
            return U.arrayBaseOffset(klass);
        }

        private int guessOopSize(int defaultValue) {
            int oopSize;
            try {
                long off1 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj1"));
                long off2 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj2"));
                oopSize = (int) Math.abs(off2 - off1);
            } catch (Throwable e) {
                Logger.getLogger(Experiments.class.getName())
                        .log(Level.WARNING, "Unable to determine oopsize", e);
                return defaultValue;
            }

            return oopSize;
        }


        public static class CompressedOopsClass {
            public Object obj1;
            public Object obj2;
        }
    }
}
