package com.chatapp.server.network;

import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Protocol;
import com.chatapp.server.repository.UserRepository;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles communication with a single connected client.
 * Each client connection runs in its own thread.
 */
public class ClientHandler implements Runnable {

    private Socket socket;
    private ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean connected;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while (connected && (line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                Message message = Protocol.fromJson(line);
                if (message != null) {
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                log("Connection error: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    /**
     * Route incoming messages to the appropriate handler.
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case LOGIN:
                handleLogin(message);
                break;
            case REGISTER:
                handleRegister(message);
                break;
            case LOGOUT:
                handleLogout();
                break;
            case TEXT:
                handleTextMessage(message);
                break;
            case FILE_SEND:
                handleFileMessage(message);
                break;
            case PING:
                sendMessage(Message.systemMessage(MessageType.PONG, "pong"));
                break;
            default:
                log("Unhandled message type: " + message.getType());
                break;
        }
    }

    /**
     * Handle user login request.
     */
    private void handleLogin(Message message) {
        String user = message.getSender();
        String password = message.getContent();

        // Check if already logged in elsewhere
        if (ServerManager.getInstance().isOnline(user)) {
            sendMessage(Message.systemMessage(MessageType.LOGIN_FAIL, "User already logged in."));
            return;
        }

        // Verify credentials
        if (UserRepository.getInstance().authenticate(user, password)) {
            this.username = user;
            ServerManager.getInstance().addClient(username, this);

            // Send login success
            sendMessage(Message.systemMessage(MessageType.LOGIN_SUCCESS, "Login successful. Welcome " + username + "!"));

            // Notify other users
            Message onlineNotify = Message.systemMessage(MessageType.USER_ONLINE, username);
            ServerManager.getInstance().broadcastToAllExcept(username, onlineNotify);

            // Send online user list to the new user
            sendOnlineUserList();

            // Notify server GUI
            if (server.getListener() != null) {
                server.getListener().onClientConnected(username);
            }

            log(username + " logged in successfully.");
        } else {
            sendMessage(Message.systemMessage(MessageType.LOGIN_FAIL, "Invalid username or password."));
        }
    }

    /**
     * Handle user registration request.
     */
    private void handleRegister(Message message) {
        String user = message.getSender();
        String password = message.getContent();

        if (user == null || user.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendMessage(Message.systemMessage(MessageType.REGISTER_FAIL, "Username and password cannot be empty."));
            return;
        }

        if (UserRepository.getInstance().userExists(user)) {
            sendMessage(Message.systemMessage(MessageType.REGISTER_FAIL, "Username already exists."));
            return;
        }

        if (UserRepository.getInstance().registerUser(user, password)) {
            sendMessage(Message.systemMessage(MessageType.REGISTER_SUCCESS, "Registration successful! You can now login."));
            log("New user registered: " + user);
        } else {
            sendMessage(Message.systemMessage(MessageType.REGISTER_FAIL, "Registration failed. Please try again."));
        }
    }

    /**
     * Handle user logout.
     */
    private void handleLogout() {
        disconnect();
    }

    /**
     * Handle a 1-to-1 text message.
     */
    private void handleTextMessage(Message message) {
        String receiver = message.getReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "Receiver not specified."));
            return;
        }

        // Forward message to the receiver
        boolean sent = ServerManager.getInstance().sendToUser(receiver, message);
        if (!sent) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "User '" + receiver + "' is not online."));
        }
    }

    /**
     * Handle file transfer: forward file data to the receiver.
     */
    private void handleFileMessage(Message message) {
        String receiver = message.getReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "Receiver not specified."));
            return;
        }

        // Change type to FILE_RECEIVE for the recipient
        message.setType(MessageType.FILE_RECEIVE);

        boolean sent = ServerManager.getInstance().sendToUser(receiver, message);
        if (!sent) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "User '" + receiver + "' is not online. File not sent."));
        } else {
            log(username + " sent file '" + message.getFileName() + "' to " + receiver);
        }
    }

    /**
     * Send the list of currently online users to this client.
     */
    private void sendOnlineUserList() {
        List<String> onlineUsers = new ArrayList<>(ServerManager.getInstance().getOnlineUsers());
        onlineUsers.remove(username); // Don't include self
        String userListStr = String.join(",", onlineUsers);
        sendMessage(Message.systemMessage(MessageType.USER_LIST, userListStr));
    }

    /**
     * Send a message to this client.
     */
    public synchronized void sendMessage(Message message) {
        if (writer != null) {
            writer.println(Protocol.toJson(message));
        }
    }

    /**
     * Disconnect this client.
     */
    public void disconnect() {
        if (!connected) return;
        connected = false;

        if (username != null) {
            ServerManager.getInstance().removeClient(username);

            // Notify other users
            Message offlineNotify = Message.systemMessage(MessageType.USER_OFFLINE, username);
            ServerManager.getInstance().broadcastToAllExcept(username, offlineNotify);

            // Notify server GUI
            if (server.getListener() != null) {
                server.getListener().onClientDisconnected(username);
            }

            log(username + " disconnected.");
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    private void log(String msg) {
        if (server.getListener() != null) {
            server.getListener().onLog(msg);
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isConnected() {
        return connected;
    }
}
