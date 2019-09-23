package net.intelie.introspective.reflect;

public class ObjectPeeler implements ReferencePeeler {
    private final ReflectionCache cache;
    private ReflectionCache.Item cached;
    private Object obj;
    private int it;

    private Object current;

    public ObjectPeeler(ReflectionCache cache) {
        this.cache = cache;
    }

    @Override
    public void clear() {
        this.cached = null;
        this.obj = null;
        this.it = 0;
        this.current = null;
    }

    @Override
    public long resetTo(Class<?> clazz, Object value) {
        this.cached = cache.get(clazz);
        this.obj = value;
        this.it = 0;
        this.current = null;
        return cached.size();

    }

    @Override
    public boolean moveNext() {
        int fieldCount = cached.fieldCount();
        while (it < fieldCount) {
            current = cached.value(obj, it++);
            if (current != null)
                return true;
        }
        return false;
    }

    @Override
    public Object current() {
        return current;
    }

    @Override
    public Object currentIndex() {
        return it > 0 ? cached.fieldName(it - 1) : null;
    }
}

