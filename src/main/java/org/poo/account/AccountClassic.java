package org.poo.account;

import lombok.Data;
import org.poo.Components.Card;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a classic bank account.
 */
@Data
public final class AccountClassic implements Account {
    private static final int TRANSACTION_THRESHOLD_2 = 2;
    private static final int TRANSACTION_THRESHOLD_5 = 5;
    private static final int TRANSACTION_THRESHOLD_10 = 10;

    private String iban;
    private double balance;
    private String currency;
    private ArrayList<Card> cards;
    private String alias;
    private double minimumBalance;
    private Map<String, Integer> nrOfTransactionsPerCommerciants;
    private Map<Integer, Boolean> hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy;
    private Map<String, Double> moneySpentAtCommerciantsWithCashbackStrategyThreshold;

    /**
     * Default constructor for AccountClassic.
     */
    public AccountClassic() {
    }

    /**
     * Constructs a classic account with the specified currency.
     *
     * @param currency the currency of the account
     */
    public AccountClassic(final String currency) {
        this.iban = Utils.generateIBAN();
        this.balance = 0;
        this.currency = currency;
        this.cards = new ArrayList<>();
        this.minimumBalance = 0;
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy =
                new HashMap<>();
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_2, false);
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_5, false);
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_10, false);
        this.moneySpentAtCommerciantsWithCashbackStrategyThreshold = new HashMap<>();
        this.nrOfTransactionsPerCommerciants = new HashMap<>();
    }

    /**
     * Deposits an amount into the account.
     *
     * @param amount the amount to deposit
     */
    @Override
    public void deposit(final double amount) {
        this.balance += amount;
    }

    /**
     * Withdraws an amount from the account.
     *
     * @param amount the amount to withdraw
     */
    @Override
    public void withdraw(final double amount) {
        this.balance -= amount;
    }

    /**
     * Returns the type of the account.
     *
     * @return the type of the account ("classic")
     */
    @Override
    public String getAccountType() {
        return "classic";
    }

    /**
     * Sets the interest rate for the account.
     * This operation is not applicable for classic accounts.
     *
     * @param interestRate the interest rate to set
     */
    @Override
    public void setInterestRate(final double interestRate) {
        // Not applicable for classic accounts
    }

    /**
     * Gets the interest rate of the account. This operation is not applicable for classic accounts.
     *
     * @return 0 as interest rate is not applicable for classic accounts
     */
    @Override
    public double getInterestRate() {
        return 0;
    }

    /**
     * Gets the IBAN of the account.
     *
     * @return the IBAN of the account
     */
    @Override
    public String getIBAN() {
        return this.iban;
    }

    /**
     * Updates the cashback status based on the number of transactions.
     *
     * @param nrOfTransactions the number of transactions
     */
    @Override
    public void updateCashbackStatus(final int nrOfTransactions) {
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(nrOfTransactions, true);
    }

    /**
     * Retrieves the money spent at commerciants with cashback strategy threshold.
     *
     * @return the money spent at commerciants with cashback strategy threshold
     */
    @Override
    public Map<String, Double> getMoneySpentAtCommerciantsWithCashbackStrategyThreshold() {
        return this.moneySpentAtCommerciantsWithCashbackStrategyThreshold;
    }
}
