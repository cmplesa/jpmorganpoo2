package org.poo.Components;

import lombok.Data;
import org.poo.account.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
public class PendingSplitPayment {
    private String type;
    private ArrayList<String> accounts;
    private double amount;
    private ArrayList<Double> amountsForUsers;
    private String currency;
    private Integer timestamp;
    // mail -> True if accepted, False if declined
    private final Map<String, Boolean> userResponses = new HashMap<>();

    public PendingSplitPayment(final String type, final ArrayList<String> accounts,
                               final double amount, final ArrayList<Double> amountsForUsers,
                               final String currency, final Integer timestamp,
                               final ArrayList<User> users) {
        this.type = type;
        this.accounts = accounts;
        this.amount = amount;
        this.amountsForUsers = amountsForUsers;
        this.currency = currency;
        this.timestamp = timestamp;

        for (String account : accounts) {
            for (User user : users) {
                for (Account acc : user.getAccounts()) {
                    if (acc.getIBAN().equals(account)) {
                        userResponses.put(user.getEmail(), false);
                        break;
                    }
                }
            }
        }
    }
}
