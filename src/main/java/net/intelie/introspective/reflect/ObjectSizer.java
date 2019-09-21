package net.intelie.introspective.reflect;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class ObjectSizer {
    private final ReflectionCache cache;
    private final StringBuilder builder = new StringBuilder();
    private final List<Slot> Q;
    private final IdentityHashMap<Object, Object> seen = new IdentityHashMap<>();
    private int index = 0;
    private Object current;
    private Slot currentSlot;
    private long bytes;
    private Class<?> type;

    public ObjectSizer() {
        this(new ReflectionCache());
    }

    public ObjectSizer(ReflectionCache cache) {
        this.cache = cache;
        this.Q = new ArrayList<>();
        this.Q.add(new Slot(new ConstantDummyPeeler()));
    }

    public void clear() {
        current = null;
        type = null;
        bytes = 0;
        seen.clear();
        index = 0;
        currentSlot = Q.get(0);
    }

    public void resetTo(Object obj) {
        clear();
        currentSlot.reset(null, obj);
    }

    public boolean moveNext() {
        while (index >= 0) {
            if (currentSlot.moveNext() && seen.put(current = currentSlot.peeler.current(), true) == null) {
                type = current.getClass();

                //the value is a boxed primitive
                Long fast = JVMPrimitives.getFastPath(type, current);
                if (fast != null) {
                    bytes = fast;
                    return true;
                }

                index++;
                if (index >= Q.size())
                    Q.add(currentSlot = new Slot(new GenericPeeler(cache)));
                else
                    currentSlot = Q.get(index);

                bytes = currentSlot.reset(type, this.current);
                return true;
            } else {
                currentSlot.clear();
                --index;
                currentSlot = index >= 0 ? Q.get(index) : null;
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
            Slot slot = Q.get(i);
            if (slot.init) {
                builder.append('.');
                builder.append(slot.peeler.currentIndex());
            }
        }
        return builder.toString();
    }

    private static class Slot {
        private final ReferencePeeler peeler;
        private boolean init;

        private Slot(ReferencePeeler peeler) {
            this.peeler = peeler;
        }

        public void clear() {
            this.init = false;
            this.peeler.clear();
        }

        public long reset(Class<?> type, Object value) {
            this.init = false;
            return this.peeler.resetTo(type, value);
        }

        public boolean moveNext() {
            return init = this.peeler.moveNext();
        }
    }

}
