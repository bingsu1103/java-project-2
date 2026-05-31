package com.chatapp.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the list of saved servers for the client.
 * Persists server entries to a JSON config file.
 */
public class ServerConfigManager {

    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "servers.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private List<ServerEntry> servers;

    public ServerConfigManager() {
        servers = new ArrayList<>();
        ensureConfigDir();
        load();

        // Add default server if list is empty
        if (servers.isEmpty()) {
            servers.add(new ServerEntry("Local Server", "localhost", 12345));
            save();
        }
    }

    // --- CRUD Operations ---

    public List<ServerEntry> getServers() {
        return new ArrayList<>(servers);
    }

    public void addServer(String name, String host, int port) {
        servers.add(new ServerEntry(name, host, port));
        save();
    }

    public void updateServer(int index, String name, String host, int port) {
        if (index >= 0 && index < servers.size()) {
            ServerEntry entry = servers.get(index);
            entry.setName(name);
            entry.setHost(host);
            entry.setPort(port);
            save();
        }
    }

    public void removeServer(int index) {
        if (index >= 0 && index < servers.size()) {
            servers.remove(index);
            save();
        }
    }

    public ServerEntry getServer(int index) {
        if (index >= 0 && index < servers.size()) {
            return servers.get(index);
        }
        return null;
    }

    // --- Persistence ---

    private void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<ServerEntry>>() {}.getType();
            List<ServerEntry> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                servers = loaded;
            }
        } catch (IOException e) {
            System.err.println("Error loading server config: " + e.getMessage());
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(servers, writer);
        } catch (IOException e) {
            System.err.println("Error saving server config: " + e.getMessage());
        }
    }

    /**
     * Represents a saved server entry.
     */
    public static class ServerEntry {
        private String name;
        private String host;
        private int port;

        public ServerEntry() {}

        public ServerEntry(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        @Override
        public String toString() {
            return name + " (" + host + ":" + port + ")";
        }
    }
}
