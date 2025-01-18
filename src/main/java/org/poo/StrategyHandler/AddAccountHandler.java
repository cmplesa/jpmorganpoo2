package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.account.AccountFactory;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles the "addAccount" command.
 */
public final class AddAccountHandler implements CommandHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes the add account command.
     *
     * @param command the command input
     * @param users the list of users
     * @param exchangeRates the list of exchange rates
     * @param out the output array node
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectNode addAccount = objectMapper.createObjectNode();
        String accountType = command.getAccountType();
        addAccount.put("timestamp", command.getTimestamp());
        addAccount.put("description", "New account created");

        String email = command.getEmail();
        User user = null;
        for (User u : users) {
            if (u.getEmail().equals(email)) {
                user = u;
                break;
            }
        }

        if (user == null) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", "User not found");

            ObjectNode addAccountResponse = objectMapper.createObjectNode();
            addAccountResponse.put("command", command.getCommand());
            addAccountResponse.put("timestamp", command.getTimestamp());
            addAccountResponse.set("output", errorResponse);

            out.add(addAccountResponse);
            return;
        }

        if (!accountType.equals("business")) {
            user.getTransactions().add(addAccount);
        }

        String type = command.getAccountType();
        String currency = command.getCurrency();
        double interestRate = "savings".equals(type) ? command.getInterestRate() : 0.0;

        Account account = AccountFactory.createAccount(type, currency,
                interestRate, email, exchangeRates);
        user.getAccounts().add(account);
    }
}
