package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.account.Account;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;

/**
 * Handles report generation based on commands.
 */
public final class ReportHandler implements CommandHandler {
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates,
                        final ArrayNode out, final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        ObjectMapper objectMapper = new ObjectMapper();

        String accountIBAN5 = command.getAccount();
        Integer starttimestamp = command.getStartTimestamp();
        Integer endtimestamp = command.getEndTimestamp();

        User userReport = null;
        Account accountReport = null;

        for (User u : users) {
            for (Account account5 : u.getAccounts()) {
                if (account5.getIBAN().equals(accountIBAN5)
                        || (account5.getAlias() != null
                        && account5.getAlias().equals(accountIBAN5))) {
                    userReport = u;
                    accountReport = account5;
                    break;
                }
            }
            if (userReport != null) {
                break;
            }
        }

        if (accountReport == null || userReport == null) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("command", command.getCommand());

            ObjectNode errorNotFound = objectMapper.createObjectNode();
            errorNotFound.put("timestamp", command.getTimestamp());
            errorNotFound.put("description", "Account not found");

            error.set("output", errorNotFound);
            error.put("timestamp", command.getTimestamp());
            out.add(error);
            return;
        }

        ObjectNode report = objectMapper.createObjectNode();
        report.put("command", command.getCommand());

        ObjectNode output = objectMapper.createObjectNode();
        output.put("IBAN", accountIBAN5);
        output.put("balance", accountReport.getBalance());
        output.put("currency", accountReport.getCurrency());

        ArrayNode transactionsArray2 = objectMapper.createArrayNode();
        ArrayList<ObjectNode> uniqueTransactions = new ArrayList<>();

        for (ObjectNode transaction : userReport.getTransactions()) {
            Integer timestamp = transaction.get("timestamp").asInt();
            if (timestamp >= starttimestamp && timestamp <= endtimestamp) {
                boolean isDuplicate = false;
                for (ObjectNode uniqueTransaction : uniqueTransactions) {
                    if (transaction.equals(uniqueTransaction)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (isDuplicate) {
                    continue;
                }
                uniqueTransactions.add(transaction);
                String descriptionTR = transaction.get("description").asText();
                if (descriptionTR.equals("New card created")) {
                    String accountIban = transaction.get("account").asText();
                    if (!accountIban.equals(accountIBAN5)) {
                        continue;
                    }
                }
                transactionsArray2.add(transaction);
            }
        }

        output.set("transactions", transactionsArray2);
        report.set("output", output);
        report.put("timestamp", command.getTimestamp());
        out.add(report);
    }
}
