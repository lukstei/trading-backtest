package org.lst.trading.lib.series;

import java.util.List;
import java.util.function.Function;

public class DoubleSeries extends TimeSeries<Double> {
    String mName;

    DoubleSeries(List<Entry<Double>> data, String name) {
        super(data);
        mName = name;
    }

    public DoubleSeries(String name) {
        super();
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public DoubleSeries merge(DoubleSeries other, MergeFunction<Double, Double> f) {
        return new DoubleSeries(DoubleSeries.merge(this, other, f).mData, mName);
    }

    public DoubleSeries mapToDouble(Function<Double, Double> f) {
        return new DoubleSeries(map(f).mData, mName);
    }

    public DoubleSeries plus(DoubleSeries other) {
        return merge(other, (x, y) -> x + y);
    }

    public DoubleSeries plus(double other) {
        return mapToDouble(x -> x + other);
    }

    public DoubleSeries mul(DoubleSeries other) {
        return merge(other, (x, y) -> x * y);
    }

    public DoubleSeries mul(double factor) {
        return mapToDouble(x -> x * factor);
    }

    public DoubleSeries div(DoubleSeries other) {
        return merge(other, (x, y) -> x / y);
    }

    public DoubleSeries returns() {
        return this.div(lag(1)).plus(-1);
    }

    public double getLast() {
        return getData().get(size() - 1).getItem();
    }

    public DoubleSeries tail(int n) {
        return new DoubleSeries(getData().subList(size() - n, size()), getName());
    }

    public DoubleSeries returns(int days) {
        return this.div(lag(days)).plus(-1);
    }

    public double[] toArray() {
        return stream().mapToDouble(Entry::getItem).toArray();
    }

    @Override public DoubleSeries toAscending() {
        return new DoubleSeries(super.toAscending().mData, getName());
    }

    @Override public DoubleSeries toDescending() {
        return new DoubleSeries(super.toDescending().mData, getName());
    }

    @Override public DoubleSeries lag(int k) {
        return new DoubleSeries(super.lag(k).mData, getName());
    }

    @Override public String toString() {
        return mData.isEmpty() ? "DoubleSeries{empty}" :
            "DoubleSeries{" +
                "mName=" + mName +
                ", from=" + mData.get(0).getInstant() +
                ", to=" + mData.get(mData.size() - 1).getInstant() +
                ", size=" + mData.size() +
                '}';
    }
}
