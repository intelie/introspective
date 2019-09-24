package net.intelie.introspective.reflect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringFastPathTest {
    @Test
    public void testGrowingStrings() {
        StringFastPath fast = new StringFastPath();
        String s = "";
        while (s.length() < 1024) {
            assertThat(fast.size(s)).isEqualTo(TestSizeUtils.size(s));
            s = s + s + 'x';
        }
    }

    @Test
    public void testGrowingUnicodeStrings() {
        StringFastPath fast = new StringFastPath();
        String s = "";
        while (s.length() < 1024) {
            assertThat(fast.size(s)).isEqualTo(TestSizeUtils.size(s));
            s = s + s + '€';
        }
    }
}