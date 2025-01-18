package org.poo.account;

import org.poo.Components.Card;

import java.util.ArrayList;
import java.util.Map;

/**
 * Interface representing a generic bank account.
 */
public interface Account {

    /**
     * Deposits a specified amount into the account.
     *
     * @param amount the amount to deposit
     */
    void deposit(double amount);

    /**
     * Withdraws a specified amount from the account.
     *
     * @param amount the amount to withdraw
     */
    void withdraw(double amount);

    /**
     * Retrieves the International Bank Account Number (IBAN) of the account.
     *
     * @return the IBAN of the account
     */
    String getIBAN();

    /**
     * Retrieves the currency in which the account operates.
     *
     * @return the currency of the account
     */
    String getCurrency();

    /**
     * Retrieves the type of the account (e.g., classic, savings).
     *
     * @return the type of the account
     */
    String getAccountType();

    /**
     * Retrieves the current balance of the account.
     *
     * @return the current balance
     */
    double getBalance();

    /**
     * Retrieves the list of cards associated with the account.
     *
     * @return the list of cards
     */
    ArrayList<Card> getCards();

    /**
     * Sets the balance of the account.
     *
     * @param balance the new balance to set
     */
    void setBalance(double balance);

    /**
     * Sets an alias for the account.
     *
     * @param alias the alias to set for the account
     */
    void setAlias(String alias);

    /**
     * Retrieves the alias of the account, if any.
     *
     * @return the alias of the account
     */
    String getAlias();

    /**
     * Sets the minimum balance allowed for the account.
     *
     * @param minimumBalance the minimum balance to set
     */
    void setMinimumBalance(double minimumBalance);

    /**
     * Retrieves the minimum balance allowed for the account.
     *
     * @return the minimum balance
     */
    double getMinimumBalance();

    /**
     * Sets the interest rate for the account (if applicable).
     *
     * @param interestRate the interest rate to set
     */
    void setInterestRate(double interestRate);

    /**
     * Retrieves the interest rate of the account (if applicable).
     *
     * @return the interest rate
     */
    double getInterestRate();

    /**
     * Updates the cashback status based on the number of transactions.
     *
     * @param nrOfTransactions the number of transactions
     */
    void updateCashbackStatus(int nrOfTransactions);

    /**
     * Retrieves the number of transactions per commerciant.
     *
     * @return a map of commerciant names to the number of transactions
     */
    Map<String, Integer> getNrOfTransactionsPerCommerciants();

    /**
     * Retrieves the money spent at commerciants with a cashback strategy threshold.
     *
     * @return a map of commerciant names to the money spent
     */
    Map<String, Double> getMoneySpentAtCommerciantsWithCashbackStrategyThreshold();
}
