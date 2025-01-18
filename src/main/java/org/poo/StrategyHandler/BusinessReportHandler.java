package org.poo.StrategyHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.PendingSplitPayment;
import org.poo.Components.User;
import org.poo.Components.BusinessComerciantPayment;

import org.poo.account.Account;
import org.poo.account.AccountBusiness;
import org.poo.fileio.CommandInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles generating business reports.
 */
public final class BusinessReportHandler implements CommandHandler {

    /**a
     * Executes the business report command.
     *
     * @param command The command input containing report details.
     * @param users The list of users in the bank.
     * @param exchangeRates The list of exchange rates for currency conversion.
     * @param out The JSON array to store command results.
     * @param commerciantsList The list of commerciants.
     * @param pendingSplitPayments The list of pending split payments.
     */
    @Override
    public void execute(final CommandInput command, final ArrayList<User> users,
                        final ArrayList<ExchangeRate> exchangeRates, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList,
                        final ArrayList<PendingSplitPayment> pendingSplitPayments) {
        String iban = command.getAccount();
        String reportType = command.getType();
        int startTimestamp = command.getStartTimestamp();
        int endTimestamp = command.getEndTimestamp();
        int timestamp = command.getTimestamp();

        // Find the business account
        AccountBusiness businessAccount = null;
        for (User user : users) {
            for (Account account : user.getAccounts()) {
                if (account.getAccountType().equals("business") && account.getIBAN().equals(iban)) {
                    businessAccount = (AccountBusiness) account;
                    break;
                }
            }
            if (businessAccount != null) {
                break;
            }
        }

        // Handle case where account is not found
        if (businessAccount == null) {
            ObjectNode errorNode = new ObjectMapper().createObjectNode();
            errorNode.put("command", "businessReport");
            ObjectNode output = errorNode.putObject("output");
            output.put("description", "Business account not found.");
            output.put("timestamp", timestamp);
            errorNode.put("timestamp", timestamp);
            out.add(errorNode);
            return;
        }

        if ("transaction".equals(reportType)) {
            generateTransactionReport(businessAccount,
                    startTimestamp, endTimestamp, out, timestamp, users);
        } else if ("commerciant".equals(reportType)) {
            generateCommerciantReport(command, businessAccount,
                    startTimestamp, endTimestamp, out, timestamp, users, commerciantsList);
        }
    }

    private void generateTransactionReport(final AccountBusiness businessAccount,
                                           final int startTimestamp,
                                           final int endTimestamp,
                                           final ArrayNode out,
                                           final int timestamp,
                                           final ArrayList<User> users) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reportNode = mapper.createObjectNode();
        reportNode.put("command", "businessReport");
        ObjectNode outputNode = reportNode.putObject("output");

        outputNode.put("IBAN", businessAccount.getIBAN());
        outputNode.put("balance", businessAccount.getBalance());
        outputNode.put("currency", businessAccount.getCurrency());
        outputNode.put("spending limit", businessAccount.getSpendingLimit());
        outputNode.put("deposit limit", businessAccount.getDepositLimit());
        outputNode.put("statistics type", "transaction");

        class AssociateInfo {
            private final String email;
            private final String name;
            private final String role;
            private final double spent;
            private final double deposited;
            private final int order;

            AssociateInfo(final String email, final String name, final String role,
                                 final double spent, final double deposited, final int order) {
                this.email = email;
                this.name = name;
                this.role = role;
                this.spent = spent;
                this.deposited = deposited;
                this.order = order;
            }

            public String getEmail() {
                return email;
            }

            public String getName() {
                return name;
            }

            public String getRole() {
                return role;
            }

            public double getSpent() {
                return spent;
            }

            public double getDeposited() {
                return deposited;
            }

            public int getOrder() {
                return order;
            }
        }

        ArrayList<AssociateInfo> managers = new ArrayList<>();
        ArrayList<AssociateInfo> employees = new ArrayList<>();

        double totalSpent = 0;
        double totalDeposited = 0;

        Map<String, Integer> orderTracker = businessAccount.getOrderTracker();

        for (Map.Entry<String, String> associate : businessAccount.getAssociates().entrySet()) {
            String email = associate.getKey();
            String role = associate.getValue();

            if ("owner".equals(role)) {
                continue;
            }

            double spent = businessAccount.getSpending().getOrDefault(email, 0.0);
            double deposited = businessAccount.getDeposits().getOrDefault(email, 0.0);

            String displayName = email;
            for (User user : users) {
                if (user.getEmail().equals(email)) {
                    displayName = user.getLastName() + " " + user.getFirstName();
                    break;
                }
            }

            int order = orderTracker.getOrDefault(email, Integer.MAX_VALUE);

            AssociateInfo info = new AssociateInfo(email,
                    displayName, role, spent, deposited, order);

            if ("manager".equals(role)) {
                managers.add(info);
            } else if ("employee".equals(role)) {
                employees.add(info);
            }

            totalSpent += spent;
            totalDeposited += deposited;
        }

        managers.sort((m1, m2) -> Integer.compare(m1.getOrder(), m2.getOrder()));
        employees.sort((e1, e2) -> Integer.compare(e1.getOrder(), e2.getOrder()));

        ArrayNode managersNode = outputNode.putArray("managers");
        for (AssociateInfo manager : managers) {
            ObjectNode associateNode = mapper.createObjectNode();
            associateNode.put("username", manager.getName());
            associateNode.put("spent", manager.getSpent());
            associateNode.put("deposited", manager.getDeposited());
            managersNode.add(associateNode);
        }

        ArrayNode employeesNode = outputNode.putArray("employees");
        for (AssociateInfo employee : employees) {
            ObjectNode associateNode = mapper.createObjectNode();
            associateNode.put("username", employee.getName());
            associateNode.put("spent", employee.getSpent());
            associateNode.put("deposited", employee.getDeposited());
            employeesNode.add(associateNode);
        }

        outputNode.put("total spent", totalSpent);
        outputNode.put("total deposited", totalDeposited);
        reportNode.put("timestamp", timestamp);

        out.add(reportNode);
    }

    private void generateCommerciantReport(final CommandInput command,
                                           final AccountBusiness businessAccount,
                                           final int startTimestamp,
                                           final int endTimestamp,
                                           final ArrayNode out,
                                           final int timestamp,
                                           final ArrayList<User> users,
                                           final ArrayList<Commerciant> commerciantsList) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reportNode = mapper.createObjectNode();
        reportNode.put("command", "businessReport");
        ObjectNode outputNode = reportNode.putObject("output");

        outputNode.put("IBAN", businessAccount.getIBAN());
        outputNode.put("balance", businessAccount.getBalance());
        outputNode.put("currency", businessAccount.getCurrency());
        outputNode.put("spending limit", businessAccount.getSpendingLimit());
        outputNode.put("deposit limit", businessAccount.getDepositLimit());
        outputNode.put("statistics type", "commerciant");

        List<Map.Entry<String, BusinessComerciantPayment>> sortedCommerciants =
                new ArrayList<>(businessAccount.getCommerciantsPayments().entrySet());

        sortedCommerciants.sort((e1, e2) ->
                e1.getKey().compareToIgnoreCase(e2.getKey())
        );

        ArrayNode commerciantsNode = outputNode.putArray("commerciants");

        for (Map.Entry<String, BusinessComerciantPayment> entry : sortedCommerciants) {
            String commerciantName = entry.getKey();
            BusinessComerciantPayment payment = entry.getValue();

            ObjectNode commerciantNode = mapper.createObjectNode();
            commerciantNode.put("commerciant", commerciantName);
            commerciantNode.put("total received", payment.getTotalReceived());

            ArrayNode employeesNode = commerciantNode.putArray("employees");
            for (String employeeEmail : payment.getEmployees()) {
                employeesNode.add(employeeEmail);
            }

            ArrayNode managersNode = commerciantNode.putArray("managers");
            for (String managerEmail : payment.getManagers()) {
                managersNode.add(managerEmail);
            }

            commerciantsNode.add(commerciantNode);
        }

        reportNode.put("timestamp", timestamp);
        out.add(reportNode);
    }
}
