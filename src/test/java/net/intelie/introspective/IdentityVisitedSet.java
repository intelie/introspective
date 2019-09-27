package net.intelie.introspective;

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
    public void softClear() {
        set.clear();
    }

    @Override
    public int enter(Object obj) {
        return set.put(obj, true) == null ? 1 : -1;
    }

    @Override
    public boolean exit(Object obj, int hint) {
        return true;
    }

    @Override
    public int contains(Object obj) {
        return set.containsKey(obj) ? 1 : -1;
    }
}
