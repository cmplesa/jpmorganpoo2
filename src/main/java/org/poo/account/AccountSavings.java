package org.poo.account;

import lombok.Data;
import org.poo.Components.Card;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a savings bank account.
 */
@Data
public final class AccountSavings implements Account {
    private static final int TRANSACTION_THRESHOLD_2 = 2;
    private static final int TRANSACTION_THRESHOLD_5 = 5;
    private static final int TRANSACTION_THRESHOLD_10 = 10;

    private String iban;
    private double balance;
    private String currency;
    private double interestRate;
    private ArrayList<Card> cards;
    private String alias;
    private double minimumBalance;
    private Integer nrOfTransactions;
    private Map<Integer, Boolean> hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy;
    private Map<String, Double> moneySpentAtCommerciantsWithCashbackStrategyThreshold;
    // the type of commercian -> the number of transactions
    private Map<String, Integer> nrOfTransactionsPerCommerciants;

    /**
     * Constructs a savings account with the specified currency and interest rate.
     *
     * @param currency the currency of the account
     * @param interestRate the interest rate of the account
     */
    public AccountSavings(final String currency, final double interestRate) {
        this.iban = Utils.generateIBAN();
        this.balance = 0;
        this.currency = currency;
        this.interestRate = interestRate;
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
     * @return the type of the account ("savings")
     */
    @Override
    public String getAccountType() {
        return "savings";
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
     * @param numberOfTransactions the number of transactions
     */
    @Override
    public void updateCashbackStatus(final int numberOfTransactions) {
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(numberOfTransactions, true);
    }

    public Map<String, Integer> getNrOfTransactionsPerCommerciants() {
        return this.nrOfTransactionsPerCommerciants;
    }
}
