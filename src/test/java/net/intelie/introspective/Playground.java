package net.intelie.introspective;

import net.intelie.introspective.reflect.ObjectPeeler;
import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.util.ExpiringVisitedSet;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.IntStream;

@Ignore


public class Playground {
    @Test
    public void testSmallObject() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 16);
        ObjectSizer sizer = new ObjectSizer(set);
        Map test = new LinkedHashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        testSizer(sizer, test, 10000000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
        System.out.println("Exit misses: " + set.DEBUG_EXIT_MISS);
    }


    @Test
    public void testLargeObject() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 8);
        ObjectSizer sizer = new ObjectSizer(set);

        Object[] objs = IntStream.range(0, 1000).mapToObj(x -> {
            Map test = new HashMap();
            test.put(111 + x * 1000, Arrays.asList("aaa" + x, 222 + x * 1000));
            test.put(333.0 + x * 1000, Collections.singletonMap("bbb" + x, 444 + x * 1000));
            return test;
        }).toArray(Object[]::new);

        testSizer(sizer, objs, 10000);
        System.out.println("Collisions: " + set.DEBUG_COLLISIONS);
        System.out.println("Rehashes: " + set.DEBUG_REHASHES);
        System.out.println("Rehashes (time): " + set.DEBUG_REHASHES_TIME / 1e9);
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
        System.out.println("Objects: " + total);
        System.out.println("Time: " + (System.nanoTime() - start) / 1e9);
    }

    @Test
    public void testPerformance() {
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
