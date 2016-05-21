package com.simon816.soak;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.simon816.soak.PluginRepository.PartialVersionInfo;
import com.simon816.soak.PluginRepository.PluginInfo;
import com.simon816.soak.PluginRepository.PluginJar;
import com.simon816.soak.PluginRepository.PluginVersionInfo;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.plugin.meta.version.DefaultArtifactVersion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Tasks {

    private abstract static class SoakTask implements Runnable {

        private final MessageReceiver reciever;
        private boolean sending;

        public SoakTask(MessageReceiver receiver) {
            this.reciever = receiver;
        }

        @Override
        public void run() {
            List<Text> messages = Lists.newArrayList();
            this.sending = true;
            Sponge.getScheduler().createTaskBuilder().intervalTicks(1).execute(task -> {
                if (!this.sending) {
                    task.cancel();
                }
                List<Text> toSend = Lists.newArrayList(messages);
                messages.clear();
                this.reciever.sendMessages(toSend);
            }).submit(SoakPlugin.instance());
            runTask(messages);
            this.sending = false;
        }

        protected abstract void runTask(List<Text> response);

    }

    private static class InstallTask extends SoakTask {

        private final Collection<String> ids;

        public InstallTask(MessageReceiver receiver, Collection<String> ids) {
            super(receiver);
            this.ids = ids;
        }

        @Override
        public void runTask(List<Text> response) {
            response.add(Text.of("Attempting installation of the plugins " + this.ids));
            for (String id : this.ids) {
                try {
                    PluginVersionInfo plugin = SoakPlugin.instance().getRepository().getVersionInfo(id);
                    if (plugin == null) {
                        response.add(Text.of("Plugin ID '" + id + "' not found. Skipping"));
                        continue;
                    }
                    performInstallation(plugin, response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class UpdateTask extends SoakTask {

        public UpdateTask(MessageReceiver receiver) {
            super(receiver);
        }

        @Override
        public void runTask(List<Text> response) {
            Collection<PluginContainer> plugins = Sponge.getPluginManager().getPlugins();
            for (PluginContainer plugin : plugins) {
                if (!plugin.getInstance().isPresent()) {
                    continue; // Don't care about virtual plugins
                }
                response.add(Text.of("Querying latest version of " + plugin.getName() + " (current=" + plugin.getVersion() + ")"));
                try {
                    PluginVersionInfo updated = SoakPlugin.instance().getRepository().getVersionInfo(plugin.getId());
                    if (updated == null) {
                        response.add(Text.of("Not found in the plugin repository"));
                        continue;
                    }
                    performInstallation(updated, response);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static class RemoveTask extends SoakTask {

        private final Collection<String> ids;

        public RemoveTask(MessageReceiver receiver, Collection<String> ids) {
            super(receiver);
            this.ids = ids;
        }

        @Override
        public void runTask(List<Text> response) {
            System.out.println("Remove: " + this.ids);
        }
    }

    private static class SearchTask extends SoakTask {

        private final String query;

        public SearchTask(MessageReceiver receiver, String query) {
            super(receiver);
            this.query = query;
        }

        @Override
        public void runTask(List<Text> response) {
            try {
                Collection<PluginInfo> pluginMetas = SoakPlugin.instance().getRepository().search(this.query);
                if (pluginMetas.isEmpty()) {
                    response.add(Text.of("No plugins found for query '" + this.query + "'"));
                    return;
                }
                response.add(Text.of("The following plugins were found for the query '" + this.query + "':"));
                for (PluginInfo plugin : pluginMetas) {
                    formatPlugin(plugin, response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static SoakTask install(MessageReceiver receiver, Collection<String> ids) {
        return new InstallTask(receiver, ids);
    }

    private static void performInstallation(PluginVersionInfo plugin, List<Text> response) {
        response.add(Text.of("Preparing installation of " + plugin.getPluginId() + " version " + plugin.getVersion()));
        List<PartialVersionInfo> deps = plugin.getDependencies();
        if (!deps.isEmpty()) {
            response.add(Text.of("Found the following dependencies"));
            for (PartialVersionInfo dep : deps) {
                response.add(Text.of(dep.getPluginId() + "@" + dep.getVersion()));
                Optional<PluginContainer> existing = Sponge.getPluginManager().getPlugin(dep.getPluginId());
                if (existing.isPresent()) {
                    Optional<String> version = existing.get().getVersion();
                    if (version.isPresent()) {
                        DefaultArtifactVersion existingVersion = new DefaultArtifactVersion(version.get());
                        if (dep.getVersion().compareTo(existingVersion) > 0) {
                            response.add(Text.of(existing.get().getName() + " requires update from " + existingVersion + " to " + dep.getVersion()));
                        } else {
                            response.add(Text.of("Dependency satified"));
                        }
                    } else {
                        response.add(Text.of("Version unknown for plugin " + existing.get().getName() + ", assuming compatible"));
                    }
                } else {
                    response.add(Text.of("Dependency not found locally, searching on remote repository"));
                    try {
                        PluginVersionInfo depVersion = SoakPlugin.instance().getRepository().getVersionInfo(dep.getPluginId());
                        if (depVersion == null) {
                            response.add(Text.of("Could not find dependency"));
                        } else {
                            performInstallation(depVersion, response);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        response.add(Text.of("Will now install " + plugin.getPluginId()));
        try {
            PluginJar jar = SoakPlugin.instance().getRepository().getPluginJar(plugin);
            Path file = Files.createFile(SoakPlugin.instance().getPluginDir().resolve(jar.getFilename()));
            response.add(Text.of("Destination: " + file));
            OutputStream output = Files.newOutputStream(file);
            ByteStreams.copy(jar.getInputStream(), output);
            jar.getInputStream().close();
            output.close();
            response.add(Text.of("Success"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void formatPlugin(PluginInfo plugin, List<Text> response) {
        response.add(Text.of(plugin.getName() + " (id=" + plugin.getId() + ")"));
        response.add(Text.of("  Version: " + plugin.getRecommendedVersion()));
        response.add(Text.of("  Description: " + plugin.getDescription()));
        response.add(Text.of("  Link: " + plugin.getWebLink()));
    }

    public static SoakTask update(MessageReceiver receiver) {
        return new UpdateTask(receiver);
    }

    public static SoakTask remove(MessageReceiver receiver, Collection<String> ids) {
        return new RemoveTask(receiver, ids);
    }

    public static SoakTask search(MessageReceiver receiver, String query) {
        return new SearchTask(receiver, query);
    }

}
