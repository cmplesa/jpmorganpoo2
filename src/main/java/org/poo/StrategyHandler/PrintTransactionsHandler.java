package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.Components.PendingSplitPayment;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles the command to print a user's transactions.
 * Filters transactions for a user by removing sensitive
 * information (e.g., IBAN) from "Card payment" transactions.
 */
public final class PrintTransactionsHandler implements CommandHandler {

    /**
     * Executes the "printTransactions" command.
     *
     * @param command       The command input containing the user's email.
     * @param users         The list of users in the system.
     * @param exchangeRates The list of exchange rates (not used in this handler).
     * @param out           The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        final String emailPrint = command.getEmail();
        ObjectNode printTransactions = objectMapper.createObjectNode();
        printTransactions.put("command", command.getCommand());
        User userPrint = null;

        for (User u : users) {
            if (u.getEmail().equals(emailPrint)) {
                userPrint = u;
                break;
            }
        }

        if (userPrint == null) {
            ObjectNode errResp = objectMapper.createObjectNode();
            errResp.put("error", "User not found");
            ObjectNode resp = objectMapper.createObjectNode();
            resp.put("command", command.getCommand());
            resp.put("timestamp", command.getTimestamp());
            resp.set("output", errResp);
            out.add(resp);
            return;
        }

        ArrayNode transactionsArray = objectMapper.createArrayNode();
        ArrayList<ObjectNode> transactionsList = new ArrayList<>();
        for (ObjectNode transaction : userPrint.getTransactions()) {
            if ("Card payment".equals(transaction.get("description").asText())) {
                transaction.remove("IBAN"); // Remove sensitive IBAN for card payments
            }
            transactionsList.add(transaction);
        }

        // Sort transactions by timestamp
        transactionsList.sort((t1, t2)
                -> Integer.compare(t1.get("timestamp").asInt(), t2.get("timestamp").asInt()));

        transactionsArray.addAll(transactionsList);
        printTransactions.set("output", transactionsArray);
        printTransactions.put("timestamp", command.getTimestamp());
        out.add(printTransactions);
    }
}
