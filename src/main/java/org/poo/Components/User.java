package org.poo.Components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Data
public class User {
    private String firstName;
    private String lastName;
    private String email;
    private ArrayList<Account> accounts;
    private ArrayList<ObjectNode> transactions;
    private ArrayList<ObjectNode> onlinePayments;
    private String birthDate;
    private String occupation;
    private String type;
    private int nrOfSilverTransactionsBiggerThan500;
    private int added;

    public User() {
    }

    public User(final String firstName, final String lastName, final String email,
                final String birthDate, final String occupation) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.accounts = new ArrayList<>();
        this.transactions = new ArrayList<>();
        this.onlinePayments = new ArrayList<>();
        this.birthDate = birthDate;
        this.occupation = occupation;
        this.nrOfSilverTransactionsBiggerThan500 = 0;
        this.added = 0;

        if (occupation.equals("student")) {
            this.type = "student";
        } else {
            this.type = "standard";
        }
    }

    /**
     * Upgrades the user's plan.
     *
     * @param command the command input
     * @param newPlanType the new plan type
     * @param fee the fee for the upgrade
     * @param accountIban the account IBAN
     * @param exchangeRates the exchange rates
     * @param timestamp the timestamp of the upgrade
     * @param out the output array node
     * @return the result of the upgrade
     */
    public String upgradePlan(final CommandInput command, final String newPlanType,
                              final double fee, final String accountIban,
                              final ArrayList<ExchangeRate> exchangeRates,
                              final int timestamp, final ArrayNode out) {

        System.out.println("entered");

        if (this.type.equals(newPlanType)) {
            ObjectNode transaction = new ObjectMapper().createObjectNode();
            transaction.put("description", "The user already has the " + newPlanType + " plan.");
            transaction.put("timestamp", command.getTimestamp());
            this.getTransactions().add(transaction);
            return "The user already has the " + newPlanType + " plan.";
        }

        System.out.println("FIRST ERROR");

        if ((this.type.equals("silver") && (newPlanType.equals("student")
                || newPlanType.equals("standard")))
                || (this.type.equals("gold") && (!newPlanType.equals("gold")))) {
            return "You cannot downgrade your plan.";
        }

        System.out.println("SECOND ERROR");

        Account account = null;
        for (Account acc : this.accounts) {
            if (acc.getIBAN().equals(accountIban)) {
                account = acc;
                break;
            }
        }

        if (account == null) {
            return "Account not found";
        }

        System.out.println("THIRD ERROR");

        double balance = account.getBalance();
        double balanceInRON = 0;

        if (!account.getCurrency().equals("RON")) {
            balanceInRON = convertCurrency(account.getCurrency(), "RON", balance, exchangeRates);
            if (balanceInRON == -1) {
                return "Exchange rate path not found";
            }
        } else {
            balanceInRON = balance;
        }

        if (balanceInRON < fee) {
            ObjectNode errorNode = new ObjectMapper().createObjectNode();
            errorNode.put("timestamp", command.getTimestamp());
            errorNode.put("description", "Insufficient funds");
            this.getTransactions().add(errorNode);
            return "Insufficient funds";
        }

        System.out.println("FOURTH ERROR");

        double feeInAccountCurrency = fee;
        if (!account.getCurrency().equals("RON")) {
            feeInAccountCurrency = convertCurrency("RON",
                    account.getCurrency(), fee, exchangeRates);
            if (feeInAccountCurrency == -1) {
                return "Exchange rate path not found";
            }
        }

        account.setBalance(account.getBalance() - feeInAccountCurrency);
        this.setType(newPlanType);
        System.out.println("new: " + this.getType());
        System.out.println("TIMEYSTAP: " + timestamp);


        ObjectMapper objectMapper = new ObjectMapper();

        // Add transaction to the user's list of transactions
        ObjectNode transactionNode = objectMapper.createObjectNode();
        transactionNode.put("timestamp", timestamp);
        transactionNode.put("description", "Upgrade plan");
        transactionNode.put("accountIBAN", accountIban);
        transactionNode.put("newPlanType", newPlanType);
        this.transactions.add(transactionNode);

        return "Upgrade plan";
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
