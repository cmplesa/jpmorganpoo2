package org.poo.StrategyHandler;

import org.poo.Components.User;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Commerciant;
import org.poo.Components.PendingSplitPayment;
import org.poo.fileio.CommandInput;
import org.poo.account.Account;
import org.poo.Components.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Queue;
import java.util.List;
import java.util.LinkedList;

public final class AcceptSplitHandler implements CommandHandler {
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String email = command.getEmail();
        String type = command.getSplitPaymentType();

        if (!userExists(users, email)) {
            addUserNotFoundError(out, command);
            return;
        }

        processPendingSplitPayments(command, users, exchangeRates, out,
                pendingSplitPayments, email, type);
    }

    private boolean userExists(final ArrayList<User> users, final String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return true;
            }
        }
        return false;
    }

    private void addUserNotFoundError(final ArrayNode out, final CommandInput command) {
        ObjectNode notFoundMessage = new ObjectMapper().createObjectNode();
        notFoundMessage.put("command", "acceptSplitPayment");
        ObjectNode output = new ObjectMapper().createObjectNode();
        output.put("description", "User not found");
        output.put("timestamp", command.getTimestamp());
        notFoundMessage.set("output", output);
        notFoundMessage.put("timestamp", command.getTimestamp());
        out.add(notFoundMessage);
    }

    private void processPendingSplitPayments(final CommandInput command,
                                             final ArrayList<User> users,
                                             final ArrayList<ExchangeRate> exchangeRates,
                                             final ArrayNode out,
                                             final ArrayList<PendingSplitPayment>
                                                     pendingSplitPayments,
                                             final String email, final String type) {
        Iterator<PendingSplitPayment> iterator = pendingSplitPayments.iterator();

        while (iterator.hasNext()) {
            PendingSplitPayment pendingSplitPayment = iterator.next();

            updateUserResponses(pendingSplitPayment, email);

            if (allResponsesAccepted(pendingSplitPayment)) {
                processAcceptedSplitPayment(command, users,
                        exchangeRates, out, pendingSplitPayments, iterator,
                        pendingSplitPayment, type);
                return;
            }
        }
    }

    private void updateUserResponses(final PendingSplitPayment pendingSplitPayment,
                                     final String email) {
        for (Map.Entry<String, Boolean> entry
                : pendingSplitPayment.getUserResponses().entrySet()) {
            if (email.equals(entry.getKey())) {
                entry.setValue(true);
            }
        }
    }

    private boolean allResponsesAccepted(final PendingSplitPayment pendingSplitPayment) {
        return pendingSplitPayment.getUserResponses().values()
                .stream().allMatch(Boolean::booleanValue);
    }

    private void processAcceptedSplitPayment(final CommandInput command,
                                             final ArrayList<User> users,
                                             final ArrayList<ExchangeRate> exchangeRates,
                                             final ArrayNode out,
                                             final ArrayList<PendingSplitPayment>
                                                     pendingSplitPayments,
                                             final Iterator<PendingSplitPayment> iterator,
                                             final PendingSplitPayment pendingSplitPayment,
                                             final String type) {
        ArrayList<String> accounts = pendingSplitPayment.getAccounts();
        String currency = pendingSplitPayment.getCurrency();
        double totalAmount = pendingSplitPayment.getAmount();
        Integer timestamp = pendingSplitPayment.getTimestamp();

        Set<User> uniqueUsers = findUsersByAccounts(users, accounts);

        if (type.equals("equal")) {
            processEqualSplitPayment(command, users, exchangeRates, out,
                    pendingSplitPayments, iterator, pendingSplitPayment,
                    accounts, currency, totalAmount, timestamp, uniqueUsers);
        } else if (type.equals("custom")) {
            processCustomSplitPayment(command, users, exchangeRates, out,
                    pendingSplitPayments, iterator, pendingSplitPayment,
                    accounts, currency, totalAmount, timestamp, uniqueUsers);
        }
    }

    private void processEqualSplitPayment(final CommandInput command, final ArrayList<User> users,
                                          final ArrayList<ExchangeRate> exchangeRates,
                                          final ArrayNode out, final ArrayList<PendingSplitPayment>
                                                  pendingSplitPayments,
                                          final Iterator<PendingSplitPayment> iterator,
                                          final PendingSplitPayment pendingSplitPayment, final
                                          ArrayList<String> accounts, final String currency,
                                          final double totalAmount, final Integer timestamp,
                                          final Set<User> uniqueUsers) {
        double amountPerUser = totalAmount / accounts.size();

        boolean splitError = false;
        Account accountError = null;

        for (String accountIBAN : accounts) {
            Account account = findAccountByIBAN(users, accountIBAN);

            double convertedBalance = account != null
                    ? convertCurrency(account.getCurrency(), currency,
                    account.getBalance(), exchangeRates)
                    : 0;

            if (account == null || convertedBalance < amountPerUser) {
                splitError = true;
                accountError = account;
                break;
            }
        }

        if (splitError) {
            ObjectNode errorTransaction = createErrorTransaction(timestamp, "equal",
                    currency, totalAmount, accounts, accountError);
            addTransactionToUsers(uniqueUsers, errorTransaction);
            pendingSplitPayments.remove(pendingSplitPayment);
            return;
        }

        for (String accountIBAN : accounts) {
            Account account = findAccountByIBAN(users, accountIBAN);

            double convertedAmount = convertCurrency(currency, account.getCurrency(),
                    amountPerUser, exchangeRates);
            if (account != null) {
                account.setBalance(account.getBalance() - convertedAmount);
            }
        }

        ObjectNode successTransaction = new ObjectMapper().createObjectNode();
        successTransaction.put("timestamp", timestamp);
        successTransaction.put("description", "Split payment of "
                + String.format("%.2f", totalAmount) + " " + currency);
        successTransaction.put("splitPaymentType", "equal");
        successTransaction.put("currency", currency);
        successTransaction.put("amount", totalAmount / accounts.size());
        successTransaction.set("involvedAccounts", new ObjectMapper().valueToTree(accounts));

        addTransactionToUsers(uniqueUsers, successTransaction);
        iterator.remove();
    }

    private void processCustomSplitPayment(final CommandInput command,
                                           final ArrayList<User> users,
                                           final ArrayList<ExchangeRate> exchangeRates,
                                           final ArrayNode out,
                                           final ArrayList<PendingSplitPayment>
                                                   pendingSplitPayments,
                                           final Iterator<PendingSplitPayment> iterator,
                                           final PendingSplitPayment pendingSplitPayment,
                                           final ArrayList<String> accounts,
                                           final String currency,
                                           final double totalAmount, final Integer timestamp,
                                           final Set<User> uniqueUsers) {
        ArrayList<Double> amountsForUsers = pendingSplitPayment.getAmountsForUsers();
        boolean splitError = false;
        Account accountError = null;

        for (int i = 0; i < accounts.size(); i++) {
            String accountIBAN = accounts.get(i);
            if (amountsForUsers == null) {
                return;
            }
            double amount = amountsForUsers.get(i);

            Account account = findAccountByIBAN(users, accountIBAN);
            double convertedBalance = account != null
                    ? convertCurrency(account.getCurrency(), currency,
                    account.getBalance(), exchangeRates)
                    : 0;

            if (account == null || convertedBalance < amount) {
                splitError = true;
                accountError = account;
                break;
            }
        }

        if (splitError) {
            ObjectNode errorTransaction = createErrorTransaction(timestamp,
                    "custom", currency, totalAmount, accounts, accountError);
            errorTransaction.remove("amount");
            errorTransaction.set("amountForUsers", new ObjectMapper().
                    valueToTree(amountsForUsers));
            addTransactionToUsers(uniqueUsers, errorTransaction);
            pendingSplitPayments.remove(pendingSplitPayment);
            return;
        }

        for (int i = 0; i < accounts.size(); i++) {
            String accountIBAN = accounts.get(i);
            double amount = amountsForUsers.get(i);

            Account account = findAccountByIBAN(users, accountIBAN);
            if (account != null) {
                double convertedAmount = convertCurrency(currency,
                        account.getCurrency(), amount, exchangeRates);
                account.setBalance(account.getBalance() - convertedAmount);
            }
        }

        ObjectNode successTransaction = new ObjectMapper().createObjectNode();
        successTransaction.put("timestamp", timestamp);
        successTransaction.put("description", "Split payment of "
                + String.format("%.2f", totalAmount) + " " + currency);
        successTransaction.put("splitPaymentType", "custom");
        successTransaction.put("currency", currency);
        successTransaction.set("amountForUsers", new ObjectMapper().valueToTree(amountsForUsers));
        successTransaction.set("involvedAccounts", new ObjectMapper().valueToTree(accounts));

        addTransactionToUsers(uniqueUsers, successTransaction);
        iterator.remove();
    }

    private Account findAccountByIBAN(final ArrayList<User> users,
                                      final String iban) {
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getIBAN().equals(iban)) {
                    return account;
                }
            }
        }
        return null;
    }

    private Set<User> findUsersByAccounts(final ArrayList<User> users,
                                          final ArrayList<String> accounts) {
        Set<User> uniqueUsers = new HashSet<>();
        for (String accountIBAN : accounts) {
            for (User u : users) {
                for (Account acc : u.getAccounts()) {
                    if (acc.getIBAN().equals(accountIBAN)) {
                        uniqueUsers.add(u);
                        break;
                    }
                }
            }
        }
        return uniqueUsers;
    }

    private void addTransactionToUsers(final Set<User> uniqueUsers,
                                       final ObjectNode transaction) {
        for (User user : uniqueUsers) {
            user.getTransactions().add(transaction);
        }
    }

    private ObjectNode createErrorTransaction(final Integer timestamp, final String type,
                                              final String currency,
                                              final double totalAmount,
                                              final ArrayList<String> accounts,
                                              final Account accountError) {
        ObjectNode errorTransaction = new ObjectMapper().createObjectNode();
        errorTransaction.put("timestamp", timestamp);
        errorTransaction.put("description", "Split payment of "
                + String.format("%.2f", totalAmount) + " " + currency);
        errorTransaction.put("splitPaymentType", type);
        errorTransaction.put("currency", currency);
        errorTransaction.put("amount", totalAmount / accounts.size());
        errorTransaction.set("involvedAccounts", new ObjectMapper().valueToTree(accounts));
        errorTransaction.put("error", "Account " + (accountError != null
                ? accountError.getIBAN() : "unknown") + " has insufficient funds"
                + " for a split payment.");
        return errorTransaction;
    }

    private double convertCurrency(final String from, final String to, final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo()).add(new ExchangeRate(rate.getCurrencyTo(),
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
