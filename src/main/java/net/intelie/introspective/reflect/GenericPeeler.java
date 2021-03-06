package net.intelie.introspective.reflect;

public class GenericPeeler implements ReferencePeeler {
    private final ObjectPeeler object;
    private final ArrayPeeler array;
    private ReferencePeeler current;

    public GenericPeeler(ReflectionCache cache) {
        this.object = new ObjectPeeler(cache);
        this.array = new ArrayPeeler();
    }

    @Override
    public void clear() {
        current = null;
        object.clear();
        array.clear();
    }

    @Override
    public long resetTo(Class<?> clazz, Object value) {
        current = clazz.isArray() ? array : object;
        return current.resetTo(clazz, value);
    }

    @Override
    public boolean moveNext() {
        return current.moveNext();
    }

    @Override
    public Object current() {
        return current.current();
    }

    @Override
    public Object currentIndex() {
        return current.currentIndex();
    }
}
