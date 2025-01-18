package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.Components.PendingSplitPayment;
import java.util.ArrayList;

/**
 * Handles setting the minimum balance for accounts based on commands.
 */
public final class SetMinimumBalanceHandler implements CommandHandler {
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        String accountIBAN4 = command.getAccount();
        double minBalance = command.getAmount();

        User user5 = null;
        for (User u : users) {
            for (Account account5 : u.getAccounts()) {
                if (account5.getIBAN().equals(accountIBAN4)
                        || (account5.getAlias() != null
                        && account5.getAlias().equals(accountIBAN4))) {
                    user5 = u;
                    account5.setMinimumBalance(minBalance);
                    break;
                }
            }
            if (user5 != null) {
                break;
            }
        }

        if (user5 == null) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", "Account not found");
            errorResponse.put("timestamp", command.getTimestamp());
            ObjectNode response = objectMapper.createObjectNode();
            response.put("command", command.getCommand());
            response.set("output", errorResponse);
            out.add(response);
            return;
        }
    }
}
