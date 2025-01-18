package org.poo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.Components.Bank;
import org.poo.Components.Commerciant;
import org.poo.Components.ExchangeRate;
import org.poo.Components.User;
import org.poo.checker.Checker;
import org.poo.checker.CheckerConstants;

import org.poo.fileio.*;
import org.poo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public final class Main {
    private Main() {
    }

    public static void main(final String[] args) throws IOException {
        File directory = new File(CheckerConstants.TESTS_PATH);
        Path path = Paths.get(CheckerConstants.RESULT_PATH);

        if (Files.exists(path)) {
            File resultFile = new File(String.valueOf(path));
            for (File file : Objects.requireNonNull(resultFile.listFiles())) {
                file.delete();
            }
            resultFile.delete();
        }
        Files.createDirectories(path);

        var sortedFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).
                sorted(Comparator.comparingInt(Main::fileConsumer))
                .toList();

        for (File file : sortedFiles) {
            String filepath = CheckerConstants.OUT_PATH + file.getName();
            File out = new File(filepath);
            boolean isCreated = out.createNewFile();
            if (isCreated) {
                action(file.getName(), filepath);
            }
        }

        Checker.calculateScore();
    }

    public static void action(final String filePath1,
                              final String filePath2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(CheckerConstants.TESTS_PATH + filePath1);
        ObjectInput inputData = objectMapper.readValue(file, ObjectInput.class);

        ArrayNode output = objectMapper.createArrayNode();

        Utils.resetRandom();

        ArrayList<User> usersList = new ArrayList<>();
        for (UserInput userInput : inputData.getUsers()) {
            User user = new User(userInput.getFirstName(),
                    userInput.getLastName(), userInput.getEmail(),
                    userInput.getBirthDate(), userInput.getOccupation());
            usersList.add(user);
        }

        ArrayList<ExchangeRate> exchangeRatesList = new ArrayList<>();
        for (ExchangeInput exchangeInput : inputData.getExchangeRates()) {
            ExchangeRate exchangeRate = new ExchangeRate(exchangeInput.getFrom(),
                    exchangeInput.getTo(), exchangeInput.getRate());
            exchangeRatesList.add(exchangeRate);
        }

        ArrayList<Commerciant> commerciantsList = new ArrayList<>();
        for (CommerciantInput commerciantInput : inputData.getCommerciants()) {
            Commerciant commerciant = new Commerciant(commerciantInput.getCommerciant(),
                    commerciantInput.getId(), commerciantInput.getAccount(),
                    commerciantInput.getType(), commerciantInput.getCashbackStrategy());
            commerciantsList.add(commerciant);
        }

        Bank bank = Bank.getInstance();
        CommandInput[] commandsArray = inputData.getCommands();
        ArrayList<CommandInput> commandsList = new ArrayList<>(Arrays.asList(commandsArray));

        bank.banking(usersList, exchangeRatesList, commandsList, output, commerciantsList);

        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        objectWriter.writeValue(new File(filePath2), output);

        // Resetarea instanței pentru testare
        Bank.resetInstance();
    }

    public static int fileConsumer(final File file) {
        return Integer.parseInt(
                file.getName()
                        .replaceAll(CheckerConstants.DIGIT_REGEX, CheckerConstants.EMPTY_STR)
        );
    }
}