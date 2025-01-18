package org.poo.account;

import org.poo.Components.ExchangeRate;

import java.util.ArrayList;

/**
 * Factory class for creating different types of accounts.
 */
public final class AccountFactory {

    // Private constructor to prevent instantiation
    private AccountFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates an account of the specified type.
     *
     * @param accountType the type of the account
     * @param currency the currency of the account
     * @param interestRate the interest rate of the account
     *                     (optional, can be 0 for non-savings accounts)
     * @return the created account
     * @throws IllegalArgumentException if the account type is unknown
     */
    public static Account createAccount(final String accountType, final String currency,
                                        final double interestRate, final String email,
                                        final ArrayList<ExchangeRate> exchangeRates) {
        switch (accountType.toLowerCase()) {
            case "classic":
                return new AccountClassic(currency);
            case "savings":
                return new AccountSavings(currency, interestRate);
            case "business":
                return new AccountBusiness(currency, email, exchangeRates);
            default:
                throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }
}
