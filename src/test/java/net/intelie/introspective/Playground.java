package net.intelie.introspective;

import net.intelie.introspective.reflect.ObjectPeeler;
import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.util.BloomVisitedSet;
import net.intelie.introspective.util.ExpiringVisitedSet;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.IntStream;

@Ignore
public class Playground {
    @Test
    public void testSmallObject() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), set, 1 << 15);
        Map test = new HashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        testSizer(sizer, test, 10000000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
        System.out.println("Hard clears: " + set.DEBUG_HARDCLEARS);
        System.out.println("Hard clears (time): " + set.DEBUG_HARDCLEARS_TIME / 1e9);
        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);
    }

    @Test
    public void testSmallObjectBfs() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 10, 1 << 15, 1 << 15);
        Map test = new HashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        testBfsSizer(sizer, test, 10000000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
        System.out.println("Hard clears: " + set.DEBUG_HARDCLEARS);
        System.out.println("Hard clears (time): " + set.DEBUG_HARDCLEARS_TIME / 1e9);
        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);
    }

    @Test
    public void testLargeObject() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), new BloomVisitedSet(1 << 24, 3), 1 << 15);

        Object[] objs = IntStream.range(0, 10000).mapToObj(x -> {
            Map test = new HashMap();
            test.put(111 + x * 10000, Arrays.asList("aaa" + x, 222 + x * 10000));
            test.put(333.0 + x * 10000, Collections.singletonMap("bbb" + x, 444 + x * 10000));
            return test;
        }).toArray(Object[]::new);

        testSizer(sizer, objs, 1000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
        System.out.println("Hard clears: " + set.DEBUG_HARDCLEARS);
        System.out.println("Hard clears (time): " + set.DEBUG_HARDCLEARS_TIME / 1e9);
        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);

    }

    @Test
    public void testLargeObjectBfs() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 16);
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 20, 1 << 15, 1 << 15);

        Object[] objs = IntStream.range(0, 10000).mapToObj(x -> {
            Map test = new HashMap();
            test.put(111 + x * 10000, Arrays.asList("aaa" + x, 222 + x * 10000));
            test.put(333.0 + x * 10000, Collections.singletonMap("bbb" + x, 444 + x * 10000));
            return test;
        }).toArray(Object[]::new);

        testBfsSizer(sizer, objs, 1000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
        System.out.println("Hard clears: " + set.DEBUG_HARDCLEARS);
        System.out.println("Hard clears (time): " + set.DEBUG_HARDCLEARS_TIME / 1e9);
        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);

    }

    private void testSizer(ObjectSizer sizer, Object test, int measureCount) {
        for (int i = 0; i < measureCount / 100; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) ;
        }

        long start = System.nanoTime();
        long memStart = ThreadResources.allocatedBytes(Thread.currentThread());
        long total = 0;
        for (int i = 0; i < measureCount; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) total += 1;
        }
        long memEnd = ThreadResources.allocatedBytes(Thread.currentThread());
        System.out.println("Allocation: " + (memEnd - memStart));
        System.out.println("Total: " + total);
        long end = System.nanoTime();
        System.out.println("Time: " + (end - start) / 1e9);
        System.out.println("Objects/s: " + (long) (total * 1e9 / (end - start)));
    }

    private void testBfsSizer(BloomObjectSizer sizer, Object test, int measureCount) {
        for (int i = 0; i < measureCount / 100; i++) {
            sizer.visit(test);
        }

        long start = System.nanoTime();
        long memStart = ThreadResources.allocatedBytes(Thread.currentThread());
        long total = 0;
        for (int i = 0; i < measureCount; i++) {
            sizer.clear();
            sizer.visit(test);
            total += sizer.count();
        }
        long memEnd = ThreadResources.allocatedBytes(Thread.currentThread());
        System.out.println("Allocation: " + (memEnd - memStart));
        System.out.println("Total: " + total);
        long end = System.nanoTime();
        System.out.println("Time: " + (end - start) / 1e9);
        System.out.println("Objects/s: " + (long) (total * 1e9 / (end - start)));
    }

    @Test
    public void testVisitedSet() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 16);

        Object[] objs = IntStream.range(0, 1000).mapToObj(x -> new Object()).toArray();

        for (int i = 0; i < 100000; i++) {
            set.softClear();
            for (int j = 0; j < 13; j++) {
                int index = set.enter(objs[j]);
                set.exit(objs[j], index);
            }
        }

        long start = System.nanoTime();
        long memStart = ThreadResources.allocatedBytes(Thread.currentThread());

        for (int i = 0; i < 10000000; i++) {
            set.softClear();
            for (int j = 0; j < 13; j++) {
                int index = set.enter(objs[j]);
                //set.exit(objs[j], index);
            }
        }
        long memEnd = ThreadResources.allocatedBytes(Thread.currentThread());

        System.out.println("Allocation: " + (memEnd - memStart));
        System.out.println("Time: " + (System.nanoTime() - start) / 1e9);
//        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
//        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
//        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
//        System.out.println("Hard clears: " + set.DEBUG_HARDCLEARS);
//        System.out.println("Hard clears (time): " + set.DEBUG_HARDCLEARS_TIME / 1e9);
//        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);
    }

    @Test
    public void testPeeler() {
        ObjectPeeler peeler = new ObjectPeeler(new ReflectionCache());

        Map obj = new LinkedHashMap();
        obj.put(111, Arrays.asList("aaa", 222));
        obj.put(333.0, Collections.singletonMap("bbb", 444));
        Class<?> clazz = obj.getClass();

        for (int i = 0; i < 10000; i++) {
            peeler.resetTo(clazz, obj);
            while (peeler.moveNext()) ;
        }

        long start = System.nanoTime();
        long total = 0;
        for (int i = 0; i < 100000000; i++) {
            peeler.resetTo(clazz, obj);
            while (peeler.moveNext()) total++;
        }
        System.out.println(total);
        System.out.println((System.nanoTime() - start) / 1e9);

    }
}
