package net.intelie.introspective.reflect;

import java.util.Arrays;
import java.util.IdentityHashMap;

public class ObjectSizer {
    private final StringBuilder builder = new StringBuilder();
    private final IdentityHashMap<Object, Object> seen = new IdentityHashMap<>();
    private final ReflectionCache cache;
    private ReferencePeeler[] Q;
    private int index = 0;
    private Object current;
    private ReferencePeeler currentPeeler;
    private long bytes;
    private Class<?> type;

    public ObjectSizer() {
        this(new ReflectionCache());
    }

    public ObjectSizer(ReflectionCache cache) {
        this.cache = cache;
        this.Q = new ReferencePeeler[16];
        Q[0] = new ConstantDummyPeeler();
        for (int i = 1; i < Q.length; i++) {
            Q[i] = new GenericPeeler(cache);
        }
    }

    public void clear() {
        current = null;
        type = null;
        bytes = 0;
        seen.clear();

        while (index > 0)
            Q[index--].clear();
        index = 0;
        currentPeeler = Q[0];
        currentPeeler.clear();
    }

    public void resetTo(Object obj) {
        clear();
        currentPeeler.resetTo(null, obj);
    }

    public boolean moveNext() {
        while (index >= 0) {
            if (currentPeeler.moveNext()) {
                current = currentPeeler.current();
                if (seen.put(current, true) != null)
                    continue;
                type = current.getClass();

                //the value is a boxed primitive
                Long fast = JVMPrimitives.getFastPath(type);
                if (fast != null) {
                    bytes = fast;
                    return true;
                }

                index++;
                checkOverflow();

                currentPeeler = Q[index];
                bytes = currentPeeler.resetTo(type, this.current);
                return true;
            } else {
                Q[index].clear();
                currentPeeler = --index >= 0 ? Q[index] : null;
            }
        }
        return false;
    }

    private void checkOverflow() {
        if (index >= Q.length) {
            int old = Q.length;
            Q = Arrays.copyOf(Q, Q.length * 2);
            for (int i = old; i < Q.length; i++)
                Q[i] = new GenericPeeler(cache);
        }
    }

    public int visitDepth() {
        return index;
    }

    public Object current() {
        return current;
    }

    public Class<?> type() {
        return type;
    }

    public long bytes() {
        return JVMPrimitives.align(bytes);
    }

    public long unalignedBytes() {
        return bytes;
    }

    public String path() {
        builder.setLength(0);
        for (int i = 1; i <= index; i++) {
            Object index = Q[i].currentIndex();
            if (index != null) {
                builder.append('.');
                builder.append(index);
            }
        }
        return builder.toString();
    }

}
