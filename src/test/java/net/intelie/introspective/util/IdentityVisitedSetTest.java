package net.intelie.introspective.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityVisitedSetTest {
    @Test
    public void normalMethods() {
        IdentityVisitedSet set = new IdentityVisitedSet();
        Object o1 = new Object();
        Object o2 = new Object();

        assertThat(set.enter(o1)).isEqualTo(1);
        assertThat(set.enter(o1)).isEqualTo(-1);

        assertThat(set.exit(o1, -1)).isTrue();
        assertThat(set.exit(o2, -1)).isTrue();

        assertThat(set.enter(o1)).isEqualTo(-1);
        assertThat(set.enter(o2)).isEqualTo(1);

        assertThat(set.softClear()).isFalse();

        assertThat(set.enter(o1)).isEqualTo(1);
        assertThat(set.enter(o2)).isEqualTo(1);

        set.clear();

        assertThat(set.enter(o1)).isEqualTo(1);
        assertThat(set.enter(o2)).isEqualTo(1);
    }
}