package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Card;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;
import org.poo.Components.PendingSplitPayment;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the creation of a new card for a specified user account.
 */
public class CreateCardHandler implements CommandHandler {

    private final List<User> users;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for CreateCardHandler.
     *
     * @param users         The list of users in the system.
     * @param objectMapper  The ObjectMapper for creating JSON objects.
     */
    public CreateCardHandler(final List<User> users,
                             final ObjectMapper objectMapper) {
        this.users = users;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the "createCard" command.
     *
     * @param command       The command input containing user email and account details.
     * @param userList      The list of users (redundant due to constructor).
     * @param exchangeRates The list of exchange rates (not used in this handler).
     * @param out           The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> userList,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String accountIBAN = command.getAccount();
        String email1 = command.getEmail();

        User user1 = null;

        for (User u : userList) {
            if (u.getEmail().equals(email1)) {
                user1 = u;
                break;
            }
        }

        if (user1 == null) {
            return;
        }

        Card card = new Card("permanent");

        boolean accountFound = false;
        for (Account account1 : user1.getAccounts()) {
            if (account1.getIBAN().equals(accountIBAN)
                    || (account1.getAlias() != null
                    && account1.getAlias().equals(accountIBAN))) {
                account1.getCards().add(card);
                accountFound = true;
                break;
            }
        }

        if (!accountFound) {
            return;
        }

        ObjectNode createCardResponse = objectMapper.createObjectNode();
        createCardResponse.put("timestamp", command.getTimestamp());
        createCardResponse.put("description", "New card created");
        createCardResponse.put("card", card.getCardNumber());
        createCardResponse.put("cardHolder", email1);
        createCardResponse.put("account", accountIBAN);
        user1.getTransactions().add(createCardResponse);
    }

    private ObjectNode createErrorResponse(final String message,
                                           final CommandInput command) {
        ObjectNode errorResponse = objectMapper.createObjectNode();
        errorResponse.put("command", command.getCommand());
        errorResponse.put("timestamp", command.getTimestamp());
        ObjectNode output = objectMapper.createObjectNode();
        output.put("error", message);
        errorResponse.set("output", output);
        return errorResponse;
    }
}
