package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.account.AccountBusiness;
import org.poo.fileio.CommandInput;
import org.poo.Components.PendingSplitPayment;

import java.util.ArrayList;

/**
 * Handles the deposit of funds into a user's account.
 */
public final class DepositFundsHandler implements CommandHandler {

    /**
     * Executes the deposit funds command.
     *
     * @param command        The command input containing the details of the deposit.
     * @param users          The list of users.
     * @param exchangeRates  The list of exchange rates.
     * @param out            The JSON array to which the result will be appended.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {

        String accountIBAN1 = command.getAccount();
        double amount = command.getAmount();


        User user2 = null;

        // Find the user
        for (User u : users) {
            if (u.getEmail().equals(command.getEmail())) {
                user2 = u;
                break;
            }
        }

        Account accountToDeposit = null;

        // Find the user and the account to deposit into
        for (User u : users) {
            for (Account account2 : u.getAccounts()) {
                if (account2.getIBAN().equals(accountIBAN1)
                        || (account2.getAlias() != null
                        && account2.getAlias().equals(accountIBAN1))) {
                    accountToDeposit = account2;
                    break;
                }
            }
        }

        if (accountToDeposit == null) {
            System.out.println("Account not found");
            return;
        }

        System.out.println("user2: " + user2.getEmail());
        if (accountToDeposit.getAccountType().equals("business")) {
            System.out.println("CHECK MAIL: " + command.getEmail());
            AccountBusiness businessAccount = (AccountBusiness) accountToDeposit;
            for (String user : businessAccount.getAssociates().keySet()) {
                if (user.equals(command.getEmail())) {
                    businessAccount.deposit(amount, user); // Update the deposits map
                    return;
                }
            }
        }




        if (user2 == null || accountToDeposit == null) {
            System.out.println("User or account not found");
            return;
        }

        if (user2.getAccounts().contains(accountToDeposit)) {
            accountToDeposit.deposit(amount);
        }
    }
}
