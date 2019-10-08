package net.intelie.introspective.util;

import net.intelie.introspective.util.VisitedSet;

import java.util.IdentityHashMap;

public class IdentityVisitedSet implements VisitedSet {
    private final IdentityHashMap<Object, Object> set;
    private final int maxDepth;
    private int depth = 0;

    public IdentityVisitedSet(int maxDepth) {
        this.maxDepth = maxDepth;
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
        if (depth >= maxDepth)
            return -1;
        depth++;
        return set.put(obj, true) == null ? 1 : -1;
    }

    @Override
    public boolean exit(Object obj, int hint) {
        depth--;
        return true;
    }
}
