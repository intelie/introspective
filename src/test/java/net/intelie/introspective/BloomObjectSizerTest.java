package net.intelie.introspective;

import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.reflect.StringFastPath;
import net.intelie.introspective.reflect.TestSizeUtils;
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

            sizer.clear();
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
        sizer.visit(obj);
        return sizer.bytes();
    }
}