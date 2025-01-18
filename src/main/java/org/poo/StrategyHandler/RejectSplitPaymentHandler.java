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
import java.util.Iterator;

/**
 * Handles rejecting split payments.
 */
public final class RejectSplitPaymentHandler implements CommandHandler {

    /**
     * Executes the "reject split payment" command.
     *
     * @param command The command input containing transaction details.
     * @param users The list of users in the bank.
     * @param exchangeRates The list of exchange rates for currency conversion.
     * @param out The JSON array to store command results.
     * @param commerciantsList The list of merchants.
     * @param pendingSplitPayments The list of pending split payments.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String email = command.getEmail();
        Integer timestamp = command.getTimestamp();
        boolean userExists = false;
        boolean paymentFound = false;

        // Check if the user exists in the system
        for (User user : users) {
            if (user.getEmail().equals(email)) {
                userExists = true;
                break;
            }
        }

        if (!userExists) {
            // Add error to output if user is not found
            ObjectNode notFoundMessage = new ObjectMapper().createObjectNode();
            notFoundMessage.put("command", "rejectSplitPayment");
            ObjectNode output = new ObjectMapper().createObjectNode();
            output.put("description", "User not found");
            output.put("timestamp", timestamp);
            notFoundMessage.set("output", output);
            notFoundMessage.put("timestamp", timestamp);
            out.add(notFoundMessage);
            return;
        }

        // Iterate through the list of pending split payments
        Iterator<PendingSplitPayment> iterator = pendingSplitPayments.iterator();

        while (iterator.hasNext()) {
            PendingSplitPayment pendingSplitPayment = iterator.next();

            if (pendingSplitPayment.getUserResponses().containsKey(email)) {
                paymentFound = true;

                // Set the response of the rejecting user to false
                pendingSplitPayment.getUserResponses().put(email, false);

                // Generate an error transaction for all users involved
                ObjectNode errorTransaction = new ObjectMapper().createObjectNode();
                if (command.getSplitPaymentType().equals("custom")) {
                    ArrayList<Double> amountsForUsers = pendingSplitPayment.getAmountsForUsers();
                    errorTransaction.put("amountForUsers",
                            new ObjectMapper().valueToTree(amountsForUsers));
                }
                errorTransaction.put("timestamp", pendingSplitPayment.getTimestamp());
                errorTransaction.put("description", "Split payment of "
                        + String.format("%.2f", pendingSplitPayment.getAmount()) + " "
                        + pendingSplitPayment.getCurrency());
                errorTransaction.put("splitPaymentType", pendingSplitPayment.getType());
                errorTransaction.put("currency", pendingSplitPayment.getCurrency());
                errorTransaction.put("error", "One user rejected the payment.");
                errorTransaction.set("involvedAccounts", new ObjectMapper().
                        valueToTree(pendingSplitPayment.getAccounts()));

                // Notify all users involved in the payment
                for (String accountIBAN : pendingSplitPayment.getAccounts()) {
                    for (User user : users) {
                        for (Account account : user.getAccounts()) {
                            if (account.getIBAN().equals(accountIBAN)) {
                                user.getTransactions().add(errorTransaction);
                            }
                        }
                    }
                }
                // Remove the rejected split payment from the list
                iterator.remove();
                return;
            }
        }
    }
}
