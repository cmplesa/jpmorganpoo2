package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles the deletion of a user's account.
 */
public final class DeleteAccountHandler implements CommandHandler {

    /**
     * Executes the delete account command.
     *
     * @param command        The command input containing the details of the account deletion.
     * @param users          The list of users.
     * @param exchangeRates  The list of exchange rates.
     * @param out            The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode deleteAccount = objectMapper.createObjectNode();
        deleteAccount.put("command", command.getCommand());
        deleteAccount.put("timestamp", command.getTimestamp());

        String accountIBANDelete = command.getAccount();
        String emailDelete = command.getEmail();

        User userDelete = null;

        for (User u : users) {
            if (u.getEmail().equals(emailDelete)) {
                userDelete = u;
                break;
            }
        }

        if (userDelete == null) {
            System.out.println("User not found");
            ObjectNode output = objectMapper.createObjectNode();
            output.put("error", "User not found");
            deleteAccount.set("output", output);
            out.add(deleteAccount);
            return;
        }

        boolean foundAccount = false;
        boolean accountFound = false;
        for (Account accountDelete : userDelete.getAccounts()) {
            if (accountDelete.getIBAN().equals(accountIBANDelete)) {
                accountFound = true;

                if (accountDelete.getBalance() != 0) {
                    ObjectNode errorResponse = objectMapper.createObjectNode();
                    errorResponse.put("command", command.getCommand());
                    ObjectNode output = objectMapper.createObjectNode();
                    output.put("error",
                            "Account couldn't be deleted - "
                                    + "see org.poo.transactions for details");
                    output.put("timestamp", command.getTimestamp());
                    errorResponse.set("output", output);
                    errorResponse.put("timestamp", command.getTimestamp());
                    out.add(errorResponse);

                    ObjectNode transDelete = objectMapper.createObjectNode();
                    transDelete.put("timestamp", command.getTimestamp());
                    transDelete.put("description",
                            "Account couldn't be deleted - "
                                    + "there are funds remaining");

                    userDelete.getTransactions().add(transDelete);
                    foundAccount = true;
                    break;
                }

                accountDelete.getCards().clear();

                userDelete.getAccounts().remove(accountDelete);

                ObjectNode output = objectMapper.createObjectNode();
                output.put("success", "Account deleted");
                output.put("timestamp", command.getTimestamp());
                deleteAccount.set("output", output);

                break;
            }
        }

        if (foundAccount) {
            return;
        }

        if (!accountFound) {
            System.out.println("Account not found");
            ObjectNode output = objectMapper.createObjectNode();
            output.put("error", "Account not found");
            deleteAccount.set("output", output);
            out.add(deleteAccount);
            return;
        }

        out.add(deleteAccount);
    }
}
