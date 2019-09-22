package net.intelie.introspective.reflect;

import net.intelie.introspective.hotspot.JVM;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JVMPrimitives {
    private static final int objectHeaderSize;
    private static final Map<Class, Long> primitive = new IdentityHashMap<>();
    private static final Map<Class, Long> fastPath = new IdentityHashMap<>();
    private static final Map<Class, Long> arrayBaseOffset = new IdentityHashMap<>();
    private static final long oopSize;
    private static final long objectArrayBaseOffset;
    private static final Experiments experiments = Experiments.get();

    static {
        oopSize = experiments.computeOopSize();
        objectHeaderSize = experiments.computeObjectHeaderSize();

        initPrimitive(byte.class, Byte.class, experiments.computeByteSize());
        initPrimitive(short.class, Short.class, experiments.computeShortSize());
        initPrimitive(int.class, Integer.class, experiments.computeIntSize());
        initPrimitive(long.class, Long.class, experiments.computeLongSize());
        initPrimitive(float.class, Float.class, experiments.computeFloatSize());
        initPrimitive(double.class, Double.class, experiments.computeDoubleSize());
        initPrimitive(boolean.class, Boolean.class, experiments.computeBooleanSize());
        initPrimitive(char.class, Character.class, experiments.computeCharSize());

        initBaseOffset(byte[].class, experiments);
        initBaseOffset(short[].class, experiments);
        initBaseOffset(int[].class, experiments);
        initBaseOffset(long[].class, experiments);
        initBaseOffset(float[].class, experiments);
        initBaseOffset(double[].class, experiments);
        initBaseOffset(boolean[].class, experiments);
        initBaseOffset(char[].class, experiments);
        initBaseOffset(Object[].class, experiments);

        objectArrayBaseOffset = arrayBaseOffset.get(Object[].class);
    }

    private static void initPrimitive(Class<?> primitiveClass, Class<?> boxedClass, long bytes) {
        primitive.put(primitiveClass, bytes);
        fastPath.put(boxedClass, (long) JVMPrimitives.objectHeaderSize + bytes);
    }

    private static void initBaseOffset(Class<?> arrayClass, Experiments jvm) {
        arrayBaseOffset.put(arrayClass, (long) jvm.computeArrayBaseOffset(arrayClass));
    }

    public static long getPrimitive(Class<?> clazz) {
        Long answer = primitive.get(clazz);
        if (answer == null) return oopSize;
        return answer;
    }

    public static long getArrayBaseOffset(Class<?> clazz) {
        return arrayBaseOffset.getOrDefault(clazz, objectArrayBaseOffset);
    }

    public static long getFieldOffset(Field field) {
        if (experiments.U == null) return -1;
        return experiments.U.objectFieldOffset(field);
    }

    public static Object getFieldObject(Object target, long offset) {
        return experiments.U.getObject(target, offset);
    }

    public static long getObjectHeaderSize() {
        return objectHeaderSize;
    }

    public static long getOppSize() {
        return oopSize;
    }

    public static Long getFastPath(Class<?> clazz, Object obj) {
        return fastPath.get(clazz);
    }

    public static long align(long v) {
        return v + ((8 - (v & 7)) & 7);
    }

    private static class Experiments {
        private static final Experiments INSTANCE = new Experiments();
        private final Unsafe U;

        private Experiments() {
            U = JVM.unsafe;
        }

        public static Experiments get() {
            return INSTANCE;
        }


        public int computeBooleanSize() {
            return getMinDiff(MyBooleans4.class, 1);
        }

        public int computeByteSize() {
            return getMinDiff(MyBytes4.class, 1);
        }

        public int computeShortSize() {
            return getMinDiff(MyShorts4.class, 2);
        }

        public int computeCharSize() {
            return getMinDiff(MyChars4.class, 2);
        }

        public int computeFloatSize() {
            return getMinDiff(MyFloats4.class, 4);
        }

        public int computeIntSize() {
            return getMinDiff(MyInts4.class, 4);
        }

        public int computeLongSize() {
            return getMinDiff(MyLongs4.class, 8);
        }

        public int computeDoubleSize() {
            return getMinDiff(MyDoubles4.class, 8);
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

        private int getMinDiff(Class<?> klass, int defaultValue) {
            try {
                if (U == null)
                    return defaultValue;
                int off1 = (int) U.objectFieldOffset(klass.getDeclaredField("f1"));
                int off2 = (int) U.objectFieldOffset(klass.getDeclaredField("f2"));
                int off3 = (int) U.objectFieldOffset(klass.getDeclaredField("f3"));
                int off4 = (int) U.objectFieldOffset(klass.getDeclaredField("f4"));
                return Math.min(Math.abs(off2 - off1),
                        Math.min(Math.abs(off3 - off1),
                                Math.min(Math.abs(off4 - off1),
                                        Math.min(Math.abs(off3 - off2),
                                                Math.min(Math.abs(off4 - off2),
                                                        Math.abs(off4 - off3)
                                                )))));
            } catch (Throwable e) {
                Logger.getLogger(Experiments.class.getName())
                        .log(Level.WARNING, "Unable to determine primitive size for " + klass, e);
                return defaultValue;
            }
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

        public static class MyBooleans4 {
            private boolean f1, f2, f3, f4;
        }

        public static class MyBytes4 {
            private byte f1, f2, f3, f4;
        }

        public static class MyShorts4 {
            private short f1, f2, f3, f4;
        }

        public static class MyChars4 {
            private char f1, f2, f3, f4;
        }

        public static class MyInts4 {
            private int f1, f2, f3, f4;
        }

        public static class MyFloats4 {
            private float f1, f2, f3, f4;
        }

        public static class MyLongs4 {
            private long f1, f2, f3, f4;
        }

        public static class MyDoubles4 {
            private double f1, f2, f3, f4;
        }
    }

}
