package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Card;
import org.poo.Components.Commerciant;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Handles the deletion of a card from a user's account.
 */
public class DeleteCardHandler implements CommandHandler {

    /**
     * Executes the command to delete a card from a user's account.
     *
     * @param command       The command containing the details of the card to be deleted.
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

        String cardNumber = command.getCardNumber();
        String email3 = command.getEmail();

        User user34 = null;
        for (User u : users) {
            if (u.getEmail().equals(email3)) {
                user34 = u;
                break;
            }
        }

        if (user34 == null) {
            return;
        }

        String accountIBAN34 = "";
        for (Account account34 : user34.getAccounts()) {
            for (Card card34 : account34.getCards()) {
                if (card34.getCardNumber().equals(cardNumber)) {
                    accountIBAN34 = account34.getIBAN();
                    break;
                }
            }
            if (!accountIBAN34.equals("")) {
                break;
            }
        }

        Account account34 = null;

        for (Account account : user34.getAccounts()) {
            if (account.getIBAN().equals(accountIBAN34)) {
                account34 = account;
                break;
            }
        }

        if (account34 == null) {
            return;
        }

        if (account34.getBalance() > 0) {
            return;
        }

        boolean cardDeleted = false;
        for (Account account4 : user34.getAccounts()) {
            Iterator<Card> it = account4.getCards().iterator();
            while (it.hasNext()) {
                Card c = it.next();
                if (c.getCardNumber().equals(cardNumber)) {
                    it.remove();
                    cardDeleted = true;
                    break;
                }
            }
            if (cardDeleted) {
                break;
            }
        }

        if (!cardDeleted) {
            return;
        }

        ObjectNode deleteCard = objectMapper.createObjectNode();
        deleteCard.put("timestamp", command.getTimestamp());
        deleteCard.put("description", "The card has been destroyed");
        deleteCard.put("card", cardNumber);
        deleteCard.put("cardHolder", email3);
        deleteCard.put("account", accountIBAN34);
        user34.getTransactions().add(deleteCard);
    }
}
