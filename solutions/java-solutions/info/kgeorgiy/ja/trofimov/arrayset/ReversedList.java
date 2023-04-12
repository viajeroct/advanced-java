package info.kgeorgiy.ja.trofimov.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class ReversedList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> data;
    private boolean isNormalOrder = false;

    public ReversedList(List<T> data) {
        if (data instanceof ReversedList<T> castedData) {
            this.data = castedData.data;
            this.isNormalOrder = !castedData.isNormalOrder;
        } else {
            this.data = data;
        }
    }

    @Override
    public T get(int index) {
        return (isNormalOrder ? data.get(index) : data.get(size() - 1 - index));
    }

    @Override
    public int size() {
        return data.size();
    }
}
