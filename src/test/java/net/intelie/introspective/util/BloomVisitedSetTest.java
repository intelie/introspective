package net.intelie.introspective.util;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BloomVisitedSetTest {
    @Test
    public void testMix() {
        int[] count = new int[32];
        int[] count2 = new int[32];

        int tests = 100000;
        for (int i = 0; i < tests; i++) {
            int hashed = BloomVisitedSet.mix(i);
            int hashed2 = BloomVisitedSet.mix(i + 1);

            for (int j = 0; j < 32; j++) {
                if ((hashed & 1 << j) != 0) {
                    count[j]++;
                    if ((hashed2 & 1 << j) != 0)
                        count2[j]++;
                }
            }
        }

        for (int i = 0; i < 32; i++) {
            assertThat(count[i]).isBetween(tests * 9 / 20, tests * 11 / 20);
            assertThat(count2[i]).isBetween(tests * 9 / 40, tests * 11 / 40);
        }
    }

    @Test
    public void testMany() {
        BloomVisitedSet set = new BloomVisitedSet(1 << 20, 3);
        int failures = 0;
        int total = 1 << 16;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < total; j++) {
                Object obj = new Object();
                if (set.enter(obj) < 0)
                    failures++;
                assertThat(set.enter(obj)).isLessThan(0);
                set.exit(obj, 0);
            }
            assertThat(failures / (double) total).isLessThan(1);
            assertThat(set.softClear()).isFalse();
        }
    }

    @Test
    public void testAddingAgain() {
        BloomVisitedSet set = new BloomVisitedSet(1 << 20, 4);

        Object obj = new Object();

        assertThat(set.enter(obj)).isGreaterThanOrEqualTo(0);
        assertThat(set.enter(obj)).isLessThan(0);
        assertThat(set.enter(obj)).isLessThan(0);
    }

}