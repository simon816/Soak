package com.simon816.soak;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

@Plugin(id = "soak", name = "Soak")
public class SoakPlugin {

    private static SoakPlugin instance;
    @Inject private Logger logger;
    private PluginRepository repo;

    public SoakPlugin() {
        instance = this;
    }

    public Logger getLogger() {
        return this.logger;
    }

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this, SoakCommand.build(), "soak", "apt-get");
        this.repo = new OreRepository();
    }

    public static SoakPlugin instance() {
        return instance;
    }

    public PluginRepository getRepository() {
        return this.repo;
    }

    public void schedule(Runnable task) {
        Sponge.getScheduler().createTaskBuilder().async().execute(task).submit(this);
    }

    public Path getPluginDir() {
        return Paths.get("mods"); // TODO
    }
}
