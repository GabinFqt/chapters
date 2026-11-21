package com.gabinx.chapters;

import com.gabinx.chapters.logic.ClientRefreshCoalescer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientRefreshCoalescerTest {

    @Test
    void twoSchedulesFlushOnce() {
        AtomicInteger calls = new AtomicInteger();
        ClientRefreshCoalescer coalescer = new ClientRefreshCoalescer(calls::incrementAndGet);

        coalescer.schedule();
        coalescer.schedule();
        coalescer.flush();

        assertEquals(1, calls.get());
    }

    @Test
    void flushWithoutScheduleDoesNothing() {
        AtomicInteger calls = new AtomicInteger();
        ClientRefreshCoalescer coalescer = new ClientRefreshCoalescer(calls::incrementAndGet);

        coalescer.flush();

        assertEquals(0, calls.get());
    }

    @Test
    void scheduleDuringApplyRunsOnNextFlush() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ClientRefreshCoalescer> self = new AtomicReference<>();
        ClientRefreshCoalescer coalescer = new ClientRefreshCoalescer(() -> {
            if (calls.incrementAndGet() == 1) {
                self.get().schedule();
            }
        });
        self.set(coalescer);

        coalescer.schedule();
        coalescer.flush();
        assertEquals(1, calls.get());

        coalescer.flush();
        assertEquals(2, calls.get());

        coalescer.flush();
        assertEquals(2, calls.get());
    }
}
