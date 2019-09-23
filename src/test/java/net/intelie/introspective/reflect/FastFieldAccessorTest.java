package net.intelie.introspective.reflect;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class FastFieldAccessorTest {
    @Test
    public void testNormalCase() throws NoSuchFieldException {
        assertAccessor("privateValue", "42");
    }

    @Test
    public void testPublicCase() throws NoSuchFieldException {
        assertAccessor(TestClass.class.getField("publicValue"), "43");
        assertAccessor("publicValue", "43");
    }

    @Test
    public void testPrimitives() throws NoSuchFieldException {
        assertAccessor("primByte", (byte) 42);
        assertAccessor("primShort", (short) 42);
        assertAccessor("primInt", (int) 42);
        assertAccessor("primLong", (long) 42);
        assertAccessor("primFloat", 42f);
        assertAccessor("primDouble", 42d);
        assertAccessor("primBool", true);
        assertAccessor("primChar", 'x');
    }


    private void assertAccessor(String fieldName, Object expectedValue) throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField(fieldName);
        assertAccessor(field, expectedValue);
    }

    private void assertAccessor(Field field, Object expectedValue) {
        TestClass test = new TestClass();
        FastFieldAccessor accessor = new FastFieldAccessor(field);
        assertThat(accessor.get(test)).isEqualTo(expectedValue);
        assertThat(accessor.name()).isEqualTo(field.getName());

        FastFieldAccessor accessor2 = new FastFieldAccessor(field, false);
        assertThat(accessor2.get(test)).isEqualTo(expectedValue);
        assertThat(accessor2.name()).isEqualTo(field.getName());
    }

    private static class TestClass {
        public String publicValue = "43";
        private String privateValue = "42";
        private byte primByte = 42;
        private short primShort = 42;
        private int primInt = 42;
        private long primLong = 42;
        private float primFloat = 42;
        private double primDouble = 42;
        private boolean primBool = true;
        private char primChar = 'x';
    }
}