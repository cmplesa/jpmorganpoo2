package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles changing the interest rate for accounts based on commands.
 */
public final class ChangeInterestRateHandler implements CommandHandler {

    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        String accountInterest = command.getAccount();
        Double interestRateChange = command.getInterestRate();

        User userInterest = null;
        Account accountInterestRate = null;

        for (User u : users) {
            for (Account accountToFindChange : u.getAccounts()) {
                if (accountToFindChange.getIBAN().equals(accountInterest)
                        || (accountToFindChange.getAlias() != null
                        && accountToFindChange.getAlias()
                        .equals(accountInterest))) {
                    userInterest = u;
                    accountInterestRate = accountToFindChange;
                    break;
                }
            }
            if (userInterest != null) {
                break;
            }
        }

        if (accountInterestRate == null) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("command", command.getCommand());

            ObjectNode errorNotFound = objectMapper.createObjectNode();
            errorNotFound.put("timestamp", command.getTimestamp());
            errorNotFound.put("description", "Account not found");

            error.set("output", errorNotFound);
            error.put("timestamp", command.getTimestamp());
            out.add(error);
            return;
        }

        if (accountInterestRate.getAccountType().equals("classic")) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("command", command.getCommand());

            ObjectNode errorNotSavings = objectMapper.createObjectNode();
            errorNotSavings.put("timestamp", command.getTimestamp());
            errorNotSavings.put("description", "This is not a savings account");

            error.set("output", errorNotSavings);
            error.put("timestamp", command.getTimestamp());
            out.add(error);
            return;
        }

        accountInterestRate.setInterestRate(interestRateChange);

        ObjectNode transactionInterest = objectMapper.createObjectNode();
        transactionInterest.put("timestamp", command.getTimestamp());
        transactionInterest.put("description",
                "Interest rate of the account changed to " + interestRateChange);
        userInterest.getTransactions().add(transactionInterest);

        ObjectNode successResponse = objectMapper.createObjectNode();
        successResponse.put("command", command.getCommand());
        successResponse.put("timestamp", command.getTimestamp());
        successResponse.put("description", "Interest rate successfully changed");
    }
}
