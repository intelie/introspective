package net.intelie.introspective.util;

import java.util.*;

@SuppressWarnings("unchecked")
public class VisitedSet<E> {
    public static final long MAX_VALUE_STEP = 1L << 32;
    private final Random random = new Random(0);
    private final int maxSize;
    private final int mask;
    private final int maxEnters;
    private final E[] table;
    private final long[] gen;

    //used only for rehashing
    private final E[] tempTable;
    private final long[] tempGen;

    private long maxValue = 0;
    private int untilRehash;
    private long currentEnter;
    private long currentExit;
    private int seed;

    public static long TOTAL = 0;

    public VisitedSet(int maxSize) {
        Preconditions.checkArgument(Integer.bitCount(maxSize) == 1, "Max size must be a power of two");
        this.maxSize = maxSize;
        this.table = (E[]) new Object[4 * maxSize];
        this.gen = new long[4 * maxSize];

        this.tempTable = (E[]) new Object[maxSize];
        this.tempGen = new long[maxSize];
        this.mask = gen.length - 1;
        this.maxEnters = table.length / 4;
        clear();
    }

    public void clear() {
        clearGen();
        Arrays.fill(tempTable, null);
        Arrays.fill(table, null);
        seed = random.nextInt();
        maxValue = MAX_VALUE_STEP;
    }

    private void clearGen() {
        Arrays.fill(gen, Long.MIN_VALUE);
        currentEnter = currentExit = 0;
        untilRehash = maxEnters;
    }

    public void softClear() {
        if (currentEnter > Long.MAX_VALUE / 2) {
            clear();
        } else {
            currentEnter = currentExit = maxValue;
            seed = random.nextInt();
            maxValue += MAX_VALUE_STEP;
        }
    }

    private int findSpot(Object obj, long cutGen) {
        int index = hash(obj) & this.mask;
        int count = 0;
        int first = -1;

        for (long genRead = gen[index]; genRead > Long.MIN_VALUE; genRead = gen[index]) {
            count++;
            if (genRead > cutGen) {
                if (table[index] == obj)
                    return ~index;
            } else if (first < 0) {
                first = index;
            }
            index = (index + 1) & this.mask;
        }

        TOTAL += count;
        return first >= 0 ? first : index;
    }

    public int enter(Object obj) {
        long cutGen = currentEnter - maxSize;
        if (currentExit <= cutGen) return Integer.MIN_VALUE;

        int index = findSpot(obj, cutGen);
        if (index < 0) return index;

        if (gen[index] == Long.MIN_VALUE)
            untilRehash--;

        table[index] = (E) obj;
        gen[index] = maxValue;
        currentEnter++;
        if (untilRehash == 0 || currentEnter == maxValue)
            rehash();
        return index;
    }

    public boolean exit(Object obj, int hint) {
        int index = table[hint] == obj && gen[hint] > currentEnter - maxSize ? hint :
                ~findSpot(obj, currentEnter - maxSize);
        if (index < 0) return false;
        gen[index] = ++currentExit;
        return true;
    }

    public boolean contains(Object obj) {
        return findSpot(obj, currentEnter - maxSize) < 0;
    }

    private void rehash() {
        long cutGen = currentEnter - maxSize;
        int count = 0;
        int exits = 0;

        for (int i = 0; i < table.length; i++) {
            long geni = gen[i];
            if (geni > cutGen) {
                tempTable[count] = table[i];
                if (geni == maxValue) {
                    tempGen[count] = maxValue + MAX_VALUE_STEP;
                } else {
                    tempGen[count] = geni - cutGen;
                    exits++;
                }
                count++;
            }
        }
        clearGen();
        for (int i = 0; i < count; i++) {
            E obj = tempTable[i];
            int index = findSpot(obj, currentEnter);
            table[index] = obj;
            gen[index] = tempGen[i];
            untilRehash--;
        }
        currentEnter = count;
        currentExit = exits;
        //System.out.println("REHASH END");
    }

    public int hash(Object obj) {
        return System.identityHashCode(obj) ^ seed;
    }
}
