package com.gabinx.chapters.logic;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Marks a client-side refresh dirty and applies it at most once per flush cycle.
 * Callers schedule from packet handlers; a tick end (or test) flushes.
 * The applicator runs outside any coalescer lock so it can take its own locks (JEI plugin) safely.
 */
public final class ClientRefreshCoalescer {
    private final Runnable applicator;
    private final AtomicBoolean dirty = new AtomicBoolean();

    public ClientRefreshCoalescer(Runnable applicator) {
        this.applicator = Objects.requireNonNull(applicator, "applicator");
    }

    public void schedule() {
        dirty.set(true);
    }

    public void flush() {
        if (dirty.getAndSet(false)) {
            applicator.run();
        }
    }
}
