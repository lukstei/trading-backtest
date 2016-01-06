package org.lst.trading.lib.util.yahoo;

import java.time.Instant;

public class Quote {
    String mSymbol;
    double mPrice;
    Instant mInstant;

    public Quote(String symbol, double price, Instant instant) {
        mSymbol = symbol;
        mPrice = price;
        mInstant = instant;
    }

    public String getSymbol() {
        return mSymbol;
    }


    public double getPrice() {
        return mPrice;
    }

    public Instant getInstant() {
        return mInstant;
    }
}
