package net.intelie.introspective.util;

import java.util.Arrays;

public class ExpiringVisitedSet implements VisitedSet {
    private final int requiredSize;
    private final int rehashThreshold;
    private final int mask;
    private final Object[] table;
    private final long[] gen;
    //used only for rehashing
    private final Object[] tempTable;
    private final long[] tempGen;

    public long DEBUG_COLLISIONS = 0;
    public long DEBUG_REHASHES = 0;
    public long DEBUG_REHASHES_TIME = 0;
    public long DEBUG_EXIT_MISS = 0;

    private long minValue;
    private long maxValue;
    private long currentEnter;
    private long currentExit;

    public ExpiringVisitedSet(int requiredSize) {
        this(requiredSize, 3 * requiredSize, 4 * requiredSize);
    }

    public ExpiringVisitedSet(int requiredSize, int rehashThreshold, int tableSize) {
        Preconditions.checkArgument(requiredSize <= rehashThreshold, "Rehash threshold must be at least as large as required size");
        Preconditions.checkArgument(rehashThreshold < tableSize, "Table size must be stricly greater than rehash threshold");
        Preconditions.checkArgument(Integer.bitCount(tableSize) == 1, "Table size must be a power of two");
        this.requiredSize = requiredSize;
        this.rehashThreshold = rehashThreshold;
        this.table = new Object[tableSize];
        this.gen = new long[tableSize];

        this.tempTable = new Object[requiredSize];
        this.tempGen = new long[requiredSize];
        this.mask = tableSize - 1;
        clearGen();
    }

    @Override
    public void clear() {
        clearGen();
        Arrays.fill(tempTable, null);
        Arrays.fill(table, null);
    }

    private void clearGen() {
        Arrays.fill(gen, currentEnter = currentExit = minValue = Long.MIN_VALUE);
        maxValue = minValue + rehashThreshold;  //new maximum
    }

    @Override
    public void softClear() {
        if (maxValue > Long.MAX_VALUE - rehashThreshold) {
            clear();
        } else {
            currentEnter = currentExit = minValue = maxValue; //reset minima to latest maximum
            maxValue = minValue + rehashThreshold;  //new maximum
        }
    }

    private int findIndex(Object obj) {
        //this code is repeated in enter() for speed
        int index = System.identityHashCode(obj) & mask;

        int count = 0;
        while (gen[index] > minValue) {
            if (table[index] == obj)
                return ~index;

            //quadratic probing
            count++;
            index = (index + count * count) & mask;
        }

        DEBUG_COLLISIONS += count;
        return index;
    }

    @Override
    public int enter(Object obj) {
        if (currentExit + requiredSize <= currentEnter)
            return Integer.MIN_VALUE;         //queue is full

        //same code as in findIndex(), inlined here for performance
        int index = System.identityHashCode(obj) & mask;

        int count = 0;
        while (gen[index] > minValue) {
            if (table[index] == obj)
                return ~index;
            //quadratic probing
            count++;
            index = (index + count * count) & mask;
        }

        DEBUG_COLLISIONS += count;
        table[index] = obj;
        gen[index] = maxValue;
        if (++currentEnter >= maxValue)
            rehash();
        return index;
    }

    @Override
    public boolean exit(Object obj, int index) {
        if (gen[index] <= minValue || table[index] != obj) {
            DEBUG_EXIT_MISS++;
            index = ~findIndex(obj);
            if (index < 0) return false;
        }
        gen[index] = ++currentExit;
        return true;
    }

    @Override
    public int contains(Object obj) {
        return ~findIndex(obj);
    }

    private void rehash() {
        DEBUG_REHASHES++;
        long DEBUG_START = System.nanoTime();

        //we only keep the "requiredSize" most recent items after rehash
        long cutGen = Math.max(currentEnter - requiredSize, minValue);
        //not checking underflow here because rehash is only called if currentEnter == maxValue
        //and maxValue >= MIN+rehashThreshold, which is >= requiredSize, so currentEnter >= MIN+requiredSize
        assert cutGen <= currentEnter;
        //trust but verify

        int count = 0;
        int exits = 0;

        for (int i = 0; i < table.length; i++) {
            long geni = gen[i];
            if (geni > cutGen) {
                tempTable[count] = table[i];

                //we will reset generation numbers in clearGen, so
                //we have to recompute object generations accordingly
                if (geni == maxValue) {
                    tempGen[count] = rehashThreshold;
                } else {
                    tempGen[count] = geni - cutGen;
                    exits++;
                }

                count++;
            }
        }
        assert currentEnter - currentExit <= count && count <= currentEnter - cutGen;

        clearGen();
        for (int i = 0; i < count; i++) {
            Object obj = tempTable[i];
            int index = findIndex(obj);
            table[index] = obj;
            gen[index] = minValue + tempGen[i];
        }
        currentEnter = minValue + count;
        currentExit = minValue + exits;

        DEBUG_REHASHES_TIME += System.nanoTime() - DEBUG_START;
    }

}
