package net.intelie.introspective;

import net.intelie.introspective.reflect.*;
import net.intelie.introspective.util.ExpiringVisitedSet;
import net.intelie.introspective.util.VisitedSet;

public class ObjectSizer {
    private final StringBuilder builder = new StringBuilder();
    private final ReflectionCache cache;
    private final VisitedSet seen;
    private final ReferencePeeler[] stack;
    private final int[] stackExit;
    private final int maxDepth;
    private int index = 0;
    private Object current;
    private ReferencePeeler currentPeeler;
    private boolean hasNextPeeler = false;
    private long bytes;
    private Class<?> type;

    public ObjectSizer() {
        this(new ReflectionCache(), new ExpiringVisitedSet(1 << 15), 1 << 15);
    }

    public ObjectSizer(ReflectionCache cache, VisitedSet seen, int maxDepth) {
        this.cache = cache;
        this.seen = seen;
        this.stack = new ReferencePeeler[maxDepth];
        this.stackExit = new int[maxDepth];
        this.maxDepth = maxDepth;
        stack[0] = new ConstantDummyPeeler();
        for (int i = 1; i < stack.length; i++) {
            stack[i] = new GenericPeeler(cache);
        }
    }

    public void clear() {
        current = null;
        type = null;
        bytes = 0;
        seen.clear();
        cache.clear();

        for (ReferencePeeler peelr : stack)
            peelr.clear();

        index = -1;
        currentPeeler = null;
        hasNextPeeler = false;
    }

    public void resetTo(Object obj) {
        seen.softClear();
        set(obj);
    }

    public void set(Object obj) {
        index = -1;
        currentPeeler = null;
        hasNextPeeler = true;
        stack[0].resetTo(null, obj);
    }

    public boolean skipChildren() {
        if (!hasNextPeeler)
            return false;

        seen.exit(currentPeeler.current(), stackExit[index]);
        hasNextPeeler = false;
        return true;
    }

    public boolean moveNext() {
        VisitedSet seen = this.seen;
        ReferencePeeler[] stack = this.stack;
        int index = this.index;
        ReferencePeeler peeler = hasNextPeeler ? stack[++index] : currentPeeler;

        while (true) {
            if (peeler.moveNext()) {
                Object currentObj = peeler.current();
                int enterIndex = seen.enter(currentObj);
                if (enterIndex < 0)
                    continue;

                this.currentPeeler = peeler;
                this.current = currentObj;
                this.index = index;
                Class<?> currentType = this.type = currentObj.getClass();

                //the value is a boxed primitive
                long fast = JVMPrimitives.getFastPath(currentType, currentObj);
                if (fast >= 0) {
                    this.bytes = fast;
                    this.hasNextPeeler = false;
                    seen.exit(currentObj, enterIndex);
                    return true;
                }

                bytes = stack[index + 1].resetTo(currentType, currentObj);
                if (index + 2 < maxDepth) {
                    stackExit[index] = enterIndex;
                    hasNextPeeler = true;
                } else {
                    seen.exit(currentObj, enterIndex);
                }
                return true;
            } else {
                if (--index < 0) return false;
                peeler = stack[index];
                seen.exit(peeler.current(), stackExit[index]);
            }
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
        return stack[index + 1].currentIndex();
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
