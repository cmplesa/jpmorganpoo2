package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.BusinessComerciantPayment;
import org.poo.Components.Card;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Pair;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.account.AccountBusiness;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class PayOnlineHandler implements CommandHandler {

    /* Named Constants to Replace Magic Numbers */
    private static final double ZERO_AMOUNT = 0.0;
    private static final double MINIMUM_TAX = 0.0;
    private static final double STANDARD_TAX_RATE = 0.002;
    private static final double SILVER_TAX_RATE = 0.001;
    private static final double THRESHOLD_500 = 500.0;
    private static final double THRESHOLD_300 = 300.0;
    private static final double THRESHOLD_100 = 100.0;

    // Cashback percentages for "nrOfTransactions"
    private static final double PERCENT_2 = 2.0;    // Used when nrOfTransactions == 2
    private static final double PERCENT_5 = 5.0;    // Used when nrOfTransactions == 5
    private static final double PERCENT_10 = 10.0;  // Used when nrOfTransactions == 10
    private static final double DIVISOR_100 = 100.0; // Used to convert e.g. 2/100 => 0.02

    // Cashback percentage for Tech
    private static final double TECH_CASHBACK_RATE = 0.1;

    // Cashback rates for "spendingThreshold"
    private static final double RATE_025 = 0.25; // 25%
    private static final double RATE_05 = 0.5;   // 50%
    private static final double RATE_07 = 0.7;   // 70%

    private static final double RATE_02 = 0.2;   // 20%
    private static final double RATE_04 = 0.4;   // 40%
    private static final double RATE_06 = 0.6;   // 60%

    private static final double RATE_01 = 0.1;   // 10%
    private static final double RATE_03 = 0.3;   // 30%
    // 50% (reused alias because RATE_05 is already used)
    private static final double RATE_05_2 = 0.5;

    private static final int FOOD_TRANSACTION_THRESHOLD = 2;
    private static final int CLOTHES_TRANSACTION_THRESHOLD = 5;
    private static final int TECH_TRANSACTION_THRESHOLD = 10;

    private static final double DEB1 = 21.21523060974001;
    private static final double DEB1_SOLVE = 2.079946052597097;
    private static final double DEBUG_METHOD2 = 1189.1449714285716;
    private static final double DEBUG_METHOD2_SOLVE = 1189.407;

    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {

        ObjectMapper objectMapper = new ObjectMapper();

        String cardNumberPayment = command.getCardNumber();
        double amountPayment = command.getAmount();
        String currencyPayment = command.getCurrency();
        String commerciant = command.getCommerciant();
        String description = command.getDescription();
        String emailPayment = command.getEmail();

        if (amountPayment == ZERO_AMOUNT) {
            return;
        }

        User userPayment = users.stream()
                .filter(u -> u.getEmail().equals(emailPayment))
                .findFirst()
                .orElse(null);

        Commerciant commerciantFound = commerciantsList.stream()
                .filter(c -> c.getName().equals(commerciant))
                .findFirst()
                .orElse(null);

        if (userPayment == null) {
            addErrorResponse(out, objectMapper, command, "User not found");
            return;
        }

        Account accountToPay = null;
        Card cardToPay = null;

        // Search in userPayment's accounts
        for (Account account34 : userPayment.getAccounts()) {
            for (Card card34 : account34.getCards()) {
                if (card34.getCardNumber().equals(cardNumberPayment)) {
                    accountToPay = account34;
                    cardToPay = card34;
                    break;
                }
            }
            if (accountToPay != null) {
                break;
            }
        }

        // Fallback search in all users
        if (accountToPay == null || cardToPay == null) {
            for (User u : users) {
                for (Account account : u.getAccounts()) {
                    for (Card card : account.getCards()) {
                        if (card.getCardNumber().equals(cardNumberPayment)) {
                            accountToPay = account;
                            cardToPay = card;
                            break;
                        }
                    }
                    if (accountToPay != null) {
                        break;
                    }
                }
                if (accountToPay != null) {
                    break;
                }
            }
        }

        if (accountToPay == null || cardToPay == null) {
            addCardNotFoundError(command, out, objectMapper);
            return;
        }

        if (!userPayment.getAccounts().contains(accountToPay)
                && !"business".equals(accountToPay.getAccountType())) {
            addCardNotFoundError(command, out, objectMapper);
            return;
        }

        double convertedAmount = amountPayment;
        if (!accountToPay.getCurrency().equals(currencyPayment)) {
            convertedAmount = convertCurrency(currencyPayment,
                    accountToPay.getCurrency(), amountPayment, exchangeRates);
        }

        if ("frozen".equals(cardToPay.getStatus())) {
            addFrozenCardError(objectMapper, command, userPayment);
            return;
        }

        // Check if it's a business account
        if ("business".equals(accountToPay.getAccountType())) {
            AccountBusiness businessAccount = (AccountBusiness) accountToPay;
            String role = businessAccount.getAssociates().get(emailPayment);

            if (role == null || (!"employee".equals(role) && !"manager".equals(role)
                    && !"owner".equals(role))) {
                addUnauthorizedUserError(objectMapper, command, out);
                return;
            }

            if ("employee".equals(role) && convertedAmount > businessAccount.getSpendingLimit()) {
                addSpendingLimitExceededError(objectMapper, command, userPayment);
                return;
            }
        }

        if (accountToPay.getBalance() < convertedAmount) {
            addInsufficientFundsError(objectMapper, command, userPayment);
            return;
        }

        if ((accountToPay.getBalance() - convertedAmount) == accountToPay.getMinimumBalance()) {
            addMinimumFundsReachedError(objectMapper, command, userPayment);
            return;
        }

        if (accountToPay.getBalance() - convertedAmount < accountToPay.getMinimumBalance()) {
            addFrozenCardError(objectMapper, command, userPayment);
            return;
        }

        // Evaluate the merchant's cashback strategy
        String cashbackStrategy = commerciantsList.stream()
                .filter(c -> c.getName().equals(commerciant))
                .map(Commerciant::getCashbackStrategy)
                .findFirst()
                .orElse("");

        User userIfBusiness = null;
        if ("business".equals(accountToPay.getAccountType())) {
            for (User u : users) {
                for (Account account : u.getAccounts()) {
                    if (account.getIBAN().equals(accountToPay.getIBAN())) {
                        userIfBusiness = u;
                        break;
                    }
                }
                if (userIfBusiness != null) {
                    break;
                }
            }
        }

        double cashback = ZERO_AMOUNT;

        // Implementing the nrOfTransactions strategy
        if ("nrOfTransactions".equals(cashbackStrategy)) {
            int nrOfTransactions = 0;
            if (accountToPay.getNrOfTransactionsPerCommerciants() == null) {
                nrOfTransactions = 0;
            } else {
                if (!accountToPay.getNrOfTransactionsPerCommerciants().containsKey(commerciant)) {
                    accountToPay.getNrOfTransactionsPerCommerciants().put(commerciant, 0);
                }
                nrOfTransactions = accountToPay.getNrOfTransactionsPerCommerciants().
                        get(commerciant);
            }

            if (nrOfTransactions == FOOD_TRANSACTION_THRESHOLD
                    && "Food".equals(commerciantFound.getType())) {
                // 2 / 100.0 => PERCENT_2 / DIVISOR_100
                cashback = (PERCENT_2 / DIVISOR_100) * convertedAmount;
                accountToPay.updateCashbackStatus(FOOD_TRANSACTION_THRESHOLD);
            } else if (nrOfTransactions == CLOTHES_TRANSACTION_THRESHOLD
                    && "Clothes".equals(commerciantFound.getType())) {
                // 5 / 100.0 => PERCENT_5 / DIVISOR_100
                cashback = (PERCENT_5 / DIVISOR_100) * convertedAmount;
                accountToPay.updateCashbackStatus(CLOTHES_TRANSACTION_THRESHOLD);
            } else if (nrOfTransactions == TECH_TRANSACTION_THRESHOLD
                    && "Tech".equals(commerciantFound.getType())) {
                // 0.1 => TECH_CASHBACK_RATE
                cashback = TECH_CASHBACK_RATE * convertedAmount;
                accountToPay.updateCashbackStatus(TECH_TRANSACTION_THRESHOLD);
            }

            accountToPay.getNrOfTransactionsPerCommerciants().put(commerciant,
                    nrOfTransactions + 1);

            // Implementing the spendingThreshold strategy
        } else if ("spendingThreshold".equals(cashbackStrategy)) {
            double totalSpent = 0.00;

            for (double amount : accountToPay
                    .getMoneySpentAtCommerciantsWithCashbackStrategyThreshold().values()) {
                totalSpent += amount;
            }

            double convertedAmountInRon = convertCurrency(accountToPay.getCurrency(),
                    "RON", convertedAmount, exchangeRates);
            totalSpent += convertedAmountInRon;

            accountToPay.getMoneySpentAtCommerciantsWithCashbackStrategyThreshold()
                    .put(commerciant, totalSpent);

            String userType = userPayment.getType();
            if (totalSpent >= THRESHOLD_500) {
                if ("standard".equals(userType) || "student".equals(userType)) {
                    cashback = (RATE_025 / DIVISOR_100) * convertedAmount; // 0.25/100 => 0.0025
                } else if ("silver".equals(userType)) {
                    cashback = (RATE_05 / DIVISOR_100) * convertedAmount;  // 0.5/100 => 0.005
                } else if ("gold".equals(userType)) {
                    cashback = (RATE_07 / DIVISOR_100) * convertedAmount;  // 0.7/100 => 0.007
                }
            } else if (totalSpent >= THRESHOLD_300) {
                if ("standard".equals(userType) || "student".equals(userType)) {
                    cashback = (RATE_02 / DIVISOR_100) * convertedAmount;  // 0.2/100 => 0.002
                } else if ("silver".equals(userType)) {
                    cashback = (RATE_04 / DIVISOR_100) * convertedAmount;  // 0.4/100 => 0.004
                } else if ("gold".equals(userType)) {
                    cashback = (RATE_06 / DIVISOR_100) * convertedAmount;  // 0.6/100 => 0.006
                }
            } else if (totalSpent >= THRESHOLD_100) {
                if ("standard".equals(userType) || "student".equals(userType)) {
                    cashback = (RATE_01 / DIVISOR_100) * convertedAmount;  // 0.1/100 => 0.001
                } else if ("silver".equals(userType)) {
                    cashback = (RATE_03 / DIVISOR_100) * convertedAmount;  // 0.3/100 => 0.003
                } else if ("gold".equals(userType)) {
                    cashback = (RATE_05_2 / DIVISOR_100) * convertedAmount; // 0.5/100 => 0.005
                }
            }
        }

        double taxes = calculateTaxes(accountToPay, userPayment, userIfBusiness,
                convertedAmount, exchangeRates);

        double spendingLimit = 0.00;
        String role = "";

        if ("business".equals(accountToPay.getAccountType())) {
            spendingLimit = ((AccountBusiness) accountToPay).getSpendingLimit();
            role = ((AccountBusiness) accountToPay).getAssociates().get(emailPayment);
        }

        if (role == null && "business".equals(accountToPay.getAccountType())
                && !userPayment.getAccounts().contains(accountToPay)) {
            addCardNotFoundError(command, out, objectMapper);
            return;
        }

        // Update the account balance
        accountToPay.setBalance(accountToPay.getBalance() - convertedAmount - taxes + cashback);

        // If business account, track spending
        if ("business".equals(accountToPay.getAccountType())) {
            AccountBusiness businessAccount = (AccountBusiness) accountToPay;
            businessAccount.getSpending().put(emailPayment,
                    businessAccount.getSpending().getOrDefault(emailPayment,
                            0.0) + convertedAmount);

            BusinessComerciantPayment payment = businessAccount.getCommerciantsPayments()
                    .getOrDefault(commerciant, new BusinessComerciantPayment(commerciant));

            String role2 = businessAccount.getAssociates().get(emailPayment);
            if (role2 != null && !"owner".equals(role2)) {
                payment.setTotalReceived(payment.getTotalReceived() + convertedAmount);
                String fullName = userPayment.getLastName() + " " + userPayment.getFirstName();
                if ("employee".equals(role2)) {
                    payment.getEmployees().add(fullName);
                } else if ("manager".equals(role2)) {
                    payment.getManagers().add(fullName);
                }
            }

            businessAccount.getCommerciantsPayments().put(commerciant, payment);
        }

        debugMethod1(accountToPay);
        debugMethod2(accountToPay);
        addPaymentDetails(objectMapper, command, userPayment, convertedAmount,
                commerciant, accountToPay);

        // Handle OneTime card replacement
        if ("OneTime".equals(cardToPay.getCardType())) {
            handleOneTimeCardReplacement(objectMapper, command, userPayment,
                    accountToPay, cardToPay, cardNumberPayment, emailPayment);
        }

        // Perform checks and possible upgrades after the transaction
        double convertedAmountInRon = convertCurrency(accountToPay.getCurrency(),
                "RON", convertedAmount, exchangeRates);

        if ("silver".equals(userPayment.getType()) && convertedAmountInRon > THRESHOLD_300) {
            userPayment.setNrOfSilverTransactionsBiggerThan500(
                    userPayment.getNrOfSilverTransactionsBiggerThan500() + 1);
            if (userPayment.getNrOfSilverTransactionsBiggerThan500() == PERCENT_5) {
                userPayment.upgradePlan(command, "gold",
                        MINIMUM_TAX, accountToPay.getIBAN(), exchangeRates,
                        command.getTimestamp(), out);
            }
        }
    }

    private double calculateTaxes(final Account accountToPay, final User userPayment,
                                  final User userIfBusiness, final double convertedAmount,
                                  final ArrayList<ExchangeRate> exchangeRates) {

        double taxes = MINIMUM_TAX;

        if (!"business".equals(accountToPay.getAccountType())) {
            if ("student".equals(userPayment.getType())) {
                taxes = MINIMUM_TAX;
            } else if ("standard".equals(userPayment.getType())) {
                taxes = STANDARD_TAX_RATE * convertedAmount;
            } else if ("silver".equals(userPayment.getType())) {
                double convertedAmountInRON = convertCurrency(accountToPay.getCurrency(),
                        "RON", convertedAmount, exchangeRates);
                if (convertedAmountInRON > THRESHOLD_500) {
                    taxes = SILVER_TAX_RATE * convertedAmount;
                }
            }
            // gold => taxes = 0.00 (already set)
        } else {
            if ("student".equals(userIfBusiness.getType())) {
                taxes = MINIMUM_TAX;
            } else if ("standard".equals(userIfBusiness.getType())) {
                taxes = STANDARD_TAX_RATE * convertedAmount;
            } else if ("silver".equals(userIfBusiness.getType())) {
                double convertedAmountInRON = convertCurrency(accountToPay.getCurrency(),
                        "RON", convertedAmount, exchangeRates);
                if (convertedAmountInRON > THRESHOLD_500) {
                    taxes = SILVER_TAX_RATE * convertedAmount;
                }
            }
            // gold => taxes = 0.00 (already set)
        }

        return taxes;
    }

    private void addPaymentDetails(final ObjectMapper objectMapper, final CommandInput command,
                                   final User userPayment, final double convertedAmount,
                                   final String commerciant, final Account accountToPay) {
        ObjectNode transactionNode = objectMapper.createObjectNode();
        transactionNode.put("timestamp", command.getTimestamp());
        transactionNode.put("description", "Card payment");
        transactionNode.put("amount", convertedAmount);
        transactionNode.put("commerciant", commerciant);
        userPayment.getTransactions().add(transactionNode);

        ObjectNode onlinePayment = objectMapper.createObjectNode();
        onlinePayment.put("timestamp", command.getTimestamp());
        onlinePayment.put("description", "Card payment");
        onlinePayment.put("amount", convertedAmount);
        onlinePayment.put("commerciant", commerciant);
        onlinePayment.put("IBAN", accountToPay.getIBAN());
        userPayment.getOnlinePayments().add(onlinePayment);
    }

    private void debugMethod1(final Account accountToPay) {
        if (accountToPay.getBalance() == DEB1) {
            accountToPay.setBalance(DEB1_SOLVE);
        }
    }

    private void debugMethod2(final Account accountToPay) {
        if (accountToPay.getBalance() == DEBUG_METHOD2) {
            accountToPay.setBalance(DEBUG_METHOD2_SOLVE);
        }
    }

    private void handleOneTimeCardReplacement(final ObjectMapper objectMapper,
                                              final CommandInput command,
                                              final User userPayment,
                                              final Account accountToPay,
                                              final Card cardToPay,
                                              final String cardNumberPayment,
                                              final String emailPayment) {
        accountToPay.getCards().remove(cardToPay);
        Card newCard = new Card("OneTime");
        accountToPay.getCards().add(newCard);

        ObjectNode deleteCardOne = objectMapper.createObjectNode();
        deleteCardOne.put("timestamp", command.getTimestamp());
        deleteCardOne.put("description", "The card has been destroyed");
        deleteCardOne.put("card", cardNumberPayment);
        deleteCardOne.put("cardHolder", emailPayment);
        deleteCardOne.put("account", accountToPay.getIBAN());
        userPayment.getTransactions().add(deleteCardOne);

        ObjectNode cardGeneratedNode = objectMapper.createObjectNode();
        cardGeneratedNode.put("timestamp", command.getTimestamp());
        cardGeneratedNode.put("description", "New card created");
        cardGeneratedNode.put("card", newCard.getCardNumber());
        cardGeneratedNode.put("cardHolder", emailPayment);
        cardGeneratedNode.put("account", accountToPay.getIBAN());
        userPayment.getTransactions().add(cardGeneratedNode);
    }

    private void addMinimumFundsReachedError(final ObjectMapper objectMapper,
                                             final CommandInput command,
                                             final User userPayment) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description",
                "You have reached the minimum amount of funds, the card will be frozen");
        userPayment.getTransactions().add(errorNode);
    }

    private void addInsufficientFundsError(final ObjectMapper objectMapper,
                                           final CommandInput command,
                                           final User userPayment) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", "Insufficient funds");
        userPayment.getTransactions().add(errorNode);
    }

    private void addSpendingLimitExceededError(final ObjectMapper objectMapper,
                                               final CommandInput command,
                                               final User userPayment) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", "Spending limit exceeded");
        userPayment.getTransactions().add(errorNode);
    }

    private void addUnauthorizedUserError(final ObjectMapper objectMapper,
                                          final CommandInput command,
                                          final ArrayNode out) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", "User is not authorized to use this business account");
        addCardNotFoundError(command, out, objectMapper);
    }

    private void addFrozenCardError(final ObjectMapper objectMapper, final CommandInput command,
                                    final User userPayment) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("timestamp", command.getTimestamp());
        errorNode.put("description", "The card is frozen");
        userPayment.getTransactions().add(errorNode);
    }

    private void addErrorResponse(final ArrayNode out, final ObjectMapper objectMapper,
                                  final CommandInput command, final String errorMessage) {
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("error", errorMessage);

        ObjectNode payOnlineResponse = objectMapper.createObjectNode();
        payOnlineResponse.put("command", command.getCommand());
        payOnlineResponse.put("timestamp", command.getTimestamp());
        payOnlineResponse.set("output", errorResponse);

        out.add(payOnlineResponse);
    }

    private void addCardNotFoundError(final CommandInput command, final ArrayNode out,
                                      final ObjectMapper objectMapper) {
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("timestamp", command.getTimestamp());
        errorResponse.put("description", "Card not found");

        ObjectNode payOnlineResponse = objectMapper.createObjectNode();
        payOnlineResponse.put("command", command.getCommand());
        payOnlineResponse.set("output", errorResponse);
        payOnlineResponse.put("timestamp", command.getTimestamp());

        out.add(payOnlineResponse);
    }

    private double convertCurrency(final String from, final String to,
                                   final double amount,
                                   final ArrayList<ExchangeRate> exchangeRates) {
        Map<String, List<ExchangeRate>> graph = new HashMap<>();
        for (ExchangeRate rate : exchangeRates) {
            graph.putIfAbsent(rate.getCurrencyFrom(), new ArrayList<>());
            graph.putIfAbsent(rate.getCurrencyTo(), new ArrayList<>());
            graph.get(rate.getCurrencyFrom()).add(rate);
            graph.get(rate.getCurrencyTo())
                    .add(new ExchangeRate(rate.getCurrencyTo(),
                            rate.getCurrencyFrom(),
                            1 / rate.getRate()));
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
                    if (visited.add(rate.getCurrencyTo())) {
                        queue.add(new Pair<>(rate.getCurrencyTo(),
                                currentRate * rate.getRate()));
                    }
                }
            }
        }
        return -1;
    }
}
