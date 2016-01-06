package org.lst.trading.lib.series;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MultipleDoubleSeries extends TimeSeries<List<Double>> {
    List<String> mNames;

    public MultipleDoubleSeries(Collection<String> names) {
        mNames = new ArrayList<>(names);
    }

    public MultipleDoubleSeries(DoubleSeries... series) {
        mNames = new ArrayList<>();
        for (int i = 0; i < series.length; i++) {
            if (i == 0) {
                _init(series[i]);
            } else {
                addSeries(series[i]);
            }
        }
    }

    void _init(DoubleSeries series) {
        mData = new ArrayList<>();
        for (Entry<Double> entry : series) {
            LinkedList<Double> list = new LinkedList<>();
            list.add(entry.mT);
            add(new Entry<>(list, entry.mInstant));
        }
        mNames.add(series.mName);
    }

    public void addSeries(DoubleSeries series) {
        mData = TimeSeries.merge(this, series, (l, t) -> {
            l.add(t);
            return l;
        }).mData;
        mNames.add(series.mName);
    }

    public DoubleSeries getColumn(String name) {
        int index = getNames().indexOf(name);
        List<Entry<Double>> entries = mData.stream().map(t -> new Entry<Double>(t.getItem().get(index), t.getInstant())).collect(toList());
        return new DoubleSeries(entries, name);
    }

    public int indexOf(String name) {
        return mNames.indexOf(name);
    }

    public List<String> getNames() {
        return mNames;
    }

    @Override public String toString() {
        return mData.isEmpty() ? "MultipleDoubleSeries{empty}" :
            "MultipleDoubleSeries{" +
                "mNames={" + mNames.stream().collect(joining(", ")) +
                ", from=" + mData.get(0).getInstant() +
                ", to=" + mData.get(mData.size() - 1).getInstant() +
                ", size=" + mData.size() +
                '}';
    }
}
