package net.intelie.introspective.reflect;

import net.intelie.introspective.ThreadResources;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.vm.LightVM;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectPeelerTest {
    private final ReflectionCache cache = new ReflectionCache(x -> !x.isSynthetic());

    @Test
    public void testSimpleClass() {
        TestClass obj = new TestClass("abc");

        ObjectPeeler peeler = new ObjectPeeler(cache);
        long bytes = peeler.resetTo(TestClass.class, obj);

        LightVM.current();
        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(obj));
        //remember this is also a reference
        assertThat(bytes).isEqualTo(12 + 32 + 10 * JVMPrimitives.getOppSize());

        assertNext(peeler, "boxedByte", (byte) 1);
        assertNext(peeler, "boxedShort", (short) 2);
        assertNext(peeler, "boxedInt", 3);
        assertNext(peeler, "boxedLong", 4L);
        assertNext(peeler, "boxedFloat", 5f);
        assertNext(peeler, "boxedDouble", 6d);
        assertNext(peeler, "boxedBool", true);
        assertNext(peeler, "boxedChar", 'x');
        assertNext(peeler, "this$0", "abc");

        assertThat(peeler.moveNext()).isFalse();
    }

    @Test
    public void testNewHashMap() {
        HashMap map = new HashMap();

        ObjectPeeler peeler = new ObjectPeeler(cache);
        long bytes = peeler.resetTo(HashMap.class, map);

        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(map));
    }

    @Test
    public void testBoxedLong() {
        ObjectPeeler peeler = new ObjectPeeler(cache);
        long bytes = peeler.resetTo(Long.class, 123L);

        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(123L));
    }

    @Test
    public void testString() {
        String s = "";
        for (int i = 0; i < 100; i++) {
            s += "x";
            ObjectPeeler peeler = new ObjectPeeler(cache);
            long bytes = peeler.resetTo(String.class, s);

            assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(s));
        }
    }

    @Test
    public void testNewArrayList() {
        List list = new ArrayList<>();

        ObjectPeeler peeler = new ObjectPeeler(cache);
        long bytes = peeler.resetTo(ArrayList.class, list);

        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(list));
    }

    @Test
    public void testSimpleStaticClass() {
        TestStaticClass obj = new TestStaticClass("abc");

        ObjectPeeler peeler = new ObjectPeeler(cache);
        long bytes = peeler.resetTo(TestStaticClass.class, obj);

        LightVM.current();
        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(obj));
        assertThat(bytes).isEqualTo(12 + 32 + 9 * JVMPrimitives.getOppSize());

        assertNext(peeler, "boxedByte", (byte) 1);
        assertNext(peeler, "boxedShort", (short) 2);
        assertNext(peeler, "boxedInt", 3);
        assertNext(peeler, "boxedLong", 4L);
        assertNext(peeler, "boxedFloat", 5f);
        assertNext(peeler, "boxedDouble", 6d);
        assertNext(peeler, "boxedBool", true);
        assertNext(peeler, "boxedChar", 'x');
        assertNext(peeler, "this$0", "abc");
        assertThat(peeler.moveNext()).isFalse();
    }

    @Test
    public void testAllocations() {
        ObjectPeeler peeler = new ObjectPeeler(cache);
        TestClass obj = new TestClass("abc");
        peeler.resetTo(TestClass.class, obj);
        while (peeler.moveNext()) ;

        long start = ThreadResources.allocatedBytes(Thread.currentThread());
        for (int i = 0; i < 1000; i++) {
            peeler.resetTo(TestClass.class, obj);
            while (peeler.moveNext()) ;
        }
        assertThat((ThreadResources.allocatedBytes(Thread.currentThread()) - start) / 1000).isEqualTo(0);
    }

    @Test
    @Ignore
    public void testPerformance() {
        ObjectPeeler peeler = new ObjectPeeler(cache);

//        Map obj = new LinkedHashMap();
//        obj.put(111, Arrays.asList("aaa", 222));
//        obj.put(333.0, Collections.singletonMap("bbb", 444));
        TestClass obj = new TestClass("ccc");
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


    private void assertNext(ReferencePeeler peeler, Object index, Object value) {
        assertThat(peeler.moveNext()).isTrue();
        assertThat(peeler.currentIndex()).isEqualTo(index);
        assertThat(peeler.current()).isEqualTo(value);
    }

    private static class TestStaticClass {
        private static String MUST_IGNORE = "ABCDEFGHIJ";
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

        private String this$0;

        public TestStaticClass(String string) {
            this.this$0 = string;
        }
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

        private String this$0;

        public TestClass(String string) {
            this.this$0 = string;
        }
    }
}