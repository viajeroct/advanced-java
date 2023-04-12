package info.kgeorgiy.java.advanced.concurrent;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Full tests for easy version
 * of <a href="https://www.kgeorgiy.info/courses/java-advanced/homeworks.html#homework-concurrent">Iterative parallelism</a> homework
 * for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScalarIPTest<P extends ScalarIP> extends BaseIPTest<P> {
    public static final int PROCESSORS = 4; //Runtime.getRuntime().availableProcessors();

    public ScalarIPTest() {
    }

    @Test
    public void test01_maximum() throws InterruptedException {
        test(Collections::max, ScalarIP::maximum, COMPARATORS);
    }

    @Test
    public void test02_minimum() throws InterruptedException {
        test(Collections::min, ScalarIP::minimum, COMPARATORS);
    }

    @Test
    public void test03_all() throws InterruptedException {
        testS(Stream::allMatch, ScalarIP::all, PREDICATES);
    }

    @Test
    public void test04_any() throws InterruptedException {
        testS(Stream::anyMatch, ScalarIP::any, PREDICATES);
    }

    @Test
    public void test05_count() throws InterruptedException {
        testS((stream, predicate) -> (int) stream.filter(predicate).count(), ScalarIP::count, PREDICATES);
    }

    @Test
    public void test10_sleepPerformance() throws InterruptedException {
        testPerformance(50, 10, 2, (instance, threads, values) ->
                instance.maximum(threads, values, (o1, o2) -> sleep(Integer.compare(o1, o2))));
        testPerformance(100, 10, 2, (instance, threads, values) ->
                instance.count(threads, values, v -> sleep(v % 3 == 1)));
    }

    @Test
    public void test11_burnPerformance() throws InterruptedException {
        testPerformance(100 * PROCESSORS, PROCESSORS, 1.5, (instance, threads, values) ->
                instance.maximum(threads, values, (o1, o2) -> burn(Integer.compare(o1, o2))));
        testPerformance(100 * PROCESSORS, PROCESSORS, 2, (instance, threads, values) ->
                instance.count(threads, values, v1 -> burn(v1 % 3 == 1)));
    }

    private void testPerformance(
            final int size,
            final int threads,
            final double delta,
            final ConcurrentConsumer<P, Integer, List<Integer>> consumer
    ) throws InterruptedException {
        final double speedup = speedup(size, consumer, threads);
        Assert.assertTrue(String.format("Lower bound hit: %.1f", speedup), speedup > 1 / delta);
        Assert.assertTrue(String.format("Upper bound hit: %.1f", speedup), speedup < delta);
    }

    protected double speedup(
            final int size,
            final ConcurrentConsumer<P, Integer, List<Integer>> consumer,
            final int threads
    ) throws InterruptedException {
        System.err.println("    Warm up");

        final int subtasks = getSubtasks(threads, threads);
        final List<Integer> data = List.copyOf(randomList(((size - 1) / subtasks + 1) * subtasks));

        for (int i = 0; i < 5; i++) {
            performance(threads, threads, data, consumer);
        }
        System.err.println("    Measurement");

        final double performance1 = performance(1, threads, data, consumer);
        final double performance2 = performance(threads, threads, data, consumer);
        final double speedup = performance2 / performance1;
        System.err.format("    Performance ratio %.1f for %d threads (%.1f %.1f ms/op)%n", speedup, threads, performance1, performance2);
        return speedup;
    }

    protected int getSubtasks(final int threads, final int totalThreads) {
        return threads;
    }

    private <T> double performance(
            final int threads,
            final int totalThreads,
            final List<T> data,
            final ConcurrentConsumer<P, Integer, List<T>> consumer
    ) throws InterruptedException {
        final int subtasks = getSubtasks(threads, totalThreads);
        assert subtasks % threads == 0 && data.size() % subtasks == 0;

        final P instance = createInstance(threads);

        final long start = System.nanoTime();
        consumer.accept(instance, subtasks, data);
        final long time = System.nanoTime() - start;

        final int sequential = subtasks - 1;
        final int parallel = (data.size() - subtasks) / threads;
        return time / 1e6 / (sequential + parallel);
    }

    private static <T> T sleep(final T result) {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return result;
    }

    private static <T> T burn(final T result) {
        int total = result.hashCode();
        for (int i = 0; i < 10_000_000; i++) {
            total += i;
        }
        if (total == result.hashCode()) {
            throw new AssertionError("No burn");
        }
        return result;
    }
}
