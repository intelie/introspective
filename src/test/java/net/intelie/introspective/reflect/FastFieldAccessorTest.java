package net.intelie.introspective.reflect;

import net.intelie.introspective.ThreadResources;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.Assert.*;

public class FastFieldAccessorTest {
    @Test
    public void simpleTest() throws NoSuchFieldException {

        Field field = HashMap.class.getDeclaredField("table");
        FastFieldAccessor accessor = new FastFieldAccessor(field);

        for (int i = 0; i < 1000; i++) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put(42, 42);
            accessor.get(map);
        }

        HashMap<Object, Object> map = new HashMap<>();
        map.put(42, 42);

        long start = System.nanoTime();
        long memStart = ThreadResources.allocatedBytes();
        long total = 0;
        for (int i = 0; i < 100000000; i++) {
            total += Array.getLength(accessor.get(map));
        }
        System.out.println((ThreadResources.allocatedBytes() - memStart));
        System.out.println(total);
        System.out.println((System.nanoTime() - start) / 1e9);
    }
}