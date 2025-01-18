package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public interface CommandHandler {
    /**
     * Executes a specific command based on the implementation.
     *
     * @param command       The command input to process.
     * @param users         List of users in the bank.
     * @param exchangeRates List of exchange rates.
     * @param out           Output result as a JSON array.
     */
    void execute(CommandInput command, ArrayList<User> users,
                 ArrayList<ExchangeRate> exchangeRates, ArrayNode out,
                 ArrayList<Commerciant> commerciantsList,
                 ArrayList<PendingSplitPayment> pendingSplitPayments);
}
