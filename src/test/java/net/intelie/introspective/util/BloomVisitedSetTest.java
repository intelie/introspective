package net.intelie.introspective.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class BloomVisitedSetTest {

    @Test
    public void testMany() {
        BloomVisitedSet set = new BloomVisitedSet(1 << 20, 3);
        int failures = 0;
        int total = 1<<16;
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