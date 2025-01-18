package org.poo.account;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.poo.Components.Card;
import org.poo.Components.BusinessComerciantPayment;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Pair;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Data
public final class AccountBusiness implements Account {
    private static final int SPENDING_LIMIT_RON = 500;
    private static final int DEPOSIT_LIMIT_RON = 500;
    private static final int TRANSACTION_THRESHOLD_2 = 2;
    private static final int TRANSACTION_THRESHOLD_5 = 5;
    private static final int TRANSACTION_THRESHOLD_10 = 10;

    private String iban;
    private double balance;
    private String currency;
    private ArrayList<Card> cards;
    private String alias;
    private double minimumBalance;
    private Map<String, String> associates; // Email -> Role (owner, manager, employee)
    private double depositLimit;
    private double spendingLimit;
    // the type of commerciant -> the number of transactions
    private Map<String, Integer> nrOfTransactionsPerCommerciants;
    @Setter
    private Integer nrOfTransactions;
    private Map<Integer, Boolean> hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy;
    @Getter
    private Map<String, Double> moneySpentAtCommerciantsWithCashbackStrategyThreshold;
    private Map<String, BusinessComerciantPayment> commerciantsPayments;

    private Map<String, Double> deposits; // Email -> Total deposited amount
    private Map<String, Double> spending; // Email -> Total spent amount

    // order tracker of associates added to the account
    private Map<String, Integer> orderTracker;

    public AccountBusiness() {
        this.deposits = new HashMap<>();
        this.spending = new HashMap<>();
    }

    public AccountBusiness(final String currency, final String email,
                           final ArrayList<ExchangeRate> exchangeRates) {
        this.iban = Utils.generateIBAN();
        this.balance = 0;
        this.currency = currency;
        this.cards = new ArrayList<>();
        this.minimumBalance = 0;
        this.associates = new HashMap<>();
        this.associates.put(email, "owner");
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy = new HashMap<>();
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_2, false);
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_5, false);
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(TRANSACTION_THRESHOLD_10, false);
        this.moneySpentAtCommerciantsWithCashbackStrategyThreshold = new HashMap<>();
        this.nrOfTransactionsPerCommerciants = new HashMap<>();
        this.deposits = new HashMap<>();
        this.spending = new HashMap<>();
        this.commerciantsPayments = new HashMap<>();
        this.orderTracker = new HashMap<>();

        // Convert limits to the account currency
        this.spendingLimit = convertCurrency("RON", currency, SPENDING_LIMIT_RON, exchangeRates);
        this.depositLimit = convertCurrency("RON", currency, DEPOSIT_LIMIT_RON, exchangeRates);
    }

    /**
     * Adds an associate to the account.
     *
     * @param email the email of the associate
     * @param role the role of the associate
     * @param ownerEmail the email of the owner
     */
    public void addAssociate(final String email, final String role, final String ownerEmail) {
        if (!associates.containsKey(ownerEmail) || !associates.get(ownerEmail).equals("owner")) {
            throw new IllegalArgumentException("Only the owner can add new associates.");
        }
        if (role.equalsIgnoreCase("owner") && associates.containsValue("owner")) {
            throw new IllegalArgumentException("An account can only have one owner.");
        }
        associates.put(email, role.toLowerCase());
    }

    /**
     * Sets the minimum balance for the account.
     *
     * @param amount the minimum balance amount
     * @param ownerEmail the email of the owner
     */
    public void setMinimumBalance(final double amount, final String ownerEmail) {
        if (!associates.containsKey(ownerEmail) || !associates.get(ownerEmail).equals("owner")) {
            throw new IllegalArgumentException("Only the owner can set the minimum balance.");
        }
        this.minimumBalance = amount;
    }

    /**
     * Deletes the account.
     *
     * @param ownerEmail the email of the owner
     */
    public void deleteAccount(final String ownerEmail) {
        if (!associates.containsKey(ownerEmail) || !associates.get(ownerEmail).equals("owner")) {
            throw new IllegalArgumentException("Only the owner can delete the account.");
        }
        this.associates.clear();
        this.cards.clear();
        this.balance = 0;
        this.minimumBalance = 0;
    }

    @Override
    public void deposit(final double amount) {
        this.balance += amount;
    }

    /**
     * Deposits an amount to the account by an associate.
     *
     * @param amount the amount to deposit
     * @param email the email of the associate
     */
    public void deposit(final double amount, final String email) {
        if (amount > this.depositLimit && this.associates.get(email).equals("employee")) {
            return;
        }
        this.balance += amount;
        this.deposits.put(email, this.deposits.getOrDefault(email, 0.0) + amount);
    }

    @Override
    public void withdraw(final double amount) {
        if (this.balance - amount < this.minimumBalance) {
            throw new IllegalArgumentException("Insufficient balance or below minimum balance.");
        }
        this.balance -= amount;
    }

    @Override
    public String getAccountType() {
        return "business";
    }

    @Override
    public void setInterestRate(final double interestRate) {
        // Not applicable for business accounts
    }

    @Override
    public double getInterestRate() {
        return 0;
    }

    @Override
    public String getIBAN() {
        return this.iban;
    }

    /**
     * Updates the cashback status based on the number of transactions.
     *
     * @param transactions the number of transactions
     */
    public void updateCashbackStatus(final int transactions) {
        this.hasReceivedCashbackFromCommerciantsWithNrOfTransactionsStrategy.
                put(transactions, true);
    }

    /**
     * Retrieves the number of transactions per commerciant.
     *
     * @return a map of commerciant names to the number of transactions
     */
    public Map<String, Integer> getNrOfTransactionsPerCommerciants() {
        return this.nrOfTransactionsPerCommerciants;
    }

    private double convertCurrency(final String from, final String to,
                                   final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo()).add(new ExchangeRate(
                    rate.getCurrencyTo(), rate.getCurrencyFrom(), 1 / rate.getRate()));
        }

        Queue<Pair<String, Double>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(new Pair<>(from, 1.0));
        visited.add(from);

        while (!queue.isEmpty()) {
            Pair<String, Double> current = queue.poll();
            String currentCurrency = current.getKey();
            double currentRate = current.getValue();

            if (currentCurrency.equals(to)) {
                return amount * currentRate;
            }

            if (graph.containsKey(currentCurrency)) {
                for (ExchangeRate rate : graph.get(currentCurrency)) {
                    if (!visited.contains(rate.getCurrencyTo())) {
                        visited.add(rate.getCurrencyTo());
                        queue.add(new Pair<>(rate.getCurrencyTo(), currentRate * rate.getRate()));
                    }
                }
            }
        }
        return -1;
    }
}
