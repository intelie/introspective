package net.intelie.introspective.util;

import net.intelie.introspective.util.VisitedSet;

import java.util.IdentityHashMap;

public class IdentityVisitedSet implements VisitedSet {
    private final IdentityHashMap<Object, Object> set;

    public IdentityVisitedSet() {
        this.set = new IdentityHashMap<>();
    }

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public boolean softClear() {
        set.clear();
        return false;
    }

    @Override
    public int enter(Object obj) {
        return set.put(obj, true) == null ? 1 : -1;
    }

    @Override
    public boolean exit(Object obj, int hint) {
        return true;
    }
}
