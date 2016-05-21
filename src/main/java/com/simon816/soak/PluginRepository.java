package com.simon816.soak;

import org.spongepowered.plugin.meta.version.ArtifactVersion;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PluginRepository {

    PluginVersionInfo getVersionInfo(String pluginId) throws IOException;

    Collection<PluginInfo> search(String query) throws IOException;

    PluginJar getPluginJar(PluginVersionInfo info) throws IOException;

    interface PluginInfo {

        String getId();

        Instant getCreationDate();

        String getName();

        String getRecommendedVersion();

        String getDescription();

        String getWebLink();

        List<String> getAuthors();

    }

    interface PartialVersionInfo {

        String getPluginId();

        ArtifactVersion getVersion();

    }

    interface PluginVersionInfo extends PartialVersionInfo {

        Instant getReleaseDate();

        long getFileSize();

        List<PartialVersionInfo> getDependencies();

    }

    interface PluginJar {

        String getFilename();

        InputStream getInputStream();
    }
}
