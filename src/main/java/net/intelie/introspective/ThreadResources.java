package net.intelie.introspective;

import net.intelie.introspective.hotspot.Field;
import net.intelie.introspective.hotspot.JVM;
import net.intelie.introspective.hotspot.Type;
import net.intelie.introspective.hotspot.VMThread;

public abstract class ThreadResources {
    private static final JVM vm;
    private static final Type threadType;
    private static final Type tlabType;
    private static final Field allocatedBytes;
    private static final Field tlab;
    private static final Field tlabTop;
    private static final Field tlabStart;
    private static final boolean validAllocated, validTlab;

    static {
        vm = initJVM();
        threadType = initType("Thread");
        allocatedBytes = initField(threadType, "_allocated_bytes");
        validAllocated = allocatedBytes != null;

        tlabType = initType("ThreadLocalAllocBuffer");
        tlab = initField(threadType, "_tlab");
        tlabStart = initField(tlabType, "_start");
        tlabTop = initField(tlabType, "_top");
        validTlab = tlab != null && tlabStart != null && tlabTop != null;
    }

    private static Field initField(Type type, String name) {
        try {
            return type != null ? type.field(name) : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static Type initType(String name) {
        try {
            return vm != null ? vm.type(name) : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static JVM initJVM() {
        JVM jvm;
        try {
            jvm = new JVM();
        } catch (Throwable e) {
            jvm = null;
        }
        return jvm;
    }


    public static boolean isValidAllocated() {
        return validAllocated;
    }

    public static boolean isValidTlab() {
        return validTlab;
    }

    public static long allocatedBytes(Thread thread) {
        if (!validAllocated) return 0;
        long offset = VMThread.of(thread);
        long allocated = vm.getLong(offset + allocatedBytes.offset);
        if (!validTlab) return allocated;

        long top = vm.getAddress(offset + tlab.offset + tlabTop.offset);
        long start = vm.getAddress(offset + tlab.offset + tlabStart.offset);
        return allocated + (top - start);
    }

}
