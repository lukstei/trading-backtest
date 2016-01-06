package org.lst.trading.lib.model;

import java.time.Instant;

public interface Order {
    int getId();
    int getAmount();
    double getOpenPrice();
    Instant getOpenInstant();
    String getInstrument();
    boolean isLong();

    static int getSideSign(Order order) {
        return getSideSign(order.isLong());
    }

    static int getSideSign(boolean isLong) {
        return isLong ? 1 : -1;
    }

    static double getPl(Order order, double currentPrice) {
        return order.getAmount() * (currentPrice - order.getOpenPrice()) * getSideSign(order);
    }
}
