package dev.supersand24;

import java.util.List;

public interface IModule {

    String getName();

    List<ICommand> getCommands();

    public default boolean IsEnabled(GuildSettings settings) {
        return settings.enabledModules.contains(getName());
    }

}
