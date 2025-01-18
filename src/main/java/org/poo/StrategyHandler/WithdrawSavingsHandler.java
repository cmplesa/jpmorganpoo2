package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Pair;
import org.poo.Components.User;
import org.poo.Components.PendingSplitPayment;
import org.poo.account.Account;
import org.poo.account.AccountClassic;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.poo.fileio.CommandInput;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Handles the withdrawal of savings.
 */
public final class WithdrawSavingsHandler implements CommandHandler {

    private static final int MINIMUM_AGE = 21;

    /**
     * Executes the withdraw savings command.
     *
     * @param command The command input containing details.
     * @param users The list of users in the bank.
     * @param exchangeRates The list of exchange rates for currency conversion.
     * @param out The JSON array to store command results.
     * @param commerciantsList The list of commerciants.
     * @param pendingSplitPayments The list of pending split payments.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String savingsIBAN = command.getAccount();
        double amount = command.getAmount();
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();
        ObjectMapper mapper = new ObjectMapper();

        boolean hasClassicAccountToDeposit = false;
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getAccountType().equals("classic")
                        && account.getCurrency().equals(currency)) {
                    hasClassicAccountToDeposit = true;
                    break;
                }
            }
        }


        if (!hasClassicAccountToDeposit) {
            addTransactionToUser(users, savingsIBAN,
                    "You do not have a classic account.", timestamp, mapper);
            return;
        }

        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getIBAN().equals(savingsIBAN)) {
                    // search if the user has any classic account

                    if (account.getAccountType().equals("classic")
                            || account.getAccountType().equals("business")) {
                        addTransaction(user,
                                "Account is not of type savings", savingsIBAN, timestamp, mapper);
                        return;
                    }

                    if (!isUserOfAge(user.getBirthDate())) {
                        addTransaction(user,
                                "You don't have the minimum age required.",
                                savingsIBAN, timestamp, mapper);
                        return;
                    }

                    if (account.getBalance() < amount) {
                        addTransaction(user,
                                "Insufficient funds", savingsIBAN, timestamp, mapper);
                        return;
                    }

                    AccountClassic destinationAccount = findDestinationAccount(user, currency);
                    if (destinationAccount == null) {
                        addTransaction(user,
                                "You do not have a classic account.",
                                savingsIBAN, timestamp, mapper);
                        return;
                    }

                    double convertedAmount =
                            convertCurrency(account.getCurrency(), currency, amount, exchangeRates);
                    if (convertedAmount == -1) {
                        addTransaction(user,
                                "Exchange rate path not found", savingsIBAN, timestamp, mapper);
                        return;
                    }

                    account.withdraw(amount);
                    destinationAccount.deposit(convertedAmount);

                    addSuccessfulTransaction(user,
                            savingsIBAN, destinationAccount.getIBAN(), amount, timestamp, mapper);
                    addSuccessfulTransaction(user,
                            savingsIBAN, destinationAccount.getIBAN(), amount, timestamp, mapper);
                    return;
                }
            }
        }

        addTransactionToUser(users, savingsIBAN, "Account not found", timestamp, mapper);
    }

    private boolean isUserOfAge(final String birthDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate birthDateLocal = LocalDate.parse(birthDate, formatter);
        LocalDate currentDate = LocalDate.now();
        Period age = Period.between(birthDateLocal, currentDate);
        return age.getYears() >= MINIMUM_AGE;
    }

    private AccountClassic findDestinationAccount(final User user, final String currency) {
        for (Account account : user.getAccounts()) {
            if (account instanceof AccountClassic
                    && account.getCurrency().equalsIgnoreCase(currency)) {
                return (AccountClassic) account;
            }
        }
        return null;
    }

    private double convertCurrency(final String from, final String to, final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }

        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo()).add(
                    new ExchangeRate(rate.getCurrencyTo(),
                            rate.getCurrencyFrom(), 1 / rate.getRate()));
        }

        Queue<Pair<String, Double>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(new Pair<>(from, 1.0));
        visited.add(from);

        while (!queue.isEmpty()) {
            Pair<String, Double> current = queue.poll();
            String currentCurrency = current.getKey();
            double currentRate = current.getValue();

            if (currentCurrency.equalsIgnoreCase(to)) {
                return amount * currentRate;
            }

            if (graph.containsKey(currentCurrency)) {
                for (ExchangeRate rate : graph.get(currentCurrency)) {
                    if (!visited.contains(rate.getCurrencyTo())) {
                        visited.add(rate.getCurrencyTo());
                        queue.add(new Pair<>(rate.getCurrencyTo(),
                                currentRate * rate.getRate()));
                    }
                }
            }
        }
        return -1;
    }

    private void addTransaction(final User user, final String message, final String accountIBAN,
                                final int timestamp, final ObjectMapper mapper) {
        ObjectNode transaction = mapper.createObjectNode();
        transaction.put("description", message);
        transaction.put("timestamp", timestamp);
        user.getTransactions().add(transaction);
    }

    private void addTransactionToUser(final ArrayList<User> users,
                                      final String accountIBAN,
                                      final String message, final int timestamp,
                                      final ObjectMapper mapper) {
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getIBAN().equals(accountIBAN)) {
                    addTransaction(user, message, accountIBAN, timestamp, mapper);
                    return;
                }
            }
        }
    }

    private void addSuccessfulTransaction(final User user, final String savingsIBAN,
                                          final String classicIBAN, final double amount,
                                          final int timestamp, final ObjectMapper mapper) {
        ObjectNode transaction = mapper.createObjectNode();
        transaction.put("description", "Savings withdrawal");
        transaction.put("timestamp", timestamp);
        transaction.put("savingsAccountIBAN", savingsIBAN);
        transaction.put("classicAccountIBAN", classicIBAN);
        transaction.put("amount", amount);
        user.getTransactions().add(transaction);
    }
}
