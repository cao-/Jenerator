package org.cao.jenerator;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class Jenerator<T> implements Iterable<T> {
    private final Thread generatorThread;
    private volatile Thread callingThread;
    private volatile Throwable throwable;
    private volatile boolean shouldRunGenerator; // used to prevent threads from spuriously unpark
    private volatile T value;

    public Jenerator(Consumer<Consumer<T>> consumerConsumer) {
        AtomicReference<Thread> threadReference = new AtomicReference<>();
        threadReference.set(Thread.ofVirtual().unstarted(() -> {
            while (!shouldRunGenerator) LockSupport.park();
            consumerConsumer.accept(value -> {
                Objects.requireNonNull(value, "A Jenerator cannot yield null values.");
                if (Thread.currentThread() != threadReference.get())
                    throw new IllegalStateException("Values can be yielded only from the main generator thread");
                this.value = value;
                shouldRunGenerator = false;
                LockSupport.unpark(callingThread);
                while (!shouldRunGenerator) LockSupport.park();
            });
            shouldRunGenerator = false;
            LockSupport.unpark(callingThread);
        }));
        generatorThread = threadReference.get();
        generatorThread.setUncaughtExceptionHandler((t, e) -> {
            shouldRunGenerator = false;
            LockSupport.unpark(callingThread);
            throwable = e;
        });
        generatorThread.start();
    }

    private T resume() {
        callingThread = Thread.currentThread();
        shouldRunGenerator = true;
        LockSupport.unpark(generatorThread);
        while (shouldRunGenerator) LockSupport.park();
        if (throwable != null) throw new RuntimeException(throwable);
        if (value == null) return null;
        T result = value;
        value = null;
        return result;
    }

    private final Iterator<T> iterator = new Iterator<>() {
        T next;
        boolean ended;

        @Override
        public boolean hasNext() {
            if (next != null) return true;
            if (ended) return false;
            next = resume();
            if (next == null) ended = true;
            return hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            T t = next;
            next = null;
            return t;
        }
    };

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }
}
