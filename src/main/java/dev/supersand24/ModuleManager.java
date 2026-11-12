package dev.supersand24;

import dev.supersand24.autovoice.AutoVoiceModule;
import dev.supersand24.economy.EconomyModule;
import dev.supersand24.grandarchive.GrandArchiveModule;
import dev.supersand24.logging.LogModule;

import java.util.Arrays;
import java.util.List;

public class ModuleManager {

    public static final List<IModule> MODULES = Arrays.asList(
            new EconomyModule(),
            new AutoVoiceModule(),
            new LogModule(),
            new GrandArchiveModule()
    );

    public static IModule getModuleForCommand(String commandName) {
        for (IModule module : MODULES) {
            for (ICommand cmd : module.getCommands()) {
                if (cmd.getName().equals(commandName)) {
                    return module;
                }
            }
        }
        return null;
    }

    public static ICommand getCommand(String commandName) {
        for (IModule module : MODULES) {
            for (ICommand cmd : module.getCommands()) {
                if (cmd.getName().equals(commandName)) {
                    return cmd;
                }
            }
        }
        return null;
    }

}
