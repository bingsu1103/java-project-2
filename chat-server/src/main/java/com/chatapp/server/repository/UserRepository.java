package com.chatapp.server.repository;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple file-based user repository.
 * Stores user credentials in a properties file.
 * In a production app, this would use a database like SQLite.
 */
public class UserRepository {

    private static UserRepository instance;
    private static final String DATA_DIR = "chat-data";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users.properties";

    private final ConcurrentHashMap<String, String> users; // username -> password

    private UserRepository() {
        users = new ConcurrentHashMap<>();
        ensureDataDir();
        loadUsers();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /**
     * Register a new user.
     * @return true if registration is successful
     */
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, password);
        saveUsers();
        return true;
    }

    /**
     * Authenticate a user with username and password.
     * @return true if credentials are valid
     */
    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    /**
     * Check if a username already exists.
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    /**
     * Get the total number of registered users.
     */
    public int getUserCount() {
        return users.size();
    }

    // --- Persistence ---

    private void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(fis);
            for (String key : props.stringPropertyNames()) {
                users.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void saveUsers() {
        try (FileOutputStream fos = new FileOutputStream(USERS_FILE)) {
            Properties props = new Properties();
            for (var entry : users.entrySet()) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
            props.store(fos, "Chat Application - User Registry");
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
}
