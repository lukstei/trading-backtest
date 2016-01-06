package org.lst.trading.lib.model;

import java.time.Instant;

public interface Order {
    int getId();

    int getAmount();

    double getOpenPrice();

    Instant getOpenInstant();

    String getInstrument();

    default boolean isLong() {
        return getAmount() > 0;
    }

    default boolean isShort() {
        return !isLong();
    }

    default int getSign() {
        return isLong() ? 1 : -1;
    }

    default double calculatePl(double currentPrice) {
        return getAmount() * (currentPrice - getOpenPrice());
    }
}
