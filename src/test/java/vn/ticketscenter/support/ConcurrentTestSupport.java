package vn.ticketscenter.support;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public final class ConcurrentTestSupport {
    private ConcurrentTestSupport() {}

    public static void meet(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new AssertionError("Concurrent test barrier failed", ex);
        }
    }
}
