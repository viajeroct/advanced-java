package info.kgeorgiy.ja.trofimov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs in parallel task on {@link List} using main function
 * {@link #parallelizeFunction(int, List, Function, Function)}, which
 * accepts number of {@code threads} to parallel on.
 * Class implements {@link AdvancedIP} interface, may be specified {@link #parallelMapper}.
 * If {@link #parallelMapper} is specified, than class uses it to make task in parallel.
 */
public class IterativeParallelism implements AdvancedIP {
    /**
     * Special class for making taks in parallel.
     */
    private final ParallelMapper parallelMapper;

    /**
     * If you use this constructor, IterativeParallelism will use
     * {@link #parallelMapper} object to perform all operations.
     *
     * @param parallelMapper object of {@code ParallelMapper}
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Empty constructor.
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Function which joins list.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return string representation of {@code values}, using {@link Collectors#joining()}
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * Returns list, in which each element satisfies {@code predicate}.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @param <T>       type of list elements
     * @return filtered list
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Maps list using {@code f} mapper function.
     *
     * @param threads number of concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @param <T>     type of elements of the list
     * @param <U>     type of elements in resulting list, after applying function {@code f}
     * @return mapped list
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f)
            throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * Returns maximum element in the list.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        type of elements of the list
     * @return maximum value compared by {@code comparator}
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * Returns minimum element in the list.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        type of elements of the list
     * @return maximum value compared by {@code comparator}
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     * @see #maximum(int, List, Comparator)
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Checks some {@code predicate} on all elements in list.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of elements of the list
     * @return true if all elements in the list satisfy {@code predicate}, otherwise false
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(x -> x));
    }

    /**
     * Checks some {@code predicate} on any elements in list.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of elements of the list
     * @return true if any element in the list satisfy {@code predicate}, otherwise false
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    /**
     * Counts number of values that satisfy some predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       type of elements
     * @return number of elements, that satisfy {@code predicate}
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return parallelizeFunction(threads, values,
                stream -> stream.map(x -> predicate.test(x) ? 1 : 0).mapToInt(Integer::intValue).sum(),
                stream -> stream.mapToInt(Integer::intValue).sum());
    }

    /**
     * Divides {@code list} on approximately equal {@code threads} parts.
     *
     * @param list    which we divide on parts
     * @param threads number of parts we expect
     * @param <E>     type of data we divide
     * @return list of indexes, which are borders of divided blocks
     */
    private <E> List<Integer> divideInParts(List<E> list, int threads) {
        List<Integer> parts = new ArrayList<>();
        int fullBlocks = list.size() / threads;
        int restBlockSize = list.size() % threads;
        int left, right = 0;
        for (int i = 0; i < threads; i++) {
            left = right;
            right += fullBlocks + (restBlockSize-- > 0 ? 1 : 0);
            parts.add(left);
        }
        parts.add(list.size());
        return parts;
    }

    /**
     * Main function for parallelize given task {@code f} on {@code list},
     * after applying function for all elements, reduces them, using {@code reducer}.
     * If {@link #parallelMapper} is null, then is creates threads by itself,
     * else it uses {@link #parallelMapper}.
     *
     * @param threads number of threads
     * @param list    data
     * @param f       function which applies to each element of {@code list}
     * @param reducer function which collects results in different threads
     * @param <E>     type of list elements
     * @param <R>     type of result
     * @return result of applying function and reducing result
     * @throws InterruptedException if Thread throws it
     */
    private <E, R> R parallelizeFunction(int threads, List<E> list, Function<Stream<E>, R> f,
                                         Function<Stream<R>, R> reducer) throws InterruptedException {
        if (list == null) {
            throw new IllegalArgumentException("Can't parallelize f on null data.");
        }
        if (threads < 1) {
            throw new IllegalArgumentException("Number of threads must greater or equal to 1.");
        }
        threads = Math.max(1, Math.min(list.size(), threads));

        List<Integer> parts = divideInParts(list, threads);
        if (parallelMapper != null) {
            List<Stream<E>> data = new ArrayList<>();
            for (int i = 0; i + 1 < parts.size(); i++) {
                data.add(list.subList(parts.get(i), parts.get(i + 1)).stream());
            }
            return reducer.apply(parallelMapper.map(f, data).stream());
        } else {
            List<R> results = new ArrayList<>(Collections.nCopies(threads, null));
            List<Thread> pool = new ArrayList<>();

            for (int i = 0; i + 1 < parts.size(); i++) {
                final int finalI = i;
                Thread curThread = new Thread(() -> results.set(finalI,
                        f.apply(list.subList(parts.get(finalI), parts.get(finalI + 1)).stream())));
                curThread.start();
                pool.add(curThread);
            }

            for (int i = 0; i < pool.size(); i++) {
                try {
                    pool.get(i).join();
                } catch (InterruptedException e) {
                    for (int j = i + 1; j < pool.size(); j++) {
                        pool.get(j).interrupt();
                    }
                    throw e;
                }
            }

            return reducer.apply(results.stream());
        }
    }

    /**
     * Function, which returns lambda for reducing stream
     * using given monoid {@code monoid}.
     *
     * @param monoid monoid for reducer
     * @param <T>    type of monoid and Stream
     * @return returns lambda, which reduces stream into one value
     */
    private <T> Function<Stream<T>, T> getReducer(Monoid<T> monoid) {
        return stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    /**
     * Reduce list in parallel using {@code threads}, uses given {@code monoid}.
     *
     * @param threads number of threads
     * @param values  given array to which applies {@code monoid}
     * @param monoid  monoid to make reduce operation
     * @return single reduced value
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> reducer = getReducer(monoid);
        return parallelizeFunction(threads, values, reducer, reducer);
    }

    /**
     * Reduce list in parallel using {@code threads}, uses given {@code monoid},
     * preliminary mapped using {@code lift} function.
     *
     * @param threads number of threads
     * @param values  given array to which applies {@code monoid}
     * @param monoid  monoid to make reduce operation
     * @param <R>     type of result after applying {@code lift} mapping function
     * @param <T>     type of list elements
     * @return single reduced value
     * @throws InterruptedException if {@link #parallelizeFunction(int, List, Function, Function)}
     *                              throws it
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid)
            throws InterruptedException {
        Function<Stream<R>, R> reducer = getReducer(monoid);
        return parallelizeFunction(threads, values, stream -> reducer.apply(stream.map(lift)), reducer);
    }
}
