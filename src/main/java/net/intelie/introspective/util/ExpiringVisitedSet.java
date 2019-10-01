package net.intelie.introspective.util;

import java.util.Arrays;

public class ExpiringVisitedSet implements VisitedSet {
    private final int requiredSize;
    private final int rehashThreshold;
    private final int mask;
    private final Object[] table;
    private final int[] gen;
    //used only for rehashing
    private final Object[] tempTable;
    private final int[] tempGen;

    public long DEBUG_COLLISIONS = 0;
    public long DEBUG_REHASHES = 0;
    public long DEBUG_REHASHES_TIME = 0;
    public long DEBUG_HARDCLEARS = 0;
    public long DEBUG_HARDCLEARS_TIME = 0;
    public long DEBUG_EXIT_MISS = 0;

    private int minValue;
    private int maxValue;
    private int currentEnter;
    private int currentExit;

    public ExpiringVisitedSet(int requiredSize) {
        this(requiredSize, 3 * requiredSize, 4 * requiredSize);
    }

    public ExpiringVisitedSet(int requiredSize, int rehashThreshold, int tableSize) {
        Preconditions.checkArgument(0 <= requiredSize, "Required size (%d) must be non-negative", requiredSize);
        Preconditions.checkArgument(requiredSize <= rehashThreshold,
                "Rehash threshold (%s) must be at least as large as required size (%s)", rehashThreshold, requiredSize);
        Preconditions.checkArgument(rehashThreshold < tableSize,
                "Table size (%s) must be stricly greater than rehash threshold (%s)", tableSize, rehashThreshold);
        Preconditions.checkArgument(Integer.bitCount(tableSize) == 1, "Table size (%s) must be a power of two",
                tableSize);
        this.requiredSize = requiredSize;
        this.rehashThreshold = rehashThreshold;
        this.table = new Object[tableSize];
        this.gen = new int[tableSize];

        this.tempTable = new Object[requiredSize];
        this.tempGen = new int[requiredSize];
        this.mask = tableSize - 1;
        clearGen();
    }

    @Override
    public int maxDepth() {
        return requiredSize;
    }

    @Override
    public void clear() {
        clearGen();
        Arrays.fill(tempTable, null);
        Arrays.fill(table, null);
    }

    private void clearGen() {
        Arrays.fill(gen, currentEnter = currentExit = minValue = Integer.MIN_VALUE);
        maxValue = minValue + rehashThreshold;  //new maximum
    }

    @Override
    public boolean softClear() {
        if (maxValue > Integer.MAX_VALUE - rehashThreshold) {
            DEBUG_HARDCLEARS++;
            long DEBUG_START = System.nanoTime();
            clearGen();
            DEBUG_HARDCLEARS_TIME += System.nanoTime() - DEBUG_START;
            return false;
        } else {
            currentEnter = currentExit = minValue = maxValue; //reset minima to latest maximum
            maxValue = minValue + rehashThreshold;  //new maximum
            return true;
        }
    }

    private int findIndex(Object obj) {
        //this code is repeated in enter() for speed
        int index = System.identityHashCode(obj) & mask;

        int collisions = 0;
        while (gen[index] > minValue) {
            if (table[index] == obj)
                return ~index;
            //quadratic probing
            collisions++;
            index = (index + collisions) & mask;
        }

        DEBUG_COLLISIONS += collisions;
        return index;
    }

    @Override
    public int enter(Object obj) {
        if (currentExit + requiredSize <= currentEnter)
            return Integer.MIN_VALUE;         //queue is full

        //same code as in findIndex(), inlined here for performance
        int index = System.identityHashCode(obj) & mask;

        int collisions = 0;
        while (gen[index] > minValue) {
            if (table[index] == obj)
                return ~index;
            //quadratic probing
            collisions++;
            index = (index + collisions) & mask;
        }

        DEBUG_COLLISIONS += collisions;
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

    public int contains(Object obj) {
        return ~findIndex(obj);
    }

    private void rehash() {
        DEBUG_REHASHES++;
        long DEBUG_START = System.nanoTime();

        //we only keep the "requiredSize" most recent items after rehash
        int cutGen = Math.max(currentEnter - requiredSize, minValue);
        //not checking underflow here because rehash is only called if currentEnter == maxValue
        //and maxValue >= MIN+rehashThreshold, which is >= requiredSize, so currentEnter >= MIN+requiredSize
        assert cutGen <= currentEnter;
        //trust but verify

        int count = 0;
        int exits = 0;

        for (int i = 0; i < table.length; i++) {
            int geni = gen[i];
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
