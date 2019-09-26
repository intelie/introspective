package net.intelie.introspective;

import net.intelie.introspective.reflect.*;
import net.intelie.introspective.util.VisitedSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ObjectSizer {
    private final StringBuilder builder = new StringBuilder();
    private final ReflectionCache cache;
    private final VisitedSet<Object> seen;
    private ReferencePeeler[] Q;
    private int[] Qindex;
    private int index = 0;
    private Object current;
    private ReferencePeeler currentPeeler;
    private boolean hasNextPeeler = false;
    private long bytes;
    private Class<?> type;

    public ObjectSizer() {
        this(new ReflectionCache(), 1<<24);
    }

    public ObjectSizer(ReflectionCache cache, int maxRecentlySeen) {
        this.cache = cache;
        this.seen = new VisitedSet<>(maxRecentlySeen);
        this.Q = new ReferencePeeler[16];
        this.Qindex = new int[16];
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

        while (index >= 0)
            Q[index--].clear();
        currentPeeler = null;
        hasNextPeeler = false;
    }

    public void resetTo(Object obj) {
        seen.softClear();
        index = -1;
        currentPeeler = null;
        if (obj != null) {
            hasNextPeeler = true;
            Q[0].resetTo(null, obj);
        }
    }

    public boolean skipChildren() {
        if (!hasNextPeeler)
            return false;

        Q[index + 1].clear();
        hasNextPeeler = false;
        return true;
    }

    public boolean moveNext() {
        if (hasNextPeeler) {
            currentPeeler = Q[++index];
            hasNextPeeler = false;
        }

        while (currentPeeler != null) {
            if (currentPeeler.moveNext()) {
                Object currentObj = this.current = currentPeeler.current();
                int enterIndex = seen.enter(currentObj);
                if (enterIndex < 0)
                    continue;

                Class<?> currentType = this.type = currentObj.getClass();

                //the value is a boxed primitive
                long fast = JVMPrimitives.getFastPath(currentType, currentObj);
                if (fast >= 0) {
                    bytes = fast;
                    seen.exit(currentObj, enterIndex);
                    return true;
                }

                checkOverflow();
                hasNextPeeler = true;
                bytes = Q[index + 1].resetTo(currentType, currentObj);
                Qindex[index + 1] = enterIndex;
                return true;
            } else {
                Q[index].clear();
                ReferencePeeler peeler = currentPeeler = --index >= 0 ? Q[index] : null;
                if (peeler != null)
                    seen.exit(peeler.current(), Qindex[index]);
            }
        }
        return false;
    }

    private void checkOverflow() {
        if (index + 1 >= Q.length) {
            int old = Q.length;
            Q = Arrays.copyOf(Q, Q.length * 2);
            Qindex = Arrays.copyOf(Qindex, Qindex.length * 2);
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

    public Object pathSegment(int index) {
        return Q[index + 1].currentIndex();
    }

    public String path() {
        builder.setLength(0);
        for (int i = 0; i < index; i++) {
            Object index = pathSegment(i);
            if (index instanceof String) {
                builder.append('.').append(index);
            } else {
                builder.append('[').append(index).append(']');
            }
        }
        return builder.toString();
    }
}
