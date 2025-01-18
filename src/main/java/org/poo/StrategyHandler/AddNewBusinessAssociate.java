package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.account.AccountBusiness;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

public final class AddNewBusinessAssociate implements CommandHandler {
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String email = command.getEmail();
        String role = command.getRole();
        String account = command.getAccount();

        User user = null;
        AccountBusiness businessAccount = null;

        // searching the account by IBAN
        for (User u : users) {
            for (Account acc : u.getAccounts()) {
                if (acc.getIBAN().equals(account) && acc instanceof AccountBusiness) {
                    user = u;
                    businessAccount = (AccountBusiness) acc;
                    break;
                }
            }
            if (businessAccount != null) {
                break;
            }
        }

        if (businessAccount == null) {
            System.out.println("Business account not found.");
            return;
        }

        System.out.println("Is this email already in the system? "
                + businessAccount.getAssociates().containsKey(email));

        if (businessAccount.getAssociates().containsKey(email)) {
            System.out.println("This email is already associated with the account.");
            return;
        }

        try {
            businessAccount.getAssociates().put(email, role);
            businessAccount.getOrderTracker().put(email, businessAccount.getOrderTracker().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
