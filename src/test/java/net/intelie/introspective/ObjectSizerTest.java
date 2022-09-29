package net.intelie.introspective;

import net.intelie.introspective.reflect.ReflectionCache;
import net.intelie.introspective.reflect.StringFastPath;
import net.intelie.introspective.reflect.TestSizeUtils;
import net.intelie.introspective.util.BloomVisitedSet;
import net.intelie.introspective.util.ExpiringVisitedSet;
import net.intelie.introspective.util.IdentityVisitedSet;
import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.LightVM;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectSizerTest {
    @Test
    public void estimateSingleton() {
        Map<Object, Object> test = Collections.singletonMap("abc", 123);

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void estimateMap() {
        Map<Object, Object> test = new HashMap<>();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void wontBreakOnDeepLinkedList() {
        Map<Object, Object> test = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            test.put(i, i);
        }

        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), new IdentityVisitedSet(), 10);
        sizer.resetTo(test);
        while (sizer.moveNext()) {
        }
        assertThat(sizer.skipped()).isGreaterThan(0);
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
        List<Object> test = new LinkedList<>();
        for (int i = 0; i < 1000; i++)
            test.add(i);

        long size = 0, count = 0;
        ObjectSizer sizer = new ObjectSizer(new ReflectionCache(), new IdentityVisitedSet(), 200);
        sizer.resetTo(test);
        while (sizer.moveNext()) {
            size += sizer.bytes();
            count++;
        }

        assertThat(size).isLessThan(TestSizeUtils.size(test));
        assertThat(count).isEqualTo(791);
    }

    @Test
    public void estimateVisitedSet() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 15);
        int expected = (1 << 20) + (1 << 18);
        assertThat(estimate(set)).isBetween((long) (expected * 0.9), (long) (expected * 1.1));
    }

    @Test
    public void estimateLinkedHashMap() {
        Map<Object, Object> test = new LinkedHashMap<>();
        test.put(111, Arrays.asList("aaa", 222));
        test.put(333.0, Collections.singletonMap("bbb", 444));

        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test));
    }

    @Test
    public void estimateLambda() {
        Integer obj = 1;
        Function<?, ?> test = (Function<Integer, Integer>) x -> x + obj;

        assertThat(estimate(obj)).isEqualTo(TestSizeUtils.size(obj));
        assertThat(estimate(test)).isEqualTo(TestSizeUtils.size(test)).isGreaterThan(TestSizeUtils.size(obj));
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

    @Test
    public void fullTest() throws ClassNotFoundException {
        fullTestWith(new ObjectSizer());
        fullTestWith(new ObjectSizer(new ReflectionCache(), new ExpiringVisitedSet(1 << 15), 1 << 15));
        fullTestWith(new ObjectSizer(new ReflectionCache(), new BloomVisitedSet(1 << 20, 2), 1 << 15));
        fullTestWith(new ObjectSizer(new ReflectionCache(), new IdentityVisitedSet(), 1 << 15));
    }

    private void fullTestWith(ObjectSizer sizer) throws ClassNotFoundException {
        Map<Object, Object> test = new LinkedHashMap<>();
        Object value1 = Arrays.asList("aaa", 222);
        Object value2 = Collections.singletonMap("bbb", 444);
        test.put(111, value1);
        test.put(333.0, value2);

        TestClass obj = new TestClass(test);

        sizer.resetTo(obj);

        assertIteratorSame(sizer, obj, 0, "");
        assertIteratorEq(sizer, (byte) 1, 1, ".boxedByte");
        assertIteratorEq(sizer, (short) 2, 1, ".boxedShort");
        assertIteratorEq(sizer, 3, 1, ".boxedInt");
        assertIteratorEq(sizer, (long) 4, 1, ".boxedLong");
        assertIteratorEq(sizer, (float) 5, 1, ".boxedFloat");
        assertIteratorEq(sizer, (double) 6, 1, ".boxedDouble");
        assertIteratorEq(sizer, true, 1, ".boxedBool");
        assertIteratorEq(sizer, 'x', 1, ".boxedChar");
        assertIteratorSame(sizer, test, 1, ".this$0");
        assertIteratorUnknown(sizer, Class.forName("java.util.LinkedHashMap$Entry"), 2, ".this$0.head");
        assertIteratorUnknown(sizer, Class.forName("java.util.LinkedHashMap$Entry"), 3, ".this$0.head.after");
        assertIteratorEq(sizer, 333.0, 4, ".this$0.head.after.key");
        assertIteratorSame(sizer, value2, 4, ".this$0.head.after.value");
        assertIteratorEqFull(sizer, "bbb", 5, ".this$0.head.after.value.k");
        assertIteratorEq(sizer, 444, 5, ".this$0.head.after.value.v");
        assertIteratorEq(sizer, 111, 3, ".this$0.head.key");
        assertIteratorSame(sizer, value1, 3, ".this$0.head.value");
        assertOnlyPath(sizer, Serializable[].class, 4, ".this$0.head.value.a");
        assertIteratorEqFull(sizer, "aaa", 5, ".this$0.head.value.a[0]");
        assertIteratorEq(sizer, 222, 5, ".this$0.head.value.a[1]");
        assertOnlyPath(sizer, Class.forName("[Ljava.util.HashMap$Node;"), 2, ".this$0.table");
        assertIteratorSame(sizer, this, 1, ".this$0$");
        assertThat(sizer.skipChildren()).isTrue();
        assertThat(sizer.skipped()).isGreaterThan(0);

        assertThat(sizer.moveNext()).isFalse();
    }

    @Test
    public void fullTestWithSkip() {
        Map<Object, Object> test = new LinkedHashMap<>();
        Object value1 = Arrays.asList("aaa", 222);
        Object value2 = Collections.singletonMap("bbb", 444);
        test.put(111, value1);
        test.put(333.0, value2);

        TestClass obj = new TestClass(test);

        ObjectSizer sizer = new ObjectSizer();
        sizer.resetTo(obj);

        assertIteratorSame(sizer, obj, 0, "");
        assertIteratorEq(sizer, (byte) 1, 1, ".boxedByte");
        assertIteratorEq(sizer, (short) 2, 1, ".boxedShort");
        assertIteratorEq(sizer, 3, 1, ".boxedInt");
        assertIteratorEq(sizer, (long) 4, 1, ".boxedLong");
        assertIteratorEq(sizer, (float) 5, 1, ".boxedFloat");
        assertIteratorEq(sizer, (double) 6, 1, ".boxedDouble");
        assertIteratorEq(sizer, true, 1, ".boxedBool");
        assertIteratorEq(sizer, 'x', 1, ".boxedChar");
        assertThat(sizer.skipChildren()).isFalse();

        assertIteratorSame(sizer, test, 1, ".this$0");
        assertThat(sizer.skipChildren()).isTrue();

        assertIteratorSame(sizer, this, 1, ".this$0$");
        assertThat(sizer.skipChildren()).isTrue();

        assertThat(sizer.moveNext()).isFalse();
    }

    private void assertIteratorEq(ObjectSizer sizer, Object obj, int depth, String path) {
        assertOnlyPath(sizer, obj.getClass(), depth, path);
        assertThat(sizer.current()).isEqualTo(obj);
        assertBytes(sizer, LightVM.current().sizeOf(obj));
    }

    private void assertIteratorEqFull(ObjectSizer sizer, Object obj, int depth, String path) {
        assertOnlyPath(sizer, obj.getClass(), depth, path);
        assertThat(sizer.current()).isEqualTo(obj);
        assertBytes(sizer, TestSizeUtils.size(obj));
    }

    private void assertBytes(ObjectSizer sizer, long expectedSize) {
        assertThat(sizer.bytes()).isEqualTo(expectedSize);
        assertThat(sizer.unalignedBytes()).isBetween(expectedSize - 7, expectedSize);
    }

    private void assertIteratorSame(ObjectSizer sizer, Object obj, int depth, String path) {
        assertOnlyPath(sizer, obj.getClass(), depth, path);
        assertThat(sizer.current()).isSameAs(obj);
        assertBytes(sizer, LightVM.current().sizeOf(obj));
    }

    private void assertIteratorUnknown(ObjectSizer sizer, Class<?> clazz, int depth, String path) {
        assertOnlyPath(sizer, clazz, depth, path);
        LightVM.current();
        assertThat(sizer.current()).isNotNull();
        assertBytes(sizer, ClassLayout.parseClass(clazz).instanceSize());
    }

    private void assertOnlyPath(ObjectSizer sizer, Class<?> clazz, int depth, String path) {
        assertThat(sizer.moveNext()).isTrue();
        assertThat(sizer.path()).isEqualTo(path);
        assertThat(sizer.type()).isEqualTo(clazz);
        assertThat(sizer.visitDepth()).isEqualTo(depth);
    }

    private long estimate(Object obj) {
        long size = 0, size2 = 0;
        ObjectSizer sizer = new ObjectSizer();
        sizer.resetTo(obj);
        while (sizer.moveNext())
            size += sizer.bytes();
        sizer.clear();
        sizer.resetTo(obj);
        while (sizer.moveNext())
            size2 += sizer.bytes();
        assertThat(size).isEqualTo(size2);
        return size;
    }

    private class TestClass {
        private byte primByte;
        private short primShort;
        private int primInt;
        private long primLong;
        private float primFloat;
        private double primDouble;
        private boolean primBool;
        private char primChar;

        private Byte boxedByte = 1;
        private Short boxedShort = 2;
        private Integer boxedInt = 3;
        private Long boxedLong = 4L;
        private Float boxedFloat = 5f;
        private Double boxedDouble = 6d;
        private Boolean boxedBool = true;
        private Character boxedChar = 'x';

        private Map<?, ?> this$0;

        public TestClass(Map<?, ?> string) {
            this.this$0 = string;
        }
    }
}
