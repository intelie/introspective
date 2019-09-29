package net.intelie.introspective.util;

public interface VisitedSet {
    void clear();

    boolean softClear();

    int enter(Object obj);

    boolean exit(Object obj, int hint);
}
