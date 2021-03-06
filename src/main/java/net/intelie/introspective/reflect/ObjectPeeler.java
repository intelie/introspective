package net.intelie.introspective.reflect;

public class ObjectPeeler implements ReferencePeeler {
    private final ReflectionCache cache;
    private ReflectionCache.Item cached;
    private Object obj;
    private int it;
    private int count;

    private Object current;

    public ObjectPeeler(ReflectionCache cache) {
        this.cache = cache;
    }

    @Override
    public void clear() {
        this.cached = null;
        this.obj = null;
        this.it = 0;
        this.count = 0;
        this.current = null;
    }

    @Override
    public long resetTo(Class<?> clazz, Object value) {
        this.cached = cache.get(clazz);
        this.obj = value;
        this.it = 0;
        this.current = null;
        this.count = cached.fieldCount();
        return cached.size();

    }

    @Override
    public boolean moveNext() {
        //doing this way to save field accesses that cost a lot
        int count = this.count;
        int it = this.it;
        Object thisObj = this.obj;
        ReflectionCache.Item cached = this.cached;

        while (it < count) {
            Object obj = cached.value(thisObj, it++);
            if (obj != null) {
                this.current = obj;
                this.it = it;
                return true;
            }
        }
        this.it = it;
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

