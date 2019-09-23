package net.intelie.introspective.reflect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JVMPrimitivesTest {
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
        assertThat(JVMPrimitives.getFastPath(Byte.class)).isEqualTo(12 + 1);
        assertThat(JVMPrimitives.getFastPath(Short.class)).isEqualTo(12 + 2);
        assertThat(JVMPrimitives.getFastPath(Integer.class)).isEqualTo(12 + 4);
        assertThat(JVMPrimitives.getFastPath(Long.class)).isEqualTo(12 + 8);
        assertThat(JVMPrimitives.getFastPath(Float.class)).isEqualTo(12 + 4);
        assertThat(JVMPrimitives.getFastPath(Double.class)).isEqualTo(12 + 8);
        assertThat(JVMPrimitives.getFastPath(Boolean.class)).isEqualTo(12 + 1);
        assertThat(JVMPrimitives.getFastPath(Character.class)).isEqualTo(12 + 2);
    }
}