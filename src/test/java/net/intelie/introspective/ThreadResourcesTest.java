package net.intelie.introspective;

import com.sun.management.ThreadMXBean;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadResourcesTest {
    private ThreadMXBean bean;

    @Before
    public void setUp() throws Exception {
        bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        //warmup
        diff();
    }

    @Test
    public void testEverythingValid() {
        System.out.println("RUNNING FROM: " + System.getProperty("java.version"));
        if (!System.getProperty("os.name").toLowerCase().contains("mac os")) {
            assertThat(ThreadResources.isValidTlab()).isTrue();
            assertThat(ThreadResources.isValidAllocated()).isTrue();
        }
    }

    @Test
    public void testEnabled() {
        for (int i = 0; i < 1000; i++)
            diff();

        long total = 0;
        for (int i = 0; i < 1000; i++) {
            total += diff();
        }
        assertThat(total / 1000).isZero();
    }

    @Test
    public void testWillGetByteArrayAllocation() {
        long start = ThreadResources.allocatedBytes();

        byte[] bytes = new byte[100000];

        long total = ThreadResources.allocatedBytes() - start;
        if (!System.getProperty("os.name").toLowerCase().contains("mac os"))
            assertThat(total).isBetween(100000L, 100000L + 100);
        else
            assertThat(total).isZero();
    }

    private long diff() {
        long check = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        long mine = ThreadResources.allocatedBytes(Thread.currentThread());
        if (System.getProperty("os.name").toLowerCase().contains("mac os")) {
            assertThat(mine).isZero();
            return 0;
        }
        assertThat(mine).isGreaterThanOrEqualTo(check);
        return mine - check;
    }

}
