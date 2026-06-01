package com.chatapp.server.repository;

import com.chatapp.common.model.ChatGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists and loads chat group definitions from groups.json.
 */
public class GroupRepository {
    private static GroupRepository instance;
    private static final String DATA_DIR = "chat-data";
    private static final String GROUPS_FILE = DATA_DIR + File.separator + "groups.json";
    
    private final ConcurrentHashMap<String, ChatGroup> groups;
    private final Gson gson;

    private GroupRepository() {
        groups = new ConcurrentHashMap<>();
        gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDataDir();
        loadGroups();
    }

    public static synchronized GroupRepository getInstance() {
        if (instance == null) {
            instance = new GroupRepository();
        }
        return instance;
    }

    public void addGroup(ChatGroup group) {
        groups.put(group.getGroupId(), group);
        saveGroups();
    }

    public ChatGroup getGroup(String groupId) {
        return groups.get(groupId);
    }

    public void removeGroup(String groupId) {
        groups.remove(groupId);
        saveGroups();
    }

    public Map<String, ChatGroup> getAllGroups() {
        return groups;
    }

    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void loadGroups() {
        File file = new File(GROUPS_FILE);
        if (!file.exists()) {
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<HashMap<String, ChatGroup>>(){}.getType();
            Map<String, ChatGroup> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                groups.putAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Error loading groups: " + e.getMessage());
        }
    }

    public synchronized void saveGroups() {
        try (Writer writer = new FileWriter(GROUPS_FILE)) {
            gson.toJson(groups, writer);
        } catch (IOException e) {
            System.err.println("Error saving groups: " + e.getMessage());
        }
    }
}
