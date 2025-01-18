package org.poo.Components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.StrategyHandler.CommandHandler;
import org.poo.StrategyHandler.AcceptSplitHandler;
import org.poo.StrategyHandler.AddAccountHandler;
import org.poo.StrategyHandler.AddInterestHandler;
import org.poo.StrategyHandler.AddNewBusinessAssociate;
import org.poo.StrategyHandler.BusinessReportHandler;
import org.poo.StrategyHandler.CashWithdrawalHandler;
import org.poo.StrategyHandler.ChangeDepositLimitHandler;
import org.poo.StrategyHandler.ChangeInterestRateHandler;
import org.poo.StrategyHandler.ChangeSpendingLimitHandler;
import org.poo.StrategyHandler.CheckCardStatusHandler;
import org.poo.StrategyHandler.CreateCardHandler;
import org.poo.StrategyHandler.CreateOneTimeCardHandler;
import org.poo.StrategyHandler.DeleteAccountHandler;
import org.poo.StrategyHandler.DeleteCardHandler;
import org.poo.StrategyHandler.DepositFundsHandler;
import org.poo.StrategyHandler.PayOnlineHandler;
import org.poo.StrategyHandler.PrintTransactionsHandler;
import org.poo.StrategyHandler.PrintUsersHandler;
import org.poo.StrategyHandler.RejectSplitPaymentHandler;
import org.poo.StrategyHandler.ReportHandler;
import org.poo.StrategyHandler.SendMoneyHandler;
import org.poo.StrategyHandler.SetAliasHandler;
import org.poo.StrategyHandler.SetMinimumBalanceHandler;
import org.poo.StrategyHandler.SpendingsReportHandler;
import org.poo.StrategyHandler.SplitPaymentHandler;
import org.poo.StrategyHandler.UpgradePlanHandler;
import org.poo.StrategyHandler.WithdrawSavingsHandler;

import org.poo.fileio.CommandInput;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a bank that handles various banking operations.
 */
public final class Bank {
    private final ArrayList<User> users;
    private final ArrayList<ExchangeRate> exchangeRates;
    private final ArrayList<CommandInput> commands;
    private final Map<String, CommandHandler> commandHandlers;
    private final ArrayList<PendingSplitPayment> pendingSplitPayments = new ArrayList<>();


    public Bank() {
        this.users = new ArrayList<>();
        this.exchangeRates = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.commandHandlers = new HashMap<>();

        initializeHandlers(users, exchangeRates, commands);
    }

    public Bank(final ArrayList<User> usersList,
                final ArrayList<ExchangeRate> exchangeRatesList,
                final ArrayList<CommandInput> commandsList) {
        this.users = usersList;
        this.exchangeRates = exchangeRatesList;
        this.commands = commandsList;
        this.commandHandlers = new HashMap<>();

        initializeHandlers(usersList, exchangeRatesList, commandsList);
    }

    private void initializeHandlers(final ArrayList<User> usersList,
                                    final ArrayList<ExchangeRate> exchangeRatesList,
                                    final ArrayList<CommandInput> commandsList) {
        // Register all command handlers
        commandHandlers.put("printUsers", new PrintUsersHandler());
        commandHandlers.put("addAccount", new AddAccountHandler());
        commandHandlers.put("createCard", new CreateCardHandler(users, new ObjectMapper()));
        commandHandlers.put("addFunds", new DepositFundsHandler());
        commandHandlers.put("deleteAccount", new DeleteAccountHandler());
        commandHandlers.put("payOnline", new PayOnlineHandler());
        commandHandlers.put("sendMoney", new SendMoneyHandler());
        commandHandlers.put("setAlias", new SetAliasHandler());
        commandHandlers.put("printTransactions", new PrintTransactionsHandler());
        commandHandlers.put("setMinimumBalance", new SetMinimumBalanceHandler());
        commandHandlers.put("checkCardStatus", new CheckCardStatusHandler());
        commandHandlers.put("splitPayment", new SplitPaymentHandler());
        commandHandlers.put("report", new ReportHandler());
        commandHandlers.put("spendingsReport", new SpendingsReportHandler());
        commandHandlers.put("changeInterestRate", new ChangeInterestRateHandler());
        commandHandlers.put("addInterest", new AddInterestHandler());
        commandHandlers.put("createOneTimeCard", new CreateOneTimeCardHandler());
        commandHandlers.put("deleteCard", new DeleteCardHandler());
        commandHandlers.put("withdrawSavings", new WithdrawSavingsHandler());
        commandHandlers.put("upgradePlan", new UpgradePlanHandler());
        commandHandlers.put("cashWithdrawal", new CashWithdrawalHandler());
        commandHandlers.put("acceptSplitPayment", new AcceptSplitHandler());
        commandHandlers.put("addNewBusinessAssociate", new AddNewBusinessAssociate());
        commandHandlers.put("businessReport", new BusinessReportHandler());
        commandHandlers.put("rejectSplitPayment", new RejectSplitPaymentHandler());
        commandHandlers.put("businessReport", new BusinessReportHandler());
        commandHandlers.put("changeSpendingLimit", new ChangeSpendingLimitHandler());
        commandHandlers.put("changeDepositLimit", new ChangeDepositLimitHandler());
    }

    /**
     * Executes banking operations based on the provided commands.
     *
     * @param usersList       The list of users.
     * @param rateList        The list of exchange rates.
     * @param commandList     The list of commands.
     * @param out             The JSON array to which the result will be appended.
     */
    public void banking(final ArrayList<User> usersList, final ArrayList<ExchangeRate> rateList,
                        final ArrayList<CommandInput> commandList, final ArrayNode out,
                        final ArrayList<Commerciant> commerciantsList) {
        for (CommandInput command : commandList) {
            CommandHandler handler = commandHandlers.get(command.getCommand());

            if (handler != null) {
                handler.execute(command, usersList, rateList, out,
                        commerciantsList, pendingSplitPayments);
            } else {
                System.out.println("Invalid command: " + command.getCommand());
            }
        }
    }
}
