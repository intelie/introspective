package net.intelie.introspective.reflect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JVMPrimitivesTest {
    @Test
    public void testCompactStrings() {
        assertThat(JVMPrimitives.isCompactStringsEnabled()).isEqualTo(
                !System.getProperty("java.version").contains("1.8"));
    }

    @Test
    public void testPrimitives() {
        assertThat(JVMPrimitives.getPrimitive(byte.class)).isEqualTo(1);
        assertThat(JVMPrimitives.getPrimitive(short.class)).isEqualTo(2);
        assertThat(JVMPrimitives.getPrimitive(int.class)).isEqualTo(4);
        assertThat(JVMPrimitives.getPrimitive(long.class)).isEqualTo(8);
        assertThat(JVMPrimitives.getPrimitive(float.class)).isEqualTo(4);
        assertThat(JVMPrimitives.getPrimitive(double.class)).isEqualTo(8);
        assertThat(JVMPrimitives.getPrimitive(boolean.class)).isEqualTo(1);
        assertThat(JVMPrimitives.getPrimitive(char.class)).isEqualTo(2);

        assertThat(JVMPrimitives.getPrimitive(Long.class)).isEqualTo(4); //oopsize
    }

    @Test
    public void testExtended() {
        assertThat(JVMPrimitives.getFastPath(String.class, "ccc")).isEqualTo(TestSizeUtils.size("ccc"));
        assertThat(JVMPrimitives.getFastPath(Byte.class, null)).isEqualTo(12 + 1);
        assertThat(JVMPrimitives.getFastPath(Short.class, null)).isEqualTo(12 + 2);
        assertThat(JVMPrimitives.getFastPath(Integer.class, null)).isEqualTo(12 + 4);
        assertThat(JVMPrimitives.getFastPath(Long.class, null)).isEqualTo(12 + 8);
        assertThat(JVMPrimitives.getFastPath(Float.class, null)).isEqualTo(12 + 4);
        assertThat(JVMPrimitives.getFastPath(Double.class, null)).isEqualTo(12 + 8);
        assertThat(JVMPrimitives.getFastPath(Boolean.class, null)).isEqualTo(12 + 1);
        assertThat(JVMPrimitives.getFastPath(Character.class, null)).isEqualTo(12 + 2);
        assertThat(JVMPrimitives.getFastPath(Object.class, null)).isEqualTo(-1);
    }
}