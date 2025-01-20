package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.User;
import org.poo.Components.ExchangeRate;
import org.poo.Components.Commerciant;
import org.poo.Components.PendingSplitPayment;
import org.poo.fileio.CommandInput;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;

/**
 * Handler for upgrading a user's plan.
 */
public class UpgradePlanHandler implements CommandHandler {

    private static final double SILVER_FEE_STANDARD_STUDENT = 100.0;
    private static final double GOLD_FEE_SILVER = 250.0;
    private static final double GOLD_FEE_STANDARD_STUDENT = 350.0;

    /**
     * Executes the command to upgrade a user's plan.
     *
     * @param command the command input containing the details of the upgrade
     * @param users the list of users
     * @param exchangeRates the list of exchange rates
     * @param out the output array node
     * @param commerciantsList the list of commerciants
     * @param pendingSplitPayments the list of pending split payments
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String newPlanType = command.getNewPlanType();
        String accountIban = command.getAccount();
        int timestamp = command.getTimestamp();

        User user = null;
        for (User u : users) {
            if (u.getAccounts().stream().anyMatch(acc ->
                    acc.getIBAN().equals(accountIban))) {
                user = u;
                break;
            }
        }


        if (user == null) {
            ObjectNode output = out.addObject();
            output.put("command", "upgradePlan");
            ObjectNode outputDetails = output.putObject("output");
            outputDetails.put("description", "Account not found");
            outputDetails.put("timestamp", timestamp);
            output.put("timestamp", timestamp);
            return;
        }
        System.out.println("***TIMESTAMP  FOR UPGRADES***" + timestamp);
        System.out.println("user: " + user.getEmail());
        System.out.println("plan: " + user.getType());
        System.out.println("***TIMESTAMP FOR UPGRADES***" + timestamp);

        double fee = 0.0;
        switch (newPlanType) {
            case "silver":
                fee = user.getType().equals("standard") || user.getType().equals("student")
                        ? SILVER_FEE_STANDARD_STUDENT : 0.0;
                break;
            case "gold":
                if (user.getType().equals("silver")) {
                    fee = GOLD_FEE_SILVER;
                } else if (user.getType().equals("standard") || user.getType().equals("student")) {
                    fee = GOLD_FEE_STANDARD_STUDENT;
                }
                break;
            default:
                break;
        }

        String result = user.upgradePlan(command, newPlanType,
                fee, accountIban, exchangeRates, timestamp, out);
    }
}
