package org.cao.jenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JeneratorTest {

    @Test
    @DisplayName("Test the behaviour of iterator")
    public void testIterator() {
        Function<Integer, Jenerator<Integer>> firstN = n -> new Jenerator<>(yield -> {
            int i = 1;
            while (i <= n) yield.accept(i++);
        });

        var jen = firstN.apply(5);
        var it = jen.iterator();

        // the returned iterator is always the same
        assertSame(it, jen.iterator());
        // next and hasNext respect the iterator contract
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertTrue(it.hasNext());
        assertEquals(4, it.next());
        assertEquals(5, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    @DisplayName("A generator that doesn't yield produces an empty iterator")
    public void testEmpty() {
        var jen = new Jenerator<Void>(yield -> {
        });

        assertThrows(NoSuchElementException.class, jen.iterator()::next);
    }

    @Test
    @DisplayName("Test that no computation is done unless requested")
    public void testLazy() throws InterruptedException {

        AtomicInteger i = new AtomicInteger(0);

        Jenerator<Integer> jen = new Jenerator<>(yield -> {
            while (true) yield.accept(i.incrementAndGet());
        });

        var it = jen.iterator();
        // the code passed to the Jenerator is not started after the creation of the Jenerator
        Thread.sleep(1000);
        assertEquals(0, i.get());
        // when asked for the next element, the Jenerator starts the code and pauses it at the first yield point
        it.next();
        Thread.sleep(1000);
        // when asked again for the next element, the code is resumed and then paused at the next yield point
        assertEquals(1, i.get());
        // in order to know if there is another element we must resume the code and see if it yields something
        it.hasNext();
        Thread.sleep(1000);
        assertEquals(2, i.get());
        // if then we ask for the next element, the value has already been computed and available
        it.next();
        Thread.sleep(1000);
        assertEquals(2, i.get());
    }


    @Test
    @DisplayName("Test what happens when a generator yields values produced by another generator")
    public void testYieldFrom() {
        Jenerator<String> lowercase = new Jenerator<>(yield -> Stream.of("a", "b", "c", "d").forEach(yield));
        Jenerator<String> uppercase = new Jenerator<>(yield -> lowercase.forEach(letter -> yield.accept(letter.toUpperCase())));

        assertEquals("A", uppercase.iterator().next());
        assertEquals("B", uppercase.iterator().next());
        // the first generator has advanced when the second did
        assertEquals("c", lowercase.iterator().next());
        // the second generator has advanced when the first did
        assertEquals("D", uppercase.iterator().next());
        assertThrows(NoSuchElementException.class, () -> lowercase.iterator().next());
        assertThrows(NoSuchElementException.class, () -> uppercase.iterator().next());
    }

    @Test
    @DisplayName("A Jenerator can be resumed from multiple threads")
    public void testResume() throws ExecutionException, InterruptedException {
        var jen = new Jenerator<Integer>(yield -> {
            yield.accept(1);
            yield.accept(2);
            yield.accept(3);
        });

        var it = jen.iterator();

        assertEquals(1, it.next());
        assertEquals(2, CompletableFuture.supplyAsync(it::next).get());
        assertEquals(3, CompletableFuture.supplyAsync(it::next).get());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    @DisplayName("Values can be yielded only from the main generator thread")
    public void testConcurrency() {
        var jen = new Jenerator<Integer>(yield -> IntStream.range(0, 100000).boxed().parallel().forEach(yield));

        assertThrows(RuntimeException.class, () -> jen.forEach(value -> {
        }));
    }

    @Test
    @DisplayName("Exceptions in the generator thread are propagated to the main thread")
    public void testExceptions() {
        class TestException extends RuntimeException {
        }

        var jen = new Jenerator<Void>(yield -> {
            throw new TestException();
        });

        var throwable = assertThrows(RuntimeException.class, () -> jen.iterator().next());
        assertTrue(throwable.getCause() instanceof TestException);
    }
}