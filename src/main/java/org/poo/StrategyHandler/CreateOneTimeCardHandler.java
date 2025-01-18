package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Card;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.Components.PendingSplitPayment;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
/**
 * Handles the creation of a one-time use card for a specified user account.
 */
public class CreateOneTimeCardHandler implements CommandHandler {

    /**
     * Executes the "createOneTimeCard" command.
     *
     * @param command       The command input containing account and user details.
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

        String accountIBAN2 = command.getAccount();
        String email2 = command.getEmail();

        User user3 = null;
        for (User u : users) {
            if (u.getEmail().equals(email2)) {
                user3 = u;
                break;
            }
        }

        if (user3 == null) {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", "User not found");
            ObjectNode createOneTimeCardResponse = objectMapper.createObjectNode();
            createOneTimeCardResponse.put("command", command.getCommand());
            createOneTimeCardResponse.put("timestamp", command.getTimestamp());
            createOneTimeCardResponse.set("output", errorResponse);
            out.add(createOneTimeCardResponse);
            return;
        }

        Card card2 = new Card("OneTime");
        for (Account account3 : user3.getAccounts()) {
            if (account3.getIBAN().equals(accountIBAN2)
                    || (account3.getAlias() != null
                            && account3.getAlias().equals(accountIBAN2))) {
                account3.getCards().add(card2);
                break;
            }
        }

        ObjectNode createOneTimeCardResponse = objectMapper.createObjectNode();
        createOneTimeCardResponse.put("timestamp", command.getTimestamp());
        createOneTimeCardResponse.put("description", "New card created");
        createOneTimeCardResponse.put("card", card2.getCardNumber());
        createOneTimeCardResponse.put("cardHolder", email2);
        createOneTimeCardResponse.put("account", accountIBAN2);
        user3.getTransactions().add(createOneTimeCardResponse);
    }
}
