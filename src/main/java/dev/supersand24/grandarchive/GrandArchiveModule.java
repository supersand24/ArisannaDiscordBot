package dev.supersand24.grandarchive;

import dev.supersand24.ICommand;
import dev.supersand24.IModule;

import java.util.List;

public class GrandArchiveModule implements IModule {

    private final List<ICommand> commands = List.of(
            new SyncCardsCommand(),
            new LookupCardCommand()
    );

    public String getName() {
        return "grand-archive";
    }

    @Override
    public List<ICommand> getCommands() { return commands; }

}
