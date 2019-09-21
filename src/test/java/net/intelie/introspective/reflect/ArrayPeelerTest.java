package net.intelie.introspective.reflect;

import net.intelie.introspective.ThreadResources;
import org.junit.Test;
import org.openjdk.jol.vm.LightVM;

import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayPeelerTest {
    @Test
    public void testPrimitive() {
        long[] obj = {1, 2, 3};

        ArrayPeeler peeler = new ArrayPeeler();
        long bytes = peeler.resetTo(long[].class, obj);

        assertThat(bytes).isEqualTo(LightVM.current().sizeOf(obj));
        assertThat(peeler.moveNext()).isFalse();
    }

    @Test
    public void testObject() {
        Integer[] obj = {1, 2, 3, null, 4};

        ArrayPeeler peeler = new ArrayPeeler();
        long bytes = peeler.resetTo(Integer[].class, obj);

        assertThat(JVMPrimitives.align(bytes)).isEqualTo(LightVM.current().sizeOf(obj));
        assertNext(peeler, 0, 1);
        assertNext(peeler, 1, 2);
        assertNext(peeler, 2, 3);
        assertNext(peeler, 4, 4);
        assertThat(peeler.moveNext()).isFalse();
    }

    @Test
    public void testString() {
        char[] obj = "abcdefgh".toCharArray();

        ArrayPeeler peeler = new ArrayPeeler();
        long bytes = peeler.resetTo(char[].class, obj);

        assertThat(bytes).isEqualTo(LightVM.current().sizeOf(obj));
        assertThat(peeler.moveNext()).isFalse();
    }

    @Test
    public void testStringGrowing() {
        StringBuilder s = new StringBuilder();
        while (s.length() < 1024) {
            char[] obj = s.toString().toCharArray();

            ArrayPeeler peeler = new ArrayPeeler();
            long bytes = peeler.resetTo(char[].class, obj);

            assertThat(JVMPrimitives.align(bytes))
                    .describedAs("%d", s.length())
                    .isEqualTo(LightVM.current().sizeOf(obj));
            assertThat(peeler.moveNext()).isFalse();

            s.append(s).append("x");
        }
    }

    @Test
    public void testGrowingSizes() {
        assertSizing(byte[]::new);
        assertSizing(short[]::new);
        assertSizing(int[]::new);
        assertSizing(long[]::new);
        assertSizing(float[]::new);
        assertSizing(double[]::new);
        assertSizing(boolean[]::new);
        assertSizing(char[]::new);
    }

    private void assertSizing(IntFunction<Object> fn) {
        for (int i = 0; i < 1024; i = i * 2 + 1) {
            Object obj = fn.apply(i);
            ArrayPeeler peeler = new ArrayPeeler();

            long bytes = peeler.resetTo(obj.getClass(), obj);

            assertThat(JVMPrimitives.align(bytes))
                    .describedAs("%d", i)
                    .isEqualTo(LightVM.current().sizeOf(obj));

        }
    }

    @Test
    public void testAllocations() {
        ArrayPeeler peeler = new ArrayPeeler();
        Integer[] obj = {1, 2, 3};
        peeler.resetTo(Integer[].class, obj);
        while (peeler.moveNext()) ;

        long start = ThreadResources.allocatedBytes(Thread.currentThread());
        for (int i = 0; i < 1000; i++) {
            peeler.resetTo(Integer[].class, obj);
            while (peeler.moveNext()) ;
        }
        assertThat((ThreadResources.allocatedBytes(Thread.currentThread()) - start) / 1000).isEqualTo(0);
    }

    private void assertNext(ReferencePeeler peeler, Object index, Object value) {
        assertThat(peeler.moveNext()).isTrue();
        assertThat(peeler.currentIndex()).isEqualTo(index);
        assertThat(peeler.current()).isEqualTo(value);
    }

}