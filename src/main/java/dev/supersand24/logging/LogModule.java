package dev.supersand24.logging;

import dev.supersand24.ICommand;
import dev.supersand24.IModule;

import java.util.List;

public class LogModule implements IModule {

    private final List<ICommand> commands = List.of(
            new LogCommand()
    );

    @Override
    public String getName() {
        return "logger";
    }

    @Override
    public List<ICommand> getCommands() { return commands; }

}
