package com.chatapp.server.network;

import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.Protocol;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all connected client handlers.
 * Singleton pattern - shared across the server.
 */
public class ServerManager {

    private static ServerManager instance;
    private final ConcurrentHashMap<String, ClientHandler> connectedClients;

    private ServerManager() {
        connectedClients = new ConcurrentHashMap<>();
    }

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    /**
     * Register a client after successful login.
     */
    public void addClient(String username, ClientHandler handler) {
        connectedClients.put(username, handler);
    }

    /**
     * Remove a client on disconnect.
     */
    public void removeClient(String username) {
        connectedClients.remove(username);
    }

    /**
     * Get a specific client handler by username.
     */
    public ClientHandler getClient(String username) {
        return connectedClients.get(username);
    }

    /**
     * Check if a user is currently connected.
     */
    public boolean isOnline(String username) {
        return connectedClients.containsKey(username);
    }

    /**
     * Get all connected usernames.
     */
    public Collection<String> getOnlineUsers() {
        return connectedClients.keySet();
    }

    /**
     * Get the number of connected clients.
     */
    public int getOnlineCount() {
        return connectedClients.size();
    }

    /**
     * Send a message to a specific user.
     */
    public boolean sendToUser(String username, Message message) {
        ClientHandler handler = connectedClients.get(username);
        if (handler != null) {
            handler.sendMessage(message);
            return true;
        }
        return false;
    }

    /**
     * Broadcast a message to all connected clients.
     */
    public void broadcastToAll(Message message) {
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(message);
        }
    }

    /**
     * Broadcast a message to all except the sender.
     */
    public void broadcastToAllExcept(String excludeUsername, Message message) {
        for (var entry : connectedClients.entrySet()) {
            if (!entry.getKey().equals(excludeUsername)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    /**
     * Disconnect all clients (used during server shutdown).
     */
    public void disconnectAll() {
        for (ClientHandler handler : connectedClients.values()) {
            handler.disconnect();
        }
        connectedClients.clear();
    }
}
