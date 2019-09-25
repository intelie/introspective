package net.intelie.introspective.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VisitedSetTest {
    @Test
    public void testExpireInTheMiddle() {
        VisitedSet<Object> set = new VisitedSet<Object>(2) {
            @Override
            public int hash(Object obj) {
                return obj.hashCode();
            }
        };

        Custom c1 = new Custom(1);
        Custom c1b = new Custom(1);
        Custom c2 = new Custom(2);

        set.enter(c1);
        set.enter(c2);
        set.exit(c1);
        set.exit(c2);

        set.enter(c1b);

        //at this point, if [1] = c2, [2] = c1b, things are broken
        assertThat(set.contains(c1)).isFalse();
        assertThat(set.contains(c1b)).isTrue();
        assertThat(set.contains(c2)).isTrue();
    }

    @Test
    public void testAddingAgain() {
        VisitedSet<Object> set = new VisitedSet<>(16);

        Object obj = new Object();

        assertThat(set.contains(obj)).isFalse();
        assertThat(set.enter(obj)).isTrue();

        assertThat(set.contains(obj)).isTrue();
        assertThat(set.enter(obj)).isFalse();

        assertThat(set.contains(obj)).isTrue();
        assertThat(set.enter(obj)).isFalse();
    }

    @Test
    public void testAddingPastEnd() {
        Object[] objs = new Object[16];
        VisitedSet<Object> set = new VisitedSet<>(objs.length);

        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Object();
            assertThat(set.enter(objs[i])).isTrue();
        }
        for (int i = 0; i < objs.length; i++) {
            assertThat(set.contains(objs[i])).isTrue();
        }
        assertThat(set.enter(new Object())).isFalse();
        set.softClear();

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.contains(objs[i])).isFalse();
        }

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.enter(new Object())).isTrue();
        }

        assertThat(set.enter(new Object())).isFalse();
    }

    @Test
    public void testExit() {
        Object[] objs = new Object[4];
        VisitedSet<Object> set = new VisitedSet<>(objs.length);

        for (int i = 0; i < objs.length; i++) {
            objs[i] = new Object();
            assertThat(set.enter(objs[i])).isTrue();
            assertThat(set.contains(objs[i])).isTrue();
        }

        assertThat(set.enter(new Object())).isFalse();
        assertThat(set.exit(new Object())).isFalse();

        for (int i = 0; i < objs.length; i++) {
            assertThat(set.exit(objs[i])).isTrue();
        }

        assertThat(set.enter(objs[0])).isFalse(); //object is still there, even with exit
        assertThat(set.enter(new Object())).isTrue(); //forcing old object to be removed
        assertThat(set.enter(objs[0])).isTrue(); //now object can be added again, and will remove #1
        assertThat(set.enter(objs[1])).isTrue(); //#1 is removed and readded, now #2 is removed
        assertThat(set.enter(objs[3])).isFalse(); //but not #3 (yet)
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