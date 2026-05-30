package com.chatapp.client.network;

import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side network handler.
 * Manages socket connection to the server and message I/O.
 */
public class ChatClient {

    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected;
    private Thread listenerThread;

    private final List<MessageListener> listeners;

    public ChatClient() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.connected = false;
    }

    /**
     * Connect to the server.
     */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;

        // Start listening for incoming messages
        listenerThread = new Thread(this::listenForMessages, "ChatClient-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Send a message to the server.
     */
    public void sendMessage(Message message) {
        if (writer != null && connected) {
            writer.println(Protocol.toJson(message));
        }
    }

    /**
     * Send a login request.
     */
    public void login(String username, String password) {
        sendMessage(Message.loginMessage(username, password));
    }

    /**
     * Send a register request.
     */
    public void register(String username, String password) {
        sendMessage(Message.registerMessage(username, password));
    }

    /**
     * Send a text message to another user.
     */
    public void sendTextMessage(String sender, String receiver, String text) {
        sendMessage(Message.textMessage(sender, receiver, text));
    }

    /**
     * Send a logout notification.
     */
    public void logout() {
        sendMessage(Message.systemMessage(MessageType.LOGOUT, ""));
        disconnect();
    }

    /**
     * Listen for incoming messages from the server.
     */
    private void listenForMessages() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                Message message = Protocol.fromJson(line);
                if (message != null) {
                    notifyListeners(message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                connected = false;
                notifyDisconnected();
            }
        }
    }

    // --- Listener management ---

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Message message) {
        for (MessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyDisconnected() {
        for (MessageListener listener : listeners) {
            listener.onDisconnected();
        }
    }

    // --- Getters ---

    public boolean isConnected() {
        return connected;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Callback interface for receiving messages from the server.
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
        void onDisconnected();
    }
}
