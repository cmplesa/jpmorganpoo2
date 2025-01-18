package org.poo.Components;

import lombok.Data;

/**
 * Represents a commerciant.
 */
@Data
public final class Commerciant {
    private String name;
    private int id;
    private String account;
    private String type;
    private String cashbackStrategy;
    private Double revenue;

    /**
     * Constructs a commerciant with the specified details.
     *
     * @param name             the name of the commerciant
     * @param id               the ID of the commerciant
     * @param account          the IBAN of the commerciant
     * @param type             the type of the commerciant
     * @param cashbackStrategy the cashback strategy of the commerciant
     */
    public Commerciant(final String name, final int id, final String account,
                       final String type, final String cashbackStrategy) {
        this.name = name;
        this.id = id;
        this.account = account;
        this.type = type;
        this.cashbackStrategy = cashbackStrategy;
        this.revenue = 0.0;
    }
}
