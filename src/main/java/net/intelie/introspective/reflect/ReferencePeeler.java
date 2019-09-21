package net.intelie.introspective.reflect;

public interface ReferencePeeler {
    void clear();

    //returns the shallow size of instance
    long resetTo(Class<?> clazz, Object value);

    boolean moveNext();

    Object current();

    Object currentIndex();
}
