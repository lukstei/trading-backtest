package org.lst.trading.lib.series;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lst.trading.lib.util.Util.check;

public class TimeSeries<T> implements Iterable<TimeSeries.Entry<T>> {
    public static class Entry<T> {
        T mT;
        Instant mInstant;

        public Entry(T t, Instant instant) {
            mT = t;
            mInstant = instant;
        }

        public T getItem() {
            return mT;
        }

        public Instant getInstant() {
            return mInstant;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (!mInstant.equals(entry.mInstant)) return false;
            if (mT != null ? !mT.equals(entry.mT) : entry.mT != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = mT != null ? mT.hashCode() : 0;
            result = 31 * result + mInstant.hashCode();
            return result;
        }

        @Override public String toString() {
            return "Entry{" +
                "mInstant=" + mInstant +
                ", mT=" + mT +
                '}';
        }
    }

    List<Entry<T>> mData;

    public TimeSeries() {
        mData = new ArrayList<>();
    }

    protected TimeSeries(List<Entry<T>> data) {
        mData = data;
    }

    public int size() {
        return mData.size();
    }

    public boolean isEmpty() {
        return mData.isEmpty();
    }

    public boolean add(Entry<T> tEntry) {
        return mData.add(tEntry);
    }

    public void add(T item, Instant instant) {
        add(new Entry<T>(item, instant));
    }

    public Stream<Entry<T>> stream() {
        return mData.stream();
    }

    public Stream<Entry<T>> reversedStream() {
        check(!(mData instanceof LinkedList));
        return IntStream.range(1, mData.size() + 1).mapToObj(i -> mData.get(mData.size() - i));
    }

    @Override public Iterator<Entry<T>> iterator() {
        return mData.iterator();
    }

    public List<Entry<T>> getData() {
        return Collections.unmodifiableList(mData);
    }

    public Entry<T> get(int index) {
        return mData.get(index);
    }

    public interface MergeFunction<T, F> {
        F merge(T t1, T t2);
    }

    public interface MergeFunction2<T1, T2, F> {
        F merge(T1 t1, T2 t2);
    }

    public <F> TimeSeries<F> map(Function<T, F> f) {
        List<Entry<F>> newEntries = new ArrayList<>(size());
        for (Entry<T> entry : mData) {
            newEntries.add(new Entry<>(f.apply(entry.mT), entry.mInstant));
        }
        return new TimeSeries<>(newEntries);
    }

    public boolean isAscending() {
        return size() <= 1 || get(0).getInstant().isBefore(get(1).mInstant);
    }

    public TimeSeries<T> toAscending() {
        if (!isAscending()) {
            return reverse();
        }
        return this;
    }

    public TimeSeries<T> toDescending() {
        if (isAscending()) {
            return reverse();
        }
        return this;
    }

    public TimeSeries<T> reverse() {
        ArrayList<Entry<T>> entries = new ArrayList<>(mData);
        Collections.reverse(entries);
        return new TimeSeries<>(entries);
    }

    public TimeSeries<T> lag(int k) {
        return lag(k, false, null);
    }

    public TimeSeries<T> lag(int k, boolean addEmpty, T emptyVal) {
        check(k > 0);
        check(mData.size() >= k);

        ArrayList<Entry<T>> entries = new ArrayList<>(addEmpty ? mData.size() : mData.size() - k);
        if (addEmpty) {
            for (int i = 0; i < k; i++) {
                entries.add(new Entry<>(emptyVal, mData.get(i).mInstant));
            }
        }

        for (int i = k; i < size(); i++) {
            entries.add(new Entry<>(mData.get(i - k).getItem(), mData.get(i).getInstant()));
        }

        return new TimeSeries<>(entries);
    }

    public static <T1, T2, F> TimeSeries<F> merge(TimeSeries<T1> t1, TimeSeries<T2> t2, MergeFunction2<T1, T2, F> f) {
        check(t1.isAscending());
        check(t2.isAscending());

        Iterator<Entry<T1>> i1 = t1.iterator();
        Iterator<Entry<T2>> i2 = t2.iterator();

        List<Entry<F>> newEntries = new ArrayList<>();

        while (i1.hasNext() && i2.hasNext()) {
            Entry<T1> n1 = i1.next();
            Entry<T2> n2 = i2.next();

            while (!n2.mInstant.equals(n1.mInstant)) {
                if (n1.mInstant.isBefore(n2.mInstant)) {
                    while (i1.hasNext()) {
                        n1 = i1.next();
                        if (!n1.mInstant.isBefore(n2.mInstant)) {
                            break;
                        }
                    }
                } else if (n2.mInstant.isBefore(n1.mInstant)) {
                    while (i2.hasNext()) {
                        n2 = i2.next();
                        if (!n2.mInstant.isBefore(n1.mInstant)) {
                            break;
                        }
                    }
                }
            }

            if (n2.mInstant.equals(n1.mInstant)) {
                newEntries.add(new Entry<F>(f.merge(n1.mT, n2.mT), n1.mInstant));
            }
        }

        return new TimeSeries<>(newEntries);
    }

    public static <T, F> TimeSeries<F> merge(TimeSeries<T> t1, TimeSeries<T> t2, MergeFunction<T, F> f) {
        return TimeSeries.<T, T, F>merge(t1, t2, f::merge);
    }

    @Override public String toString() {
        return mData.isEmpty() ? "TimeSeries{empty}" :
            "TimeSeries{" +
                "from=" + mData.get(0).getInstant() +
                ", to=" + mData.get(size() - 1).getInstant() +
                ", size=" + mData.size() +
                '}';
    }
}
