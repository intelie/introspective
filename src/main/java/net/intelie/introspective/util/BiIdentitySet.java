package net.intelie.introspective.util;

import java.util.*;

@SuppressWarnings("unchecked")
public class BiIdentitySet<E> {
    private final int[] gen;
    private final E[] table;
    private int generation;
    private int elements;

    public BiIdentitySet(int size) {
        generation = Integer.MIN_VALUE + 2;
        gen = new int[4 * size];
        Arrays.fill(gen, Integer.MIN_VALUE);
        table = (E[]) new Object[4 * size];
        elements = 0;
    }


    public int size() {
        return elements;
    }

    public boolean contains(Object o) {
        int hash = System.identityHashCode(o);
        int index = (hash & 0x7FFFFFFF) % table.length;
        int offset = 1;

        int invalidGen = generation - 2;

        while (gen[index] > invalidGen &&
                !(System.identityHashCode(table[index]) == hash &&
                        table[index] == o)) {
            index = ((index + offset) & 0x7FFFFFFF) % table.length;
            offset = offset * 2 + 1;

            if (offset == -1)
                offset = 2;
        }

        return table[index] != null;
    }

    public boolean add(Object o) {
        int hash = System.identityHashCode(o);
        int index = (hash & 0x7FFFFFFF) % table.length;
        int offset = 1;

        while (table[index] != null &&
                !(System.identityHashCode(table[index]) == hash &&
                        table[index] == o)) {

            index = ((index + offset) & 0x7FFFFFFF) % table.length;
            offset = offset * 2 + 1;

            if (offset == -1)
                offset = 2;
        }

        if (table[index] == null) { // wasn't present already
            elements++;
            table[index] = (E) o;
            return true;
        } else // was there already
            return false;
    }

    public void clear() {
        elements = 0;
        Arrays.fill(table, null);
    }



}
