package net.intelie.introspective;

import com.sun.management.ThreadMXBean;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
        assertThat(ThreadResources.isValidTlab()).isTrue();
        assertThat(ThreadResources.isValidAllocated()).isTrue();
    }

    @Test
    public void testEnabled() {
        for (int i = 0; i < 1000; i++) {
            assertThat(diff()).isZero();
        }
    }

    @Test
    public void testWillGetByteArrayAllocation() {
        long start = ThreadResources.allocatedBytes(Thread.currentThread());

        byte[] bytes = new byte[100000];

        long total = ThreadResources.allocatedBytes(Thread.currentThread()) - start;

        assertThat(total).isBetween(100000L, 100000L + 100);
    }

    private long diff() {
        long check = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        long mine = ThreadResources.allocatedBytes(Thread.currentThread());
        assertThat(mine).isGreaterThanOrEqualTo(check);
        return mine - check;
    }

    @Test
    public void testOneSmallClass() throws Exception {
        testSmallClass(1);
    }

    @Test
    public void testManySmallClass() throws Exception {
        testSmallClass(100);
    }

    private void testSmallClass(int size) throws InterruptedException {
        MemorySizedThread thread = new MemorySizedThread() {
            long start, stop;
            @Override
            public void run() {
                start = ThreadResources.allocatedBytes(Thread.currentThread());
                SmallClass[] small = new SmallClass[size];
                for (SmallClass inner : small) inner = new SmallClass('0', 1, 2, 3);
                stop = ThreadResources.allocatedBytes(Thread.currentThread());
            }

            @Override
            public long memory() {
                return stop - start;
            }
        };

        final long HEADER_SIZE = 3 * 4;
        final long CHAR_SIZE = 2;
        final long INTEGER_SIZE = 4;
        final long LONG_SIZE = 8;
        final long DOUBLE_SIZE = 8;
        long expectedLayout = HEADER_SIZE + CHAR_SIZE + INTEGER_SIZE + LONG_SIZE + DOUBLE_SIZE;
        final long ARRAY_HEADER_SIZE = 4 * 4;
        final long REFERENCE_SIZE = 4;
        long arrayCost = ARRAY_HEADER_SIZE + size * REFERENCE_SIZE;
        long expectedTotal = arrayCost + size * expectedLayout;

        thread.start();
        thread.join();
        assertThat(thread.memory()).isBetween(expectedTotal, expectedTotal + expectedTotal * (expectedTotal * 10 / 100));
    }

}

class SmallClass {
    private char c;
    private int i;
    private long l;
    private double d;

    public SmallClass(char c, int i, long l, double d) {
        this.c = c;
        this.i = i;
        this.l = l;
        this.d = d;
    }
}

abstract class MemorySizedThread extends Thread {
    public abstract long memory();
}
