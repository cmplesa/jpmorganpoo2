package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.fileio.CommandInput;
import org.poo.Components.PendingSplitPayment;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles split payment commands.
 */
public final class SplitPaymentHandler implements CommandHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String type = command.getSplitPaymentType();
        List<String> accounts = command.getAccounts();
        // do the list in ArrayList
        ArrayList<String> accountsList = new ArrayList<>(accounts);
        double amount = command.getAmount();
        List<Double> amountsForUsers = command.getAmountForUsers();
        // do the list in ArrayList
        ArrayList<Double> amountsForUsersList = null;
        if (amountsForUsers == null) {
            amountsForUsersList = null;
        } else {
            amountsForUsersList = new ArrayList<>(amountsForUsers);
        }
        String currency = command.getCurrency();
        int timestamp = command.getTimestamp();
        // create a new pending split payment
        PendingSplitPayment pendingSplitPayment = new PendingSplitPayment(type,
                accountsList, amount, amountsForUsersList, currency, timestamp, users);
        pendingSplitPayments.add(pendingSplitPayment);
        System.out.println("Added pending split payment");
        return;
    }

}

