package net.intelie.introspective;

import net.intelie.introspective.reflect.*;
import net.intelie.introspective.util.ExpiringVisitedSet;
import net.intelie.introspective.util.VisitedSet;

import java.util.Arrays;

public class ObjectSizer {
    private final StringBuilder builder = new StringBuilder();
    private final ReflectionCache cache;
    private final VisitedSet seen;
    private ReferencePeeler[] stack;
    private int[] stackExit;
    private int index = 0;
    private Object current;
    private ReferencePeeler currentPeeler;
    private boolean hasNextPeeler = false;
    private long bytes;
    private Class<?> type;

    public ObjectSizer() {
        this(new ExpiringVisitedSet(1 << 15));
    }

    public ObjectSizer(VisitedSet seen) {
        this.cache = new ReflectionCache();
        this.seen = seen;
        this.stack = new ReferencePeeler[16];
        this.stackExit = new int[16];
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

        for (ReferencePeeler peelr : stack)
            peelr.clear();

        index = -1;
        currentPeeler = null;
        hasNextPeeler = false;
    }

    public void resetTo(Object obj) {
        seen.softClear();
        index = -1;
        currentPeeler = null;
        if (obj != null) {
            hasNextPeeler = true;
            stack[0].resetTo(null, obj);
        }
    }

    public boolean skipChildren() {
        if (!hasNextPeeler)
            return false;

        //stack[index + 1].clear();
        seen.exit(currentPeeler.current(), stackExit[index]);
        hasNextPeeler = false;
        return true;
    }

    public boolean moveNext() {
        if (hasNextPeeler) {
            currentPeeler = stack[++index];
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

                stackExit[index] = enterIndex;
                checkOverflow();
                hasNextPeeler = true;
                bytes = stack[index + 1].resetTo(currentType, currentObj);
                return true;
            } else {
                ReferencePeeler peeler = currentPeeler = --index >= 0 ? stack[index] : null;
                if (peeler != null)
                    seen.exit(peeler.current(), stackExit[index]);
            }
        }
        return false;
    }

    private void checkOverflow() {
        if (index + 1 >= stack.length) {
            int old = stack.length;
            stack = Arrays.copyOf(stack, stack.length * 2);
            stackExit = Arrays.copyOf(stackExit, stackExit.length * 2);
            for (int i = old; i < stack.length; i++)
                stack[i] = new GenericPeeler(cache);
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
