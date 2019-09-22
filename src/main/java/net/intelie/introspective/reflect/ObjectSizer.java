package net.intelie.introspective.reflect;

import java.util.IdentityHashMap;

public class ObjectSizer {
    private final ReflectionCache cache;
    private final StringBuilder builder = new StringBuilder();
    private final ReferencePeeler[] Q;
    private final IdentityHashMap<Object, Object> seen = new IdentityHashMap<>();
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
        this.Q = new ReferencePeeler[10000];
        Q[0] = new ConstantDummyPeeler();
        for (int i = 1; i < 1000; i++) {
            Q[i] = new GenericPeeler(cache);
        }
    }

    public void clear() {
        current = null;
        type = null;
        bytes = 0;
        seen.clear();
        index = 0;
        currentPeeler = Q[0];
    }

    public void resetTo(Object obj) {
        clear();
        currentPeeler.resetTo(null, obj);
    }

    public boolean moveNext() {
        while (index >= 0) {
            if (currentPeeler.moveNext() && seen.put(current = currentPeeler.current(), true) == null) {
                type = current.getClass();

                //the value is a boxed primitive
                Long fast = JVMPrimitives.getFastPath(type, current);
                if (fast != null) {
                    bytes = fast;
                    return true;
                }
                currentPeeler = Q[++index];
                bytes = currentPeeler.resetTo(type, this.current);
                return true;
            } else {
                currentPeeler = --index >= 0 ? Q[index] : null;
            }
        }
        return false;
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
            ReferencePeeler peeler = Q[i];
            builder.append('.');
            builder.append(peeler.currentIndex());
        }
        return builder.toString();
    }

}
