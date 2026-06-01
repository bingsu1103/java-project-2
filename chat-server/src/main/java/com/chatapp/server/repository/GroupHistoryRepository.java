package com.chatapp.server.repository;

import com.chatapp.common.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and loads chat history for group chats on the server side.
 */
public class GroupHistoryRepository {
    private static GroupHistoryRepository instance;
    private static final String DATA_DIR = "chat-data" + File.separator + "group-history";
    private final Gson gson;

    private GroupHistoryRepository() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDataDir();
    }

    public static synchronized GroupHistoryRepository getInstance() {
        if (instance == null) {
            instance = new GroupHistoryRepository();
        }
        return instance;
    }

    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void saveGroupMessage(String groupId, Message message) {
        List<Message> history = loadGroupHistory(groupId);
        history.add(message);
        
        File file = new File(DATA_DIR + File.separator + groupId + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            System.err.println("Error saving group history for " + groupId + ": " + e.getMessage());
        }
    }

    public synchronized List<Message> loadGroupHistory(String groupId) {
        File file = new File(DATA_DIR + File.separator + groupId + ".json");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<ArrayList<Message>>(){}.getType();
            List<Message> history = gson.fromJson(reader, type);
            return history != null ? history : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading group history for " + groupId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
