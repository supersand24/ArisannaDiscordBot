package dev.supersand24.autovoice;

import dev.supersand24.ICommand;
import dev.supersand24.IModule;
import dev.supersand24.autovoice.voice.VoiceCommand;

import java.util.List;

public class AutoVoiceModule implements IModule {

    private final List<ICommand> commands = List.of(
            new VoiceCommand()
    );

    @Override
    public String getName() {
        return "auto-vc";
    }

    @Override
    public List<ICommand> getCommands() {
        return commands;
    }

}
