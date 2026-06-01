package com.chatapp.server.network;

import com.chatapp.common.model.ChatGroup;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Protocol;
import com.chatapp.server.repository.UserRepository;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            case GROUP_TEXT:
                handleGroupText(message);
                break;
            case GROUP_CREATE:
                handleGroupCreate(message);
                break;
            case GROUP_LEAVE:
                handleGroupLeave(message);
                break;
            case HISTORY_REQUEST:
                handleHistoryRequest(message);
                break;
            case FILE_SEND:
                handleFileMessage(message);
                break;
            case VOICE_CALL_REQUEST:
            case VOICE_CALL_ACCEPT:
            case VOICE_CALL_REJECT:
            case VOICE_CALL_END:
            case VOICE_DATA:
            case VIDEO_CALL_REQUEST:
            case VIDEO_CALL_ACCEPT:
            case VIDEO_CALL_REJECT:
            case VIDEO_CALL_END:
            case VIDEO_DATA:
                handleForwardMessage(message);
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

            // Send online user list and group list to the new user after a short delay
            // This ensures the client UI has fully transitioned to MainFrame and registered its listener
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                sendOnlineUserList();
                sendUserGroupList();
            }).start();

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

    private void handleForwardMessage(Message message) {
        String receiver = message.getReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "Receiver not specified."));
            return;
        }

        boolean sent = ServerManager.getInstance().sendToUser(receiver, message);
        if (!sent) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "User '" + receiver + "' is offline."));
        }
    }

    private void handleGroupLeave(Message message) {
        String groupId = message.getContent();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        ChatGroup group = ServerManager.getInstance().getGroup(groupId);
        if (group != null) {
            group.removeMember(username);
            log(username + " left group " + group.getGroupName() + " (" + groupId + ")");
            
            if (group.getMembers().isEmpty()) {
                ServerManager.getInstance().removeGroup(groupId);
                log("Group " + group.getGroupName() + " (" + groupId + ") has no members left. Deleted group configuration.");
                // Delete server group history file
                java.io.File historyFile = new java.io.File("chat-data" + java.io.File.separator + "group-history" + java.io.File.separator + groupId + ".json");
                if (historyFile.exists()) {
                    historyFile.delete();
                }
            } else {
                ServerManager.getInstance().updateGroup(group);
                // Broadcast GROUP_LEAVE notification to the remaining members of the group
                Message leaveNotify = new Message(MessageType.GROUP_LEAVE, username, groupId, username + " left the group.");
                com.chatapp.server.repository.GroupHistoryRepository.getInstance().saveGroupMessage(groupId, leaveNotify);
                ServerManager.getInstance().sendToGroup(groupId, leaveNotify);
            }
        }
    }

    private void handleHistoryRequest(Message message) {
        String groupId = message.getContent();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        java.util.List<Message> history = com.chatapp.server.repository.GroupHistoryRepository.getInstance().loadGroupHistory(groupId);
        String jsonHistory = new com.google.gson.Gson().toJson(history);
        sendMessage(new Message(MessageType.HISTORY_RESPONSE, "SERVER", groupId, jsonHistory));
    }

    private void sendUserGroupList() {
        java.util.List<String> userGroups = new java.util.ArrayList<>();
        for (ChatGroup g : ServerManager.getInstance().getAllGroups()) {
            if (g.isMember(username)) {
                userGroups.add(g.getGroupId() + ":" + g.getGroupName());
            }
        }
        String groupListStr = String.join(",", userGroups);
        sendMessage(new Message(MessageType.GROUP_LIST, "SERVER", username, groupListStr));
    }

    private void handleGroupCreate(Message message) {
        String groupName = message.getContent();
        String groupId = UUID.randomUUID().toString();
        
        ChatGroup group = new ChatGroup(groupId, groupName, username);
        
        // Members list could be sent in receiver field or fileData, for now just creator
        // Let's assume the client sends comma separated members in receiver field
        String memberList = message.getReceiver();
        if (memberList != null && !memberList.isEmpty()) {
            for (String member : memberList.split(",")) {
                group.addMember(member.trim());
            }
        }
        
        ServerManager.getInstance().addGroup(group);
        
        // Notify members
        Message success = new Message(MessageType.GROUP_CREATE_SUCCESS, "SERVER", username, groupName);
        success.setReceiver(groupId); // use receiver field to pass group ID
        
        for (String member : group.getMembers()) {
            if (ServerManager.getInstance().isOnline(member)) {
                ServerManager.getInstance().sendToUser(member, success);
            }
        }
        log("Group created: " + groupName + " by " + username);
    }

    private void handleGroupText(Message message) {
        String groupId = message.getReceiver(); // For group text, receiver is groupId
        ChatGroup group = ServerManager.getInstance().getGroup(groupId);
        if (group == null) {
            sendMessage(Message.systemMessage(MessageType.ERROR, "Group not found."));
            return;
        }
        
        // Save to server group history
        com.chatapp.server.repository.GroupHistoryRepository.getInstance().saveGroupMessage(groupId, message);
        
        // Broadcast to group members
        ServerManager.getInstance().sendToGroup(groupId, message);
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
