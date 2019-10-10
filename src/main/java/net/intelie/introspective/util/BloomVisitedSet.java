package net.intelie.introspective.util;

import java.util.Arrays;

public class BloomVisitedSet implements VisitedSet {
    private final long[] table;
    private final int seed;
    private final int k;
    private final int mask;

    public BloomVisitedSet(int m, int k) {
        this(m, k, 0);
    }

    public BloomVisitedSet(int m, int k, int seed) {
        Preconditions.checkArgument(Integer.bitCount(m) == 1, "Table size (%s) must be a power of two", m);
        this.table = new long[m >>> 6];
        this.seed = mix(seed);
        this.mask = (table.length - 1) << 6;
        this.k = k;
    }

    public static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    @Override
    public void clear() {
        Arrays.fill(table, 0);
    }

    @Override
    public boolean softClear() {
        clear();
        return false;
    }

    @Override
    public int enter(Object obj) {
        long[] table = this.table;
        int h = System.identityHashCode(obj);
        int k = this.k;
        int mask = this.mask;

        int answer = -1;

        for (int i = 0; i < k; i++) {
            int h2 = mix(h + i + seed);
            int index = (h2 & mask) >>> 6;
            int lowermask = 1 << (h2 & 63);
            long value = table[index];
            if ((value & lowermask) == 0)
                answer = 1;
            table[index] = (value | lowermask);
        }
        return answer;
    }

    @Override
    public boolean exit(Object obj, int hint) {
        return true;
    }
}
