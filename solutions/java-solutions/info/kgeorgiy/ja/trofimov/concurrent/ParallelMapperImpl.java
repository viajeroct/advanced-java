package info.kgeorgiy.ja.trofimov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Class for parallelizing specified task on list on specified
 * number of threads. Multiple clients can have access to one ParallelMapperImpl
 * object through {@link #map(Function, List)} function.
 * Implements {@code ParallelMapper} interface.
 *
 * @see #map(Function, List) - main function for parallelizing
 */
public class ParallelMapperImpl implements ParallelMapper {
    /**
     * Pool of threads, which are creating in constructor.
     * Threads are initialized only once for one object.
     */
    private final List<Thread> pool;

    /**
     * Queue of tasks, from which threads from {@link #pool} will take
     * them and execute.
     */
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    /**
     * Constructor, needs to specify number of threads.
     *
     * @param threads in which task is parallelizing
     * @throws IllegalArgumentException if number of threads is less or equal to 0
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("At least one thread must be specified.");
        }

        pool = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            pool.add(new Thread(() -> {
                try {
                    Runnable task;
                    while (true) {
                        synchronized (this) {
                            while (tasks.isEmpty()) {
                                wait();
                            }
                            task = tasks.remove();
                        }
                        task.run();
                    }
                } catch (InterruptedException ignored) {
                }
            }));
            pool.get(i).start();
        }
    }

    /**
     * Main function, which parallels specified tasks on specified list.
     *
     * @param f    function for applying on the list
     * @param args list on which function {@code f} will be applied
     * @param <T>  type of input list
     * @param <R>  type of mapped (output) list
     * @return mapped list
     * @throws InterruptedException if {@link Counter#await()} throws it
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        Counter counter = new Counter(args.size());

        for (int i = 0; i < args.size(); i++) {
            int finalI = i;
            synchronized (this) {
                tasks.add(() -> {
                    result.set(finalI, f.apply(args.get(finalI)));
                    counter.increment();
                });
                notifyAll();
            }
        }

        counter.await();
        return result;
    }

    /**
     * Interrupts all threads in pool using {@link Thread#interrupt()}.
     */
    @Override
    public void close() {
        pool.forEach(Thread::interrupt);
    }

    /**
     * Class for synchronized counting, it is full analog of
     * {@link java.util.concurrent.CountDownLatch}.
     */
    private static class Counter {
        /**
         * Number of done counts, initially it is zero.
         */
        private int done = 0;

        /**
         * Number of counts we need, must be specified by constructor.
         */
        private final int awaiting;

        /**
         * Constructor.
         *
         * @param awaiting number of counts we're awaiting
         */
        public Counter(int awaiting) {
            this.awaiting = awaiting;
        }

        /**
         * Increments {@link #done} variable, if {@link #done} is equal to {@link #awaiting},
         * call {@link #notify()} function.
         */
        public synchronized void increment() {
            done++;
            if (done == awaiting) {
                notify();
            }
        }

        /**
         * Waits while {@link #done} is less than {@link #awaiting} using
         * nonblocking {@link #wait()} function.
         *
         * @throws InterruptedException if {@link #wait()} throws it
         */
        public synchronized void await() throws InterruptedException {
            while (done < awaiting) {
                wait();
            }
        }
    }
}
