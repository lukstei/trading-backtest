package org.lst.trading.lib.model;

import java.time.Duration;
import java.time.Instant;

public interface Bar extends Comparable<Bar> {
    double getOpen();

    double getHigh();

    double getLow();

    double getClose();

    long getVolume();

    Instant getStart();

    Duration getDuration();

    double getWAP();

    default double getAverage() {
        return (getHigh() + getLow()) / 2;
    }

    @Override default int compareTo(Bar o) {
        return getStart().compareTo(o.getStart());
    }

    default Instant getEnd() {
        return getStart().plus(getDuration());
    }

}
