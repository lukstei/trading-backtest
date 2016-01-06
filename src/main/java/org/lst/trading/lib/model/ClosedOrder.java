package org.lst.trading.lib.model;

import java.time.Instant;

public interface ClosedOrder extends Order {
    double getClosePrice();
    Instant getCloseInstant();

    public default double getPl() {
        return Order.getPl(this, getClosePrice());
    }
}
