package net.intelie.introspective.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExpiringVisitedSetTest {
    @Test
    public void testMany() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 8);
        for (int j = 0; j < 1000; j++) {
            set.softClear();
            for (int i = 0; i < 16; i++) {
                Object obj = new Object();
                assertThat(set.enter(obj)).isGreaterThanOrEqualTo(0);
                assertThat(set.exit(obj, 0)).isTrue();
            }
        }
    }

    @Test
    public void testClear() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 8);
        Object obj = new Object();
        set.enter(obj);
        assertThat(set.contains(obj)).isNotNegative();
        set.clear();
        assertThat(set.contains(obj)).isNegative();
    }

    @Test
    public void testForceClear() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(1 << 8, 1 << 20, 1 << 21);

        for (int i = 0; i < (1 << 12) - 2; i++) {
            assertThat(set.softClear()).isTrue();
        }
        assertThat(set.softClear()).isFalse();
    }

    @Test
    public void testExpireInTheMiddle() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(2, 2, 8) {
            public int hash(Object obj) {
                return obj.hashCode();
            }
        };

        Custom c1 = new Custom(1);
        Custom c1b = new Custom(1);
        Custom c2 = new Custom(2);

        set.enter(c1);
        set.enter(c2);
        set.exit(c1, 0);
        set.exit(c2, 0);

        set.enter(c1b);

        //at this point, if [1] = c2, [2] = c1b, things are broken
        assertThat(set.contains(c1)).isLessThan(0);
        assertThat(set.contains(c1b)).isGreaterThanOrEqualTo(0);
        assertThat(set.contains(c2)).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testAvoidOverwrite() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(4);

        Custom c1 = new Custom(1);
        Custom c2 = new Custom(2);
        Custom c1b = new Custom(1);

        set.enter(c1);
        set.enter(c2);
        set.enter(c1b);

        assertThat(set.contains(c1)).isGreaterThanOrEqualTo(0);
        assertThat(set.contains(c1b)).isGreaterThanOrEqualTo(0);
        assertThat(set.contains(c2)).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testAddingAgain() {
        ExpiringVisitedSet set = new ExpiringVisitedSet(16);

        Object obj = new Object();

        assertThat(set.contains(obj)).isLessThan(0);
        int index = set.enter(obj);
        assertThat(index).isGreaterThan(0);

        assertThat(set.contains(obj)).isGreaterThanOrEqualTo(0);
        assertThat(set.enter(obj)).isEqualTo(~index);

        assertThat(set.contains(obj)).isGreaterThanOrEqualTo(0);
        assertThat(set.enter(obj)).isEqualTo(~index);
    }

    @Test
    public void testAddingPastEnd() {
        Object[] objs = new Object[16];
        ExpiringVisitedSet set = new ExpiringVisitedSet(objs.length);

        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Object();
            assertThat(set.enter(objs[i])).isGreaterThanOrEqualTo(0);
        }
        for (int i = 0; i < objs.length; i++) {
            assertThat(set.contains(objs[i])).isGreaterThanOrEqualTo(0);
        }
        assertThat(set.enter(new Object())).isLessThan(0);
        set.softClear();

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.contains(objs[i])).isLessThan(0);
        }

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.enter(new Object())).isGreaterThanOrEqualTo(0);
        }

        assertThat(set.enter(new Object())).isLessThan(0);
    }

    @Test
    public void testExit() {
        Object[] objs = new Object[4];
        ExpiringVisitedSet set = new ExpiringVisitedSet(objs.length, objs.length, 4 * objs.length);
        set.softClear();

        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Object();
            assertThat(set.enter(objs[i])).isGreaterThanOrEqualTo(0);
            assertThat(set.contains(objs[i])).isGreaterThanOrEqualTo(0);
        }

        assertThat(set.enter(new Object())).isLessThan(0);
        assertThat(set.exit(new Object(), 0)).isFalse();

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.exit(objs[i], 0)).isTrue();
        }

        assertThat(set.enter(objs[0])).isLessThan(0); //object is still there, even with exit
        assertThat(set.enter(new Object())).isGreaterThanOrEqualTo(0); //forcing old object to be removed
        assertThat(set.enter(objs[0])).isGreaterThanOrEqualTo(0); //now object can be added again, and will remove #1
        assertThat(set.enter(objs[1])).isGreaterThanOrEqualTo(0); //#1 is removed and readded, now #2 is removed
        assertThat(set.enter(objs[3])).isLessThan(0); //but not #3 (yet)
    }

    private static class Custom {
        private final int hash;

        public Custom(int hash) {
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

}