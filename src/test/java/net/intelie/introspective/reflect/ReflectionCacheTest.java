package net.intelie.introspective.reflect;

import net.intelie.introspective.util.UnsafeGetter;
import org.junit.Test;
import org.openjdk.jol.info.ClassData;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.FieldLayout;
import org.openjdk.jol.layouters.CurrentLayouter;
import org.openjdk.jol.vm.LightVM;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionCacheTest {
    @Test
    public void testCaching() {
        ReflectionCache cache = new ReflectionCache();
        ReflectionCache.Item item = cache.get(TestClass.class);

        assertThat(JVMPrimitives.align(item.size())).isEqualTo(alternativeSizing1(TestClass.class));
        assertThat(item.size()).isEqualTo(alternativeSizing2(TestClass.class));
        assertThat(item.fieldCount()).isEqualTo(10);
        assertThat(item.fieldName(0)).isEqualTo("boxedByte");

    }

    private long alternativeSizing1(Class<?> clazz) {
        LightVM.current();
        ClassLayout layout = new CurrentLayouter().layout(ClassData.parseClass(clazz));
        return layout.instanceSize();
    }

    private long alternativeSizing2(Class<?> clazz) {
        Unsafe unsafe = UnsafeGetter.get();
        long max = 0;
        for (Field field : clazz.getDeclaredFields()) {
            max = Math.max(max, unsafe.objectFieldOffset(field) + JVMPrimitives.getPrimitive(field.getType()));
        }
        return max;
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