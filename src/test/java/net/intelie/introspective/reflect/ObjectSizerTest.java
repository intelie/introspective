package net.intelie.introspective.reflect;

import net.intelie.introspective.ThreadResources;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectSizerTest {
    @Test
    public void estimateSingleton() {
        Map test = Collections.singletonMap("abc", 123);

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void estimateMap() {
        Map test = new HashMap<>();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    private long estimate(Object obj) {
        long size = 0;
        ObjectSizer sizer = new ObjectSizer();
        sizer.resetTo(obj);
        while (sizer.moveNext()) {
            size += sizer.bytes();
        }
        return size;
    }

    @Test
    @Ignore
    public void testPerformance() {
        ObjectSizer sizer = new ObjectSizer();
        Map test = new HashMap<>();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        for (int i = 0; i < 10000; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) ;
        }

        long start = System.nanoTime();
        long memStart = ThreadResources.allocatedBytes(Thread.currentThread());
        long total = 0;
        for (int i = 0; i < 100000000; i++) {
            sizer.resetTo(test);
            while (sizer.moveNext()) total++;
        }
        System.out.println(total);
        System.out.println((ThreadResources.allocatedBytes(Thread.currentThread()) - memStart));
        System.out.println((System.nanoTime() - start) / 1e9);

    }
}