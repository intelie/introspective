package net.intelie.introspective.reflect;

import net.intelie.introspective.util.UnsafeGetter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class StringFastPath {
    private final Unsafe U = UnsafeGetter.get();
    private final long coderOffset;
    private final int arrayOffset;
    private final long stringSize;

    public StringFastPath() {
        Field field = null;
        try {
            Field compactStringsField = String.class.getDeclaredField("COMPACT_STRINGS");
            compactStringsField.setAccessible(true);
            if (Boolean.TRUE.equals(compactStringsField.get(null)))
                field = String.class.getDeclaredField("coder");
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        coderOffset = field != null ? U.objectFieldOffset(field) : -1;
        arrayOffset = U.arrayBaseOffset(field != null ? byte[].class : char[].class);
        stringSize = computeStringSize();
    }

    private long computeStringSize() {
        long size = JVMPrimitives.getObjectHeaderSize();
        for (Field field : String.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;

            size = Math.max(size, JVMPrimitives.getFieldOffset(field) + JVMPrimitives.getPrimitive(field.getType()));
        }
        return size;
    }

    public long size(String s) {
        if (coderOffset >= 0) {
            byte coder = U.getByte(s, coderOffset);
            return JVMPrimitives.align(stringSize) + JVMPrimitives.align((s.length() << coder) + arrayOffset);
        } else {
            return JVMPrimitives.align(stringSize) + JVMPrimitives.align(arrayOffset + s.length() * 2);
        }
    }
}
