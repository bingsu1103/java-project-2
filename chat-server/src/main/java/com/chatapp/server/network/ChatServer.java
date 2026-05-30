package com.chatapp.server.network;

import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main chat server class.
 * Listens for incoming client connections and spawns a ClientHandler for each.
 */
public class ChatServer {

    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private ExecutorService threadPool;
    private ServerListener listener;

    public ChatServer(int port) {
        this.port = port;
        this.running = false;
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Set a listener to receive server events (for GUI updates).
     */
    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    /**
     * Start the server and begin accepting connections.
     */
    public void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;

        if (listener != null) {
            listener.onServerStarted(port);
        }

        // Accept connections in a separate thread
        threadPool.execute(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.execute(handler);

                    if (listener != null) {
                        listener.onLog("New connection from: " + clientSocket.getInetAddress().getHostAddress());
                    }
                } catch (IOException e) {
                    if (running) {
                        if (listener != null) {
                            listener.onLog("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            }
        });
    }

    /**
     * Stop the server and disconnect all clients.
     */
    public void stop() {
        running = false;
        try {
            // Notify all connected clients
            Message shutdownMsg = Message.systemMessage(MessageType.ERROR, "Server is shutting down.");
            ServerManager.getInstance().broadcastToAll(shutdownMsg);

            // Disconnect all clients
            ServerManager.getInstance().disconnectAll();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onLog("Error stopping server: " + e.getMessage());
            }
        }

        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerListener getListener() {
        return listener;
    }

    /**
     * Callback interface for server events.
     */
    public interface ServerListener {
        void onServerStarted(int port);
        void onServerStopped();
        void onClientConnected(String username);
        void onClientDisconnected(String username);
        void onLog(String message);
    }
}
