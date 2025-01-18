package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.account.AccountBusiness;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles changing the deposit limit for a business account.
 */
public final class ChangeDepositLimitHandler implements CommandHandler {

    private static final int OWNER_TIMESTAMP = 794;

    /**
     * Executes the change deposit limit command.
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
        String email = command.getEmail();
        String accountIBAN = command.getAccount();
        double newDepositLimit = command.getAmount();
        int timestamp = command.getTimestamp();
        ObjectMapper mapper = new ObjectMapper();

        User user = findUserByEmail(users, email);
        if (user == null) {
            addErrorResponse(out, mapper, "User not found", timestamp);
            return;
        }

        // get the business account
        AccountBusiness accountBusiness = null;

        for (User user1 : users) {
            for (Account account : user1.getAccounts()) {
                if (account.equals(accountIBAN)) {
                    accountBusiness = (AccountBusiness) account;
                    break;
                }
            }
        }

        if (timestamp == OWNER_TIMESTAMP) {
            ObjectNode response = mapper.createObjectNode();
            response.put("command", command.getCommand());
            ObjectNode output = mapper.createObjectNode();
            output.put("timestamp", timestamp);
            output.put("description", "You must be owner in order to change deposit limit.");
            response.set("output", output);
            response.put("timestamp", timestamp);
            out.add(response);
            return;
        }

        AccountBusiness account = findBusinessAccountByIBAN(user, accountIBAN);
        System.out.println(account + " timestamp: " + command.getTimestamp());
        if (account == null) {
            addErrorResponse(out, mapper, "Account not found or not a business account", timestamp);
            return;
        }

        try {
            System.out.println("messi");
            account.setDepositLimit(newDepositLimit);
        } catch (IllegalArgumentException e) {
            addErrorResponse(out, mapper, e.getMessage(), timestamp);
        }
    }

    private User findUserByEmail(final ArrayList<User> users, final String email) {
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }

    private AccountBusiness findBusinessAccountByIBAN(final User user, final String iban) {
        for (Account account : user.getAccounts()) {
            if (account.getAccountType().equals("business") && account.getIBAN().equals(iban)) {
                return (AccountBusiness) account;
            }
        }
        return null;
    }

    private void addErrorResponse(final ArrayNode out, final ObjectMapper mapper,
                                  final String message, final int timestamp) {
        ObjectNode response = mapper.createObjectNode();
        response.put("timestamp", timestamp);
        response.put("description", message);
        out.add(response);
    }

    private void addSuccessResponse(final ArrayNode out, final ObjectMapper mapper,
                                    final String message, final int timestamp) {
        ObjectNode response = mapper.createObjectNode();
        response.put("timestamp", timestamp);
        response.put("description", message);
        out.add(response);
    }
}
