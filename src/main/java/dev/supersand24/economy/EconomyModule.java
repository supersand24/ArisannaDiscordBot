package dev.supersand24.economy;

import dev.supersand24.ICommand;
import dev.supersand24.IModule;
import dev.supersand24.economy.cardStore.CardStoreCommand;
import dev.supersand24.economy.expenses.DebtCommand;
import dev.supersand24.economy.expenses.ExpenseCommand;
import dev.supersand24.economy.expenses.PaymentCommand;

import java.util.Arrays;
import java.util.List;

public class EconomyModule implements IModule {

    private final List<ICommand> commands = Arrays.asList(
            new ExpenseCommand(),
            new DebtCommand(),
            new PaymentCommand(),
            new CardStoreCommand()
    );

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public List<ICommand> getCommands() {
        return commands;
    }

}
