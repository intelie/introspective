package net.intelie.introspective;

import net.intelie.introspective.reflect.GenericPeeler;
import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.reflect.StringFastPath;
import net.intelie.introspective.reflect.TestSizeUtils;
import net.intelie.introspective.util.BloomVisitedSet;
import net.intelie.introspective.util.ExpiringVisitedSet;
import net.intelie.introspective.util.IdentityVisitedSet;
import org.junit.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class BloomObjectSizerTest {
    @Test
    public void estimateSingleton() {
        Map test = Collections.singletonMap("abc", 123);

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void wontBreakOnDeepLinkedList() {
        Map test = new LinkedHashMap();
        for (int i = 0; i < 100; i++) {
            test.put(i, i);
        }

        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1024, 1, 10);
        sizer.visit(test);
        assertThat(sizer.skipped()).isGreaterThan(0);
    }


    @Test
    public void estimateSizerSize() {

        ReflectionCache cache = new ReflectionCache(f -> !f.getType().equals(ReflectionCache.class));

        BloomObjectSizer sizer = new BloomObjectSizer(
                cache, //ignore cache itself
                1 << 20,
                (1 << 16) - 1, //adding -1 because of https://bugs.java.com/bugdatabase/view_bug.do?bug_id=8164793
                1 << 10
        );
        int expected = (1 << 20) / 8 + (1 << 16) * 4 + (1 << 10) * 96;
        assertThat(realEstimate(cache, sizer)).isBetween((long) (expected * 0.9), (long) (expected * 1.1));
    }

    @Test
    public void estimateMap() {
        Map test = new HashMap<>();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void estimateNull() {
        assertThat(estimate(null)).isEqualTo(0);
    }

    @Test
    public void estimateLinkedList() {
        List<Object> test = new LinkedList<>();
        for (int i = 0; i < 1000; i++)
            test.add(i);
        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void estimateLinkedListOverMaxDepth() {
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 20, 1, 200);
        for (int k = 0; k < 10; k++) {
            List<Object> test = new LinkedList<>();
            for (int i = 0; i < 1000; i++)
                test.add(i);

            sizer.softClear();
            sizer.visit(test);

            assertThat(sizer.bytes()).isLessThan(TestSizeUtils.size(test));
            assertThat(sizer.count()).isEqualTo(797);

        }
    }

    @Test
    public void estimateVisitedSet() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        int expected = (1 << 20) + (1 << 18);
        assertThat(estimate(set)).isBetween((long) (expected * 0.9), (long) (expected * 1.1));
    }

    @Test
    public void estimateLinkedHashMap() {
        Map test = new LinkedHashMap();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void testGrowingString() {
        StringFastPath fast = new StringFastPath();
        String s = "";
        while (s.length() < 1024) {
            assertThat(estimate(s)).isEqualTo(TestSizeUtils.size(s));
            s = s + s + 'x';
        }
    }

    private long estimate(Object obj) {
        BloomObjectSizer sizer = new BloomObjectSizer(new ReflectionCache(), 1 << 20, 1000, 1000);
        sizer.clear();
        sizer.visit(obj);
        return sizer.bytes();
    }

    private long realEstimate(ReflectionCache cache, Object obj) {
        ObjectSizer real = new ObjectSizer(cache, new IdentityVisitedSet(), 1 << 15);
        real.resetTo(obj);
        long total = 0;
        while (real.moveNext()) {
            total += real.bytes();
        }
        return total;
    }
}