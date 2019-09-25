package net.intelie.introspective.util;

import java.util.*;

@SuppressWarnings("unchecked")
public class VisitedSet<E> {
    private static final int MULTIPLIER = 4;
    private final long[] gen;
    private final E[] table;
    private final int maxSize;
    private final int mask;

    private long currentMaster;
    private long currentEnter;
    private long currentExit;

    public VisitedSet(int maxSize) {
        Preconditions.checkArgument(Integer.bitCount(maxSize) == 1, "Max size must be a power of two");
        this.maxSize = maxSize;
        this.gen = new long[MULTIPLIER * maxSize];
        this.table = (E[]) new Object[MULTIPLIER * maxSize];
        this.mask = gen.length - 1;
        clear();
    }

    public void clear() {
        currentMaster = -(1L << 32);
        Arrays.fill(gen, currentMaster);
        Arrays.fill(table, null);
        softClear();
    }

    public void softClear() {
        currentMaster += 1L << 32;
        currentEnter = currentMaster + maxSize;
        currentExit = currentMaster + maxSize;
    }

    public boolean enter(Object obj) {
        long cutGen = currentEnter - maxSize;
        if (currentExit <= cutGen) return false;

        int mask = this.mask;
        long[] gen = this.gen;
        E[] table = this.table;
        long currentMaster = this.currentMaster;

        int read = hash(obj) & mask;
        int write = read;

        for (long genRead = gen[read]; genRead >= currentMaster; genRead = gen[read]) {
            if (genRead > cutGen) {
                if (table[read] == obj)
                    return false;
                gen[read] = Long.MIN_VALUE;
                gen[write] = genRead;
                table[write] = table[read];
                write = (write + 1) & mask;
            } else if (genRead == cutGen && table[read] == obj) {
                return false;
            } else {
                gen[read] = Long.MIN_VALUE;
            }
            read = (read + 1) & mask;
        }

        table[write] = (E) obj;
        gen[write] = currentMaster + Integer.MAX_VALUE;
        currentEnter++;
        return true;
    }

    public int hash(Object obj) {
        return System.identityHashCode(obj);
    }

    public boolean contains(Object obj) {
        long cutGen = currentEnter - maxSize;

        int mask = this.mask;
        long[] gen = this.gen;
        E[] table = this.table;
        long currentMaster = this.currentMaster;

        int read = hash(obj) & mask;

        for (long genRead = gen[read]; genRead >= currentMaster; genRead = gen[read]) {
            if (genRead >= cutGen && table[read] == obj)
                return true;
            read = (read + 1) & mask;
        }

        return false;
    }

    public boolean exit(Object obj) {
        int mask = this.mask;
        long[] gen = this.gen;
        E[] table = this.table;
        long currentMaster = this.currentMaster;

        int read = hash(obj) & mask;

        while (gen[read] >= currentMaster) {
            if (table[read] == obj) {
                gen[read] = currentExit++;
                return true;
            }
            read = (read + 1) & mask;
        }

        return false;
    }
}
