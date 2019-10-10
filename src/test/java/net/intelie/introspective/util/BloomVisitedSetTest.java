package net.intelie.introspective.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;

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
            assertThat(count[i]).isBetween(tests * 49 / 100, tests * 51 / 100);
            assertThat(count2[i]).isBetween(tests * 24 / 100, tests * 26 / 100);
        }
    }

    @Test
    @Ignore
    public void testError() {
        int total = 100000;
        double[][] error = new double[100][10];
        for (int i = 1; i <= 10; i++) {
            BloomVisitedSet set = new BloomVisitedSet(1 << 20, i);
            int failures = 0;
            for (int j = 1; j <= total; j++) {
                Object obj = new Object();
                if (set.enter(obj) < 0)
                    failures++;
                assertThat(set.enter(obj)).isLessThan(0);
                set.exit(obj, 0);

                if (j % 1000 == 0)
                    error[j / 1000 - 1][i - 1] = failures / (double) j;
            }
        }
        for (int j = 0; j < 100; j++) {
            System.out.println(Arrays.stream(error[j]).mapToObj(String::valueOf).collect(Collectors.joining("\t")));
        }
    }

    @Test
    public void testMany() {
        BloomVisitedSet set = new BloomVisitedSet(1 << 20, 2);
        int total = 1 << 16;
        for (int i = 0; i < 10; i++) {
            int failures = 0;
            for (int j = 0; j < total; j++) {
                Object obj = new Object();
                if (set.enter(obj) < 0)
                    failures++;
                assertThat(set.enter(obj)).isLessThan(0);
                set.exit(obj, 0);
            }
            //System.out.println(failures / (double) total);
            assertThat(failures / (double) total).isLessThan(0.02);
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