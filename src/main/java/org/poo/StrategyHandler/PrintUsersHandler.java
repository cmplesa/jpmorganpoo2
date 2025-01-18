package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.Card;

import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles the printing of users and their account details.
 */
public final class PrintUsersHandler implements CommandHandler {

    /**
     * Executes the print users command.
     *
     * @param command        The command input containing the details of the print operation.
     * @param users          The list of users.
     * @param exchangeRates  The list of exchange rates.
     * @param out            The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode printUsers = objectMapper.createObjectNode();
        printUsers.put("command", command.getCommand());

        ArrayNode usersArray = objectMapper.createArrayNode();

        for (User user : users) {
            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("firstName", user.getFirstName());
            userNode.put("lastName", user.getLastName());
            userNode.put("email", user.getEmail());

            ArrayNode accountsArray = objectMapper.createArrayNode();
            if (user.getAccounts() != null) {
                for (Account account : user.getAccounts()) {
                    ObjectNode accountNode = objectMapper.createObjectNode();
                    accountNode.put("IBAN", account.getIBAN());
                    accountNode.put("balance", account.getBalance());
                    accountNode.put("currency", account.getCurrency());
                    accountNode.put("type", account.getAccountType());

                    ArrayNode cardsArray = objectMapper.createArrayNode();
                    if (account.getCards() != null) {
                        for (Card card : account.getCards()) {
                            ObjectNode cardNode = objectMapper.createObjectNode();
                            cardNode.put("cardNumber", card.getCardNumber());
                            cardNode.put("status", card.getStatus());
                            cardsArray.add(cardNode);
                        }
                    }
                    accountNode.set("cards", cardsArray);
                    accountsArray.add(accountNode);
                }
            }
            userNode.set("accounts", accountsArray);
            usersArray.add(userNode);
        }

        printUsers.set("output", usersArray);
        printUsers.put("timestamp", command.getTimestamp());

        out.add(printUsers);
    }
}
