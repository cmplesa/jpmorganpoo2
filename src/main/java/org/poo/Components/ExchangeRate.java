package org.poo.Components;

import lombok.Data;

@Data
public class ExchangeRate {
    private String currencyFrom;
    private String currencyTo;
    private double rate;

    public ExchangeRate(final String currencyFrom, final String currencyTo,
                        final double rate) {
        this.currencyFrom = currencyFrom;
        this.currencyTo = currencyTo;
        this.rate = rate;
    }


}
