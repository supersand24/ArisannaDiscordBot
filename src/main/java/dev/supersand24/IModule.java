package dev.supersand24;

import java.util.List;

public interface IModule {

    String getName();

    List<ICommand> getCommands();

}
