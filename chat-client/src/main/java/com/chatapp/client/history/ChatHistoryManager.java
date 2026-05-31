package com.chatapp.client.history;

import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.Protocol;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {

    private static final String HISTORY_DIR = "chat-data/history";

    private String currentUser;

    public ChatHistoryManager(String currentUser) {
        this.currentUser = currentUser;
        try {
            Files.createDirectories(Paths.get(HISTORY_DIR, currentUser));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path getHistoryFilePath(String target) {
        return Paths.get(HISTORY_DIR, currentUser, target + ".txt");
    }

    public void saveMessage(String target, Message message) {
        Path path = getHistoryFilePath(target);
        try (PrintWriter out = new PrintWriter(new FileWriter(path.toFile(), true))) {
            out.println(Protocol.toJson(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Message> loadHistory(String target) {
        List<Message> history = new ArrayList<>();
        Path path = getHistoryFilePath(target);
        if (Files.exists(path)) {
            try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        Message msg = Protocol.fromJson(line);
                        if (msg != null) {
                            history.add(msg);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return history;
    }

    public void clearHistory(String target) {
        Path path = getHistoryFilePath(target);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
