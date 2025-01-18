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

import java.util.ArrayList;

/**
 * Handles the status check of a card in the system.
 */
public class CheckCardStatusHandler implements CommandHandler {

    /**
     * Executes the command to check the status of a card.
     * @param command       The command containing the details of the card to be checked.
     * @param users         The list of users in the system.
     * @param exchangeRates The list of exchange rates (not used in this handler).
     * @param out           The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<org.poo.Components.PendingSplitPayment>
                                    pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        String cardNumberCheck = command.getCardNumber();
        boolean foundCard = false;

        for (User u : users) {
            for (Account accountCheck : u.getAccounts()) {
                for (Card cardCheck : accountCheck.getCards()) {
                    if (cardCheck.getCardNumber().equals(cardNumberCheck)) {
                        ObjectNode checkCardStatus = objectMapper.createObjectNode();
                        if (accountCheck.getBalance() == accountCheck.getMinimumBalance()) {
                            checkCardStatus.put("timestamp", command.getTimestamp());
                            checkCardStatus.put("description",
                                    "You have reached the minimum amount of funds, "
                                            + "the card will be frozen");
                            cardCheck.setStatus("frozen");
                            u.getTransactions().add(checkCardStatus);
                        }
                        foundCard = true;
                        break;
                    }
                }
                if (foundCard) {
                    break;
                }
            }
            if (foundCard) {
                break;
            }
        }

        if (!foundCard) {
            ObjectNode checkCardStatusResponse = objectMapper.createObjectNode();
            checkCardStatusResponse.put("command", command.getCommand());
            ObjectNode info = objectMapper.createObjectNode();
            info.put("timestamp", command.getTimestamp());
            info.put("description", "Card not found");
            checkCardStatusResponse.set("output", info);
            checkCardStatusResponse.put("timestamp", command.getTimestamp());
            out.add(checkCardStatusResponse);
        }
    }
}
