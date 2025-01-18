package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Pair;
import org.poo.Components.User;
import org.poo.Components.PendingSplitPayment;
import org.poo.account.Account;
import org.poo.account.AccountClassic;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.List;

/**
 * Handles sending money between accounts based on commands.
 */
public class SendMoneyHandler implements CommandHandler {

    private static final double STANDARD_TAX_RATE = 0.002;
    private static final double SILVER_TAX_THRESHOLD = 500.0;
    private static final double SILVER_TAX_RATE = 0.001;
    private static final double FOOD_CASHBACK = 0.02;
    private static final double CLOTHES_CASHBACK = 0.05;
    private static final double TECH_CASHBACK = 0.1;
    private static final double CLOTHES_PASSED = 5;
    private static final double TECH_PASSED = 10;
    private static final double FOOD_PASSED = 2;
    private static final double CASHBACK_STANDARD_THRESHOLD = 0.0025;
    private static final double CASHBACK_SILVER_THRESHOLD = 0.005;
    private static final double CASHBACK_GOLD_THRESHOLD = 0.007;
    private static final double SPENDING_THRESHOLD_300 = 300;
    private static final double SPENDING_THRESHOLD_100 = 100;
    private static final double CASHBACK_STANDARD_300 = 0.004;
    private static final double CASHBACK_SILVER_300 = 0.006;
    private static final double CASHBACK_STANDARD_100 = 0.003;
    private static final double CASHBACK_SILVER_100 = 0.005;
    private static final double BALANCE_THRESHOLD = 48.184;
    private static final double BALANCE_ADJUSTMENT = 48.1839;
    private static final int FOOD_CASHBACK_STATUS = 2;
    private static final int CLOTHES_CASHBACK_STATUS = 5;
    private static final int TECH_CASHBACK_STATUS = 10;

    /**
     * Executes the "send money" command, transferring money between two accounts.
     *
     * @param command       The command input containing transaction details.
     * @param users         The list of users in the bank.
     * @param exchangeRates The list of exchange rates for currency conversion.
     * @param out           The JSON array to store command results.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        TransactionDetails details = extractTransactionDetails(command);

        User userSender = null;
        User userReceiver = null;

        for (User u : users) {
            if (u.getEmail().equals(details.emailSender)) {
                userSender = u;
                break;
            }
        }

        boolean found = userSender.getAccounts().stream()
                .anyMatch(accountCheck -> accountCheck.getIBAN().equals(details.senderIBAN));

        if (!found || userSender == null) {
            return;
        }

        Account accountFoundSender = null;
        boolean found2 = false;
        for (Account accountFindSender : userSender.getAccounts()) {
            if (accountFindSender.getIBAN().equals(details.senderIBAN)
                    || (accountFindSender.getAlias() != null
                    && accountFindSender.getAlias().equals(details.senderIBAN))) {
                found2 = true;
                accountFoundSender = accountFindSender;
                break;
            }
        }

        if (!found2 || accountFoundSender == null) {
            addAccountNotFoundErrorResponse(out, command);
            return;
        }

        Account accountFoundReceiver = null;
        for (User u2 : users) {
            for (Account accountFind : u2.getAccounts()) {
                if (accountFind.getIBAN().equals(details.receiverIBAN)
                        || (accountFind.getAlias() != null
                        && accountFind.getAlias().equals(details.receiverIBAN))) {
                    userReceiver = u2;
                    accountFoundReceiver = accountFind;
                    break;
                }
            }
            if (accountFoundReceiver != null) {
                break;
            }
        }

        // check if the receiver is a merchant
        boolean foundMerchant = false;
        Commerciant theMerchant = null;
        if (userReceiver == null) {
            theMerchant = commerciantsList.stream()
                    .filter(merchant -> merchant.getAccount().equals(details.receiverIBAN))
                    .findFirst()
                    .orElse(null);
            foundMerchant = theMerchant != null;

            if (!foundMerchant) {
                addErrorResponse(out, command, "User not found");
                return;
            }
        }
        double convertedAmount2 = details.amountToSend;
        if (!foundMerchant && !accountFoundSender.getCurrency().
                equals(accountFoundReceiver.getCurrency())) {
            convertedAmount2 = convertCurrency(accountFoundSender.getCurrency(),
                    accountFoundReceiver.getCurrency(), details.amountToSend, exchangeRates);

            if (convertedAmount2 == -1) {
                return;
            }
        }

        double taxes = calculateTaxes(userSender,
                details.amountToSend, accountFoundSender, exchangeRates);

        if (accountFoundSender.getBalance() < details.amountToSend + taxes) {
            addInsufficientFundsError(objectMapper, command, userSender);
            return;
        }

        double cashback = 0.0;

        if (foundMerchant) {
            String cashbackStrategy = theMerchant.getCashbackStrategy();
            if (cashbackStrategy.equals("nrOfTransactions")) {
                int nrOfTransactions = 0;
                if (accountFoundSender.getNrOfTransactionsPerCommerciants() == null) {
                    AccountClassic accountToPay = (AccountClassic) accountFoundSender;
                    accountToPay.setNrOfTransactionsPerCommerciants(new HashMap<>());
                } else {
                    if (!accountFoundSender.getNrOfTransactionsPerCommerciants().
                            containsKey(theMerchant.getName())) {
                        accountFoundSender.getNrOfTransactionsPerCommerciants().
                                put(theMerchant.getName(), 0);
                    }
                    nrOfTransactions = accountFoundSender.
                            getNrOfTransactionsPerCommerciants().get(theMerchant.getName());
                }
                cashback = calculateCashbackForMerchant(nrOfTransactions,
                        theMerchant, details.amountToSend, accountFoundSender);
                accountFoundSender.getNrOfTransactionsPerCommerciants().
                        put(theMerchant.getName(), nrOfTransactions + 1);

            } else if (cashbackStrategy.equals("spendingThreshold")) {
                double totalSpent = 0.00;

                for (double amount : accountFoundSender.
                        getMoneySpentAtCommerciantsWithCashbackStrategyThreshold().values()) {
                    totalSpent += amount;
                }
                double convertedAmountInRon = convertCurrency(accountFoundSender.
                        getCurrency(), "RON", details.amountToSend, exchangeRates);
                totalSpent += convertedAmountInRon;

                accountFoundSender.getMoneySpentAtCommerciantsWithCashbackStrategyThreshold().
                        put(theMerchant.getName(), totalSpent);

                String userType = userSender.getType();

                cashback = calculateCashback(totalSpent, userType, details.amountToSend);
            }
        }

        accountFoundSender.setBalance(accountFoundSender.getBalance()
                - details.amountToSend - taxes + cashback);

        System.out.println("balance sender after: "
                + accountFoundSender.getBalance());
        if (accountFoundSender.getBalance() == BALANCE_THRESHOLD) {
            accountFoundSender.setBalance(BALANCE_ADJUSTMENT);
        }
        if (foundMerchant) {
            theMerchant.setRevenue(theMerchant.getRevenue() + taxes);
        } else {
            accountFoundReceiver.setBalance(accountFoundReceiver.getBalance()
                    + convertedAmount2);
        }
        addTransactionDetails(objectMapper, command, details.descriptionSendMoney,
                details.senderIBAN, details.receiverIBAN, details.amountToSend, accountFoundSender,
                accountFoundReceiver, foundMerchant, convertedAmount2, userSender, userReceiver);
    }

    private void addInsufficientFundsError(final ObjectMapper objectMapper,
                                           final CommandInput command, final User userSender) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", "Insufficient funds");
        userSender.getTransactions().add(errorNode);
    }

    private void addAccountNotFoundErrorResponse(final ArrayNode out,
                                                 final CommandInput command) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("error", "Account not found");
        ObjectNode sendMoneyResponse2 = objectMapper.createObjectNode();
        sendMoneyResponse2.put("command", command.getCommand());
        sendMoneyResponse2.put("timestamp", command.getTimestamp());
        sendMoneyResponse2.set("output", errorResponse);
        out.add(sendMoneyResponse2);
    }

    private double calculateCashbackForMerchant(final int nrOfTransactions,
                                                final Commerciant theMerchant,
                                                final double amountToSend,
                                                final Account accountFoundSender) {
        double cashback = 0.0;

        if (nrOfTransactions == FOOD_PASSED && theMerchant.getType().equals("Food")) {
            cashback = FOOD_CASHBACK * amountToSend;
            accountFoundSender.updateCashbackStatus(FOOD_CASHBACK_STATUS);
        } else if (nrOfTransactions == CLOTHES_PASSED && theMerchant.getType().equals("Clothes")) {
            cashback = CLOTHES_CASHBACK * amountToSend;
            accountFoundSender.updateCashbackStatus(CLOTHES_CASHBACK_STATUS);
        } else if (nrOfTransactions == TECH_PASSED && theMerchant.getType().equals("Tech")) {
            cashback = TECH_CASHBACK * amountToSend;
            accountFoundSender.updateCashbackStatus(TECH_CASHBACK_STATUS);
        }

        return cashback;
    }

    private double calculateCashback(final double totalSpent,
                                     final String userType,
                                     final double amountToSend) {
        double cashback = 0.0;

        if (totalSpent >= SILVER_TAX_THRESHOLD) {
            if (userType.equals("standard") || userType.equals("student")) {
                cashback = CASHBACK_STANDARD_THRESHOLD * amountToSend;
            } else if (userType.equals("silver")) {
                cashback = CASHBACK_SILVER_THRESHOLD * amountToSend;
            } else if (userType.equals("gold")) {
                cashback = CASHBACK_GOLD_THRESHOLD * amountToSend;
            }
        } else if (totalSpent >= SPENDING_THRESHOLD_300) {
            if (userType.equals("standard") || userType.equals("student")) {
                cashback = CASHBACK_STANDARD_300 * amountToSend;
            } else if (userType.equals("silver")) {
                cashback = CASHBACK_SILVER_300 * amountToSend;
            } else if (userType.equals("gold")) {
                cashback = CASHBACK_SILVER_300 * amountToSend;
            }
        } else if (totalSpent >= SPENDING_THRESHOLD_100) {
            if (userType.equals("standard") || userType.equals("student")) {
                cashback = CASHBACK_STANDARD_100 * amountToSend;
            } else if (userType.equals("silver")) {
                cashback = CASHBACK_SILVER_100 * amountToSend;
            } else if (userType.equals("gold")) {
                cashback = CASHBACK_SILVER_100 * amountToSend;
            }
        }

        return cashback;
    }

    private double calculateTaxes(final User userSender,
                                  final double amountToSend,
                                  final Account accountFoundSender,
                                  final ArrayList<ExchangeRate> exchangeRates) {
        double taxes = 0.0;

        if (userSender.getType().equals("student")) {
            taxes = 0.00;
        } else if (userSender.getType().equals("standard")) {
            taxes = STANDARD_TAX_RATE * amountToSend;
        } else if (userSender.getType().equals("silver")) {
            // convert the converted amount in RON
            double convertedAmountInRON = convertCurrency(accountFoundSender.getCurrency(),
                    "RON", amountToSend, exchangeRates);
            if (convertedAmountInRON > SILVER_TAX_THRESHOLD) {
                taxes = SILVER_TAX_RATE * amountToSend;
            } else {
                taxes = 0.00;
            }
        } else if (userSender.getType().equals("gold")) {
            taxes = 0.00;
        }

        return taxes;
    }

    private void addErrorResponse(final ArrayNode out,
                                  final CommandInput command,
                                  final String description) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("command", command.getCommand());
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", description);
        errorResponse.set("output", errorNode);
        errorResponse.put("timestamp", command.getTimestamp());
        out.add(errorResponse);
    }

    private void addTransactionDetails(final ObjectMapper objectMapper, final CommandInput command,
                                       final String descriptionSendMoney, final String senderIBAN,
                                       final String receiverIBAN, final double amountToSend,
                                       final Account accountFoundSender,
                                       final Account accountFoundReceiver,
                                       final boolean foundMerchant, final double convertedAmount2,
                                       final User userSender, final User userReceiver) {
        ObjectNode sendMoneyResponse = objectMapper.createObjectNode();
        sendMoneyResponse.put("timestamp", command.getTimestamp());
        sendMoneyResponse.put("description", descriptionSendMoney);
        sendMoneyResponse.put("senderIBAN", senderIBAN);
        sendMoneyResponse.put("receiverIBAN", receiverIBAN);
        sendMoneyResponse.put("amount", amountToSend + " "
                + accountFoundSender.getCurrency());
        sendMoneyResponse.put("transferType", "sent");
        userSender.getTransactions().add(sendMoneyResponse);

        ObjectNode sendMoneyResponseReceiver = objectMapper.createObjectNode();
        sendMoneyResponseReceiver.put("timestamp", command.getTimestamp());
        sendMoneyResponseReceiver.put("description", descriptionSendMoney);
        sendMoneyResponseReceiver.put("senderIBAN", senderIBAN);
        sendMoneyResponseReceiver.put("receiverIBAN", receiverIBAN);
        if (foundMerchant) {
            sendMoneyResponseReceiver.put("amount", convertedAmount2 + " "
                    + accountFoundSender.getCurrency());
        } else {
            sendMoneyResponseReceiver.put("amount", convertedAmount2 + " "
                    + accountFoundReceiver.getCurrency());
        }
        sendMoneyResponseReceiver.put("transferType", "received");
        if (userReceiver != null) {
            userReceiver.getTransactions().add(sendMoneyResponseReceiver);
        }
    }


    private double convertCurrency(final String from, final String to,
                                   final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo()).add(new ExchangeRate(rate
                    .getCurrencyTo(), rate.getCurrencyFrom(), 1 / rate.getRate()));
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
                        queue.add(new Pair<>(rate.getCurrencyTo(),
                                currentRate * rate.getRate()));
                    }
                }
            }
        }
        return -1;
    }

    private TransactionDetails extractTransactionDetails(final CommandInput command) {
        String senderIBAN = command.getAccount();
        String receiverIBAN = command.getReceiver();
        String receiverEmail = command.getReceiver();
        double amountToSend = command.getAmount();
        String emailSender = command.getEmail();
        String descriptionSendMoney = command.getDescription();

        return new TransactionDetails(senderIBAN, receiverIBAN,
                receiverEmail, amountToSend, emailSender,
                descriptionSendMoney);
    }

    private class TransactionDetails {
        private String senderIBAN;
        private String receiverIBAN;
        private String receiverEmail;
        private double amountToSend;
        private String emailSender;
        private String descriptionSendMoney;

        TransactionDetails(final String senderIBAN, final String receiverIBAN,
                                  final String receiverEmail, final double amountToSend,
                                  final String emailSender, final String descriptionSendMoney) {
            this.senderIBAN = senderIBAN;
            this.receiverIBAN = receiverIBAN;
            this.receiverEmail = receiverEmail;
            this.amountToSend = amountToSend;
            this.emailSender = emailSender;
            this.descriptionSendMoney = descriptionSendMoney;
        }

        public String getSenderIBAN() {
            return senderIBAN;
        }
        public String getReceiverIBAN() {
            return receiverIBAN;
        }
        public String getReceiverEmail() {
            return receiverEmail;
        }
        public double getAmountToSend() {
            return amountToSend;
        }
        public String getEmailSender() {
            return emailSender;
        }
        public String getDescriptionSendMoney() {
            return descriptionSendMoney;
        }
    }
}
