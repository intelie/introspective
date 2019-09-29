package net.intelie.introspective.reflect;

import java.lang.reflect.Array;

public class ArrayPeeler implements ReferencePeeler {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private Object[] obj;
    private int it;
    private Object current;

    @Override
    public void clear() {
        this.obj = null;
        this.it = 0;
        this.current = null;
    }

    @Override
    public long resetTo(Class<?> clazz, Object value) {
        Class<?> componentType = clazz.getComponentType();

        this.obj = !componentType.isPrimitive() ? (Object[]) value : EMPTY_ARRAY;
        this.it = 0;
        this.current = null;

        return JVMPrimitives.getArrayBaseOffset(clazz) +
                JVMPrimitives.getPrimitive(componentType) * Array.getLength(value);
    }

    @Override
    public boolean moveNext() {
        //doing this way to save field accesses that cost a lot
        int it = this.it;
        Object[] obj = this.obj;
        int count = obj.length;

        while (it < count) {
            Object newObj = obj[it++];
            if (newObj != null) {
                this.it = it;
                this.current = newObj;
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
        return it > 0 ? it - 1 : null;
    }
}

