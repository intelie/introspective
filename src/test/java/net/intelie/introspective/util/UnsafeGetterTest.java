package net.intelie.introspective.util;

import org.junit.Test;
import sun.misc.Unsafe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UnsafeGetterTest {
    @Test
    public void testCouldGetUnsafe() {
        assertThat(UnsafeGetter.get()).isNotNull();
    }

    @Test
    public void testCouldGetInternalObjectFieldOffsetMethodHandle() {
        assertThat(UnsafeGetter.getObjectFieldOffset()).isNotNull();
    }

    @Test
    public void testExceptionHandling() {
        assertThatThrownBy(() -> UnsafeGetter.tryGetAccessible(Unsafe.class, "unknown"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testExceptionHandlingInternalUnsafe() {
        assertThatThrownBy(() -> UnsafeGetter.tryGetRealMethodHandle(Unsafe.class, "unknown", "unknown", "objectFieldOffset"))
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(NoSuchFieldException.class);
    }

    @Test
    public void testExceptionHandlingInternalMethod() {
        assertThatThrownBy(() -> UnsafeGetter.tryGetRealMethodHandle(Unsafe.class, "theInternalUnsafe", "theUnsafe", "unknown"))
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(NoSuchMethodException.class);
    }
}
