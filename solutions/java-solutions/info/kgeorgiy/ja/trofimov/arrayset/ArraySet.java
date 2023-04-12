package info.kgeorgiy.ja.trofimov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(Collections.emptyList());
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    // :NOTE: коллекция может быть уже отсортирована
    public ArraySet(Collection<? extends T> collection,
                    Comparator<? super T> comparator) {
        if (! (collection instanceof SortedSet)) {
            TreeSet<T> treeSet = new TreeSet<>(comparator);
            treeSet.addAll(collection);
            this.data = new ArrayList<>(treeSet);
        } else {
            this.data = new ArrayList<>(collection);
        }
        this.comparator = comparator;
    }

    private ArraySet(List<T> data, Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    private void checkNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("List is empty");
        }
    }

    private T getValueOrNull(int index) {
        return index >= 0 && index < size() ? data.get(index) : null;
    }

    /*
    Returns `insertion point`.
    The insertion point is defined as the point at which the key would be
    inserted into the list: the index of the first element greater than the key.
     */
    private int binarySearch(T value) {
        int index = Collections.binarySearch(data, value, comparator);
        return index >= 0 ? index : (-1 - index);
    }

    // max e: e < value
    @Override
    public T lower(T value) {
        return getValueOrNull(getIndex(value, -1, -1));
    }

    // max e: e <= value
    @Override
    public T floor(T value) {
        return getValueOrNull(getIndex(value, 0, -1));
    }

    // min e: e >= value
    @Override
    public T ceiling(T value) {
        return getValueOrNull(getIndex(value, 0, 0));
    }

    // min e: e > value
    @Override
    public T higher(T value) {
        return getValueOrNull(getIndex(value, 1, 0));
    }

    /*
    lower index: shiftOne = -1, shiftTwo = -1
    floor index: shiftOne = 0, shiftTwo = -1
    ceiling index: shiftOne = 0, shiftTwo = 0
    higher index: shiftOne = +1, shiftTwo = 0
     */
    private int getIndex(T value, int shiftOne, int shiftTwo) {
        int index = binarySearch(value);
        return (index < size() && compare(data.get(index), value) == 0) ?
                (index + shiftOne) : (index + shiftTwo);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException("pollFirst is not supported");
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException("pollLast is not supported");
    }

    // :NOTE: здесь нужно О(1)
    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    // :NOTE: здесь нужно О(1)
    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object obj) {
        return obj != null && (Collections.binarySearch(data, (T) obj, comparator) >= 0);
    }

    @SuppressWarnings("unchecked")
    private int compare(T obj1, T obj2) {
        if (comparator != null) {
            return comparator.compare(obj1, obj2);
        }
        if (obj1 instanceof Comparable) {
            return ((Comparable<? super T>) obj1).compareTo(obj2);
        } else {
            throw new IllegalArgumentException("Comparator is not defined and `T` can't be compared");
        }
    }

    private NavigableSet<T> getSubSet(boolean fromStart, T fromElement, boolean fromInclusive,
                                      boolean toEnd, T toElement, boolean toInclusive) {
        int start = fromStart ? 0 : getIndex(fromElement, fromInclusive ? 0 : 1, 0);
        int end = toEnd ? size() - 1 : getIndex(toElement, toInclusive ? 0 : -1, -1);
        return new ArraySet<>(data.subList(start, end + 1), comparator);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Left border can't be greater than the right one");
        }
        return getSubSet(false, fromElement, fromInclusive, false, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return getSubSet(true, null, false, false, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return getSubSet(false, fromElement, inclusive, true, null, true);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public T first() {
        checkNotEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkNotEmpty();
        return data.get(size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }
}
