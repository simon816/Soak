package com.simon816.soak;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.spongepowered.plugin.meta.version.ArtifactVersion;
import org.spongepowered.plugin.meta.version.DefaultArtifactVersion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OreRepository implements PluginRepository {

    private static final URL ORE_ROOT = createUrl("https://ore-staging.spongepowered.org/");
    private static final URL API_URL = createUrl(ORE_ROOT, "api/v1/");

    private static final Gson gson = new GsonBuilder().create();

    static {
        SSLHack.disableCerts();
    }

    private static URL createUrl(URL root, String url) {
        return createUrl(root.toExternalForm() + url);
    }

    private static URL createUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        }
    }

    private static Reader open(URL url) throws IOException {
        try {
            return Resources.asCharSource(url, Charsets.UTF_8).openStream();
        } catch (FileNotFoundException fnfe) {
            return null;
        }
    }

    @Override
    public PluginVersionInfo getVersionInfo(String pluginId) throws IOException {
        Reader stream = open(createUrl(API_URL, "projects/" + pluginId + "/versions"));
        if (stream == null) {
            return null;
        }
        try {
            return gson.fromJson(stream, OreVersionInfo[].class)[0];
        } finally {
            stream.close();
        }
    }

    @Override
    public Collection<PluginInfo> search(String query) throws IOException {
        Reader stream = open(createUrl(API_URL, "projects?q=" + query));
        if (stream == null) {
            return Collections.emptyList();
        }
        try {
            return Lists.newArrayList(gson.fromJson(stream, OrePluginInfo[].class));
        } finally {
            stream.close();
        }
    }

    @Override
    public PluginJar getPluginJar(PluginVersionInfo version) throws IOException {
        Reader stream = open(createUrl(API_URL, "projects/" + version.getPluginId()));
        if (stream == null) {
            return null;
        }
        OrePluginInfo info;
        try {
            info = gson.fromJson(stream, OrePluginInfo.class);
        } finally {
            stream.close();
        }
        URL zipUrl = createUrl(info.getWebLink() + "/versions/download/" + version.getVersion());
        ZipInputStream zipStream = new ZipInputStream(zipUrl.openStream());
        ZipEntry jarInfo = zipStream.getNextEntry();
        return new PluginJar() {

            @Override
            public InputStream getInputStream() {
                return zipStream;
            }

            @Override
            public String getFilename() {
                return jarInfo.getName();
            }
        };
    }

    public static class OrePluginInfo implements PluginInfo {

        private String pluginId;
        private String createdAt;
        private String name;
        private String owner;
        private String description;
        private String href;
        private Member[] members;
        private ChannelVersion recommended;

        private transient String link;
        private transient List<String> authors;
        private transient Instant date;

        @Override
        public String getWebLink() {
            if (this.link == null) {
                if (this.href.startsWith("/")) {
                    this.href = this.href.substring(1);
                }
                this.link = ORE_ROOT.toExternalForm() + this.href;
            }
            return this.link;
        }

        @Override
        public String getId() {
            return this.pluginId;
        }

        @Override
        public List<String> getAuthors() {
            if (this.authors == null) {
                this.authors = Lists.newArrayList(this.owner);
                for (Member member : this.members) {
                    this.authors.add(member.name);
                }
            }
            return this.authors;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public Instant getCreationDate() {
            if (this.date == null) {
                this.date = Instant.parse(this.createdAt);
            }
            return this.date;
        }

        @Override
        public String getRecommendedVersion() {
            return this.recommended.version;
        }

        public static class Member {

            private String name;
        }

        public static class ChannelVersion {

            private String version;
        }

    }

    public static class PartialVersion implements PartialVersionInfo {

        private String pluginId;
        private String version;

        private transient ArtifactVersion verInfo;

        @Override
        public String getPluginId() {
            return this.pluginId;
        }

        @Override
        public ArtifactVersion getVersion() {
            if (this.verInfo == null) {
                this.verInfo = new DefaultArtifactVersion(this.version);
            }
            return this.verInfo;
        }
    }

    public static class OreVersionInfo implements PluginVersionInfo {

        private String createdAt;
        private String name;
        private PartialVersion[] dependencies;
        private String pluginId;
        private long fileSize;

        private transient ArtifactVersion version;
        private transient Instant date;
        private transient List<PartialVersionInfo> dependenciesList;

        @Override
        public String getPluginId() {
            return this.pluginId;
        }

        @Override
        public ArtifactVersion getVersion() {
            if (this.version == null) {
                this.version = new DefaultArtifactVersion(this.name);
            }
            return this.version;
        }

        @Override
        public Instant getReleaseDate() {
            if (this.date == null) {
                this.date = Instant.parse(this.createdAt);
            }
            return this.date;
        }

        @Override
        public long getFileSize() {
            return this.fileSize;
        }

        @Override
        public List<PartialVersionInfo> getDependencies() {
            if (this.dependenciesList == null) {
                this.dependenciesList = Lists.newArrayList(this.dependencies);
            }
            return this.dependenciesList;
        }
    }
}
