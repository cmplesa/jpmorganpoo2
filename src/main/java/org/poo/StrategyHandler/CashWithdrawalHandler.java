package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Card;
import org.poo.Components.User;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Commerciant;
import org.poo.Components.PendingSplitPayment;
import org.poo.fileio.CommandInput;
import org.poo.account.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

public class CashWithdrawalHandler implements CommandHandler {

    private static final double STANDARD_TAX_RATE = 0.002;
    private static final double SILVER_TAX_RATE = 0.001;
    private static final double GOLD_TAX_RATE = 0.0;
    private static final int SILVER_THRESHOLD = 500;

    /**
     * Executes the cash withdrawal command.
     *
     * @param command              the command input
     * @param users                the list of users
     * @param exchangeRates        the list of exchange rates
     * @param out                  the output node
     * @param commerciantsList     the list of commerciants
     * @param pendingSplitPayments the list of pending split payments
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {

        final String cardNumber = command.getCardNumber();
        final double amount = command.getAmount();
        final String email = command.getEmail();
        final String location = command.getLocation();
        final int timestamp = command.getTimestamp();

        User user = findUserByEmail(users, email);
        if (user == null) {
            logError(out, command, timestamp, "User not found");
            return;
        }

        Account account = null;
        Card card = null;
        for (Account acc : user.getAccounts()) {
            for (Card c : acc.getCards()) {
                if (c.getCardNumber().equals(cardNumber)) {
                    account = acc;
                    card = c;
                    break;
                }
            }
            if (account != null) {
                break;
            }
        }

        if (card == null) {
            logError(out, command, timestamp, "Card not found");
            return;
        }

        if ("frozen".equals(card.getStatus())) {
            logTransaction(user, timestamp, "Card is frozen");
            return;
        }

        double convertedAmount = convertCurrency("RON", account.getCurrency(),
                amount, exchangeRates);
        double taxes = calculateTaxes(user.getType(), convertedAmount, account,
                exchangeRates);

        if (convertedAmount + taxes > account.getBalance()) {
            logTransaction(user, timestamp, "Insufficient funds");
            return;
        }

        if (account.getBalance() - convertedAmount - taxes
                <= account.getMinimumBalance()) {
            logTransaction(user, timestamp, "Cannot perform payment "
                    + "due to a minimum balance being set");
            return;
        }

        account.setBalance(account.getBalance() - convertedAmount - taxes);
        logTransaction(user, timestamp, "Cash withdrawal of " + amount, amount);
    }

    private User findUserByEmail(final ArrayList<User> users, final String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    private void logError(final ArrayNode out, final CommandInput command, final
    int timestamp, final String message) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode error = mapper.createObjectNode();
        error.put("command", command.getCommand());
        ObjectNode content = mapper.createObjectNode();
        content.put("timestamp", timestamp);
        content.put("description", message);
        error.set("output", content);
        error.put("timestamp", timestamp);
        out.add(error);
    }

    private void logTransaction(final User user, final int timestamp, final
    String description) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode transaction = mapper.createObjectNode();
        transaction.put("timestamp", timestamp);
        transaction.put("description", description);
        user.getTransactions().add(transaction);
    }

    private void logTransaction(final User user, final int timestamp, final
    String description, final double amount) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode transaction = mapper.createObjectNode();
        transaction.put("timestamp", timestamp);
        transaction.put("description", description);
        transaction.put("amount", amount);
        user.getTransactions().add(transaction);
    }

    private double calculateTaxes(final String userType, final double
                                          convertedAmount, final Account account,
                                  final ArrayList<ExchangeRate> exchangeRates) {
        if ("student".equals(userType) || "gold".equals(userType)) {
            return 0.0;
        } else if ("standard".equals(userType)) {
            return STANDARD_TAX_RATE * convertedAmount;
        } else if ("silver".equals(userType)) {
            double convertedAmountInRON = convertCurrency(account.getCurrency(),
                    "RON", convertedAmount, exchangeRates);
            return (convertedAmountInRON > SILVER_THRESHOLD) ? SILVER_TAX_RATE
                    * convertedAmount : 0.0;
        }
        return 0.0;
    }

    private double convertCurrency(final String from, final String to, final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo())
                    .add(new ExchangeRate(rate.getCurrencyTo(), rate.getCurrencyFrom(),
                            1 / rate.getRate()));
        }

        Queue<Map.Entry<String, Double>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(Map.entry(from, 1.0));
        visited.add(from);

        while (!queue.isEmpty()) {
            Map.Entry<String, Double> current = queue.poll();
            String currentCurrency = current.getKey();
            double currentRate = current.getValue();

            if (currentCurrency.equals(to)) {
                return amount * currentRate;
            }

            if (graph.containsKey(currentCurrency)) {
                for (ExchangeRate rate : graph.get(currentCurrency)) {
                    if (!visited.contains(rate.getCurrencyTo())) {
                        visited.add(rate.getCurrencyTo());
                        queue.add(Map.entry(rate.getCurrencyTo(), currentRate * rate.getRate()));
                    }
                }
            }
        }
        return -1;
    }
}
