package net.intelie.introspective;

import net.intelie.introspective.reflect.GenericPeeler;
import net.intelie.introspective.reflect.JVMPrimitives;
import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.util.BloomVisitedSet;
import net.intelie.introspective.util.VisitedSet;

import java.util.ArrayDeque;

public class BloomObjectSizer {
    private final VisitedSet seen;
    private final int maxWidth;
    private final ObjectSizer dfs;
    private final GenericPeeler peeler;
    private final ArrayDeque<Object> queue;
    private long count = 0;
    private long bytes = 0;

    public BloomObjectSizer(ReflectionCache cache, int m, int maxWidth, int maxDepth) {
        this.seen = new BloomVisitedSet(m, 3); //unscientific experiments suggest this is a good K
        this.maxWidth = maxWidth;
        this.dfs = new ObjectSizer(cache, seen, maxDepth);
        this.peeler = new GenericPeeler(cache);
        this.queue = new ArrayDeque<>(maxWidth);
    }

    public void softClear() {
        seen.softClear();
        count = 0;
        bytes = 0;
    }

    public void clear() {
        seen.clear();
        peeler.clear();
        dfs.clear();
        count = 0;
        bytes = 0;
    }

    public long count() {
        return count;
    }

    public long bytes() {
        return bytes;
    }

    public void visit(Object obj) {
        ArrayDeque<Object> queue = this.queue;
        GenericPeeler peeler = this.peeler;
        VisitedSet seen = this.seen;
        ObjectSizer dfs = this.dfs;
        int maxWidth = this.maxWidth;
        long count = 0;
        long bytes = 0;


        if (obj != null)
            queue.add(obj);


        //using a BFS first to give objects higher in the tree a higher change
        //of not being pruned
        while (!queue.isEmpty()) {
            count++;
            Object currentObj = queue.pollFirst();
            Class<?> currentType = currentObj.getClass();

            //the value is a boxed primitive
            long fast = JVMPrimitives.getFastPath(currentType, currentObj);
            if (fast >= 0) {
                bytes += JVMPrimitives.align(fast);
                continue;
            }

            bytes += JVMPrimitives.align(peeler.resetTo(currentType, currentObj));
            while (peeler.moveNext()) {
                Object next = peeler.current();

                if (queue.size() < maxWidth) {
                    if (seen.enter(next) < 0) continue;
                    queue.add(next);
                } else {
                    //fallback to DFS
                    dfs.set(next);
                    while (dfs.moveNext()) {
                        count++;
                        bytes += dfs.bytes();
                    }
                }
            }
        }
        this.count = count;
        this.bytes = bytes;
    }

}
