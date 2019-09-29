package net.intelie.introspective.util;

public interface VisitedSet {
    void clear();

    void softClear();

    int enter(Object obj);

    boolean exit(Object obj, int hint);
}
