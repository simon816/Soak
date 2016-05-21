package com.simon816.soak;

import static org.spongepowered.api.command.args.GenericArguments.allOf;
import static org.spongepowered.api.command.args.GenericArguments.string;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SoakCommand {

    public static CommandCallable build() {
        return CommandSpec.builder()
                .children(createChildCommands())
                .build();
    }

    private static CommandExecutor consoleOnly(BiConsumer<MessageReceiver, CommandContext> argConsumer) {
        return (src, args) -> {
            if (!(src instanceof ConsoleSource)) {
                throw new CommandPermissionException(Text.of("Command only available on the console"));
            }
            argConsumer.accept(src, args);
            return CommandResult.success();
        };
    }

    private static Map<List<String>, CommandSpec> createChildCommands() {
        Map<List<String>, CommandSpec> commands = Maps.newHashMap();
        commands.put(Lists.newArrayList("install"), CommandSpec.builder()
                .arguments(allOf(string(Text.of("pluginid"))))
                .executor(consoleOnly(SoakCommand::install))
                .build());
        commands.put(Lists.newArrayList("update"), CommandSpec.builder()
                .executor(consoleOnly(SoakCommand::update))
                .build());
        commands.put(Lists.newArrayList("remove"), CommandSpec.builder()
                .arguments(allOf(string(Text.of("pluginid"))))
                .executor(consoleOnly(SoakCommand::remove))
                .build());
        commands.put(Lists.newArrayList("search"), CommandSpec.builder()
                .arguments(string(Text.of("query")))
                .executor(consoleOnly(SoakCommand::search))
                .build());
        return commands;
    }

    private static void install(MessageReceiver receiver, CommandContext args) {
        Collection<String> ids = args.getAll("pluginid");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No plugin IDs provided");
        }
        SoakPlugin.instance().schedule(Tasks.install(receiver, ids));

    }

    private static void update(MessageReceiver receiver, CommandContext args) {
        SoakPlugin.instance().schedule(Tasks.update(receiver));
    }

    private static void remove(MessageReceiver receiver, CommandContext args) {
        Collection<String> ids = args.getAll("pluginid");
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No plugin IDs provided");
        }
        SoakPlugin.instance().schedule(Tasks.remove(receiver, ids));
    }

    private static void search(MessageReceiver receiver, CommandContext args) {
        String query = args.<String>getOne("query").get();
        if (query.length() < 3) {
            throw new IllegalArgumentException("Query must be at least 3 characters");
        }
        SoakPlugin.instance().schedule(Tasks.search(receiver, query));
    }
}
