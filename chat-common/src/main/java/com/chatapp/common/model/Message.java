package com.chatapp.common.model;

import com.chatapp.common.protocol.MessageType;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a chat message exchanged between client and server.
 * This is the core data structure of the protocol.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MessageType type;
    private String sender;      // Username of sender
    private String receiver;    // Username of receiver (or group ID for group messages)
    private String content;     // Text content, file name, or emoji code
    private String timestamp;
    private byte[] fileData;    // Binary data for file transfer
    private String fileName;    // Original file name for file transfer
    private long fileSize;      // File size in bytes

    public Message() {
        this.timestamp = LocalDateTime.now().format(FORMATTER);
    }

    public Message(MessageType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
    }

    // --- Static Factory Methods ---

    /**
     * Creates a text message from sender to receiver.
     */
    public static Message textMessage(String sender, String receiver, String content) {
        return new Message(MessageType.TEXT, sender, receiver, content);
    }

    /**
     * Creates a group text message.
     */
    public static Message groupTextMessage(String sender, String groupId, String content) {
        return new Message(MessageType.GROUP_TEXT, sender, groupId, content);
    }

    /**
     * Creates a login request message.
     */
    public static Message loginMessage(String username, String password) {
        return new Message(MessageType.LOGIN, username, null, password);
    }

    /**
     * Creates a register request message.
     */
    public static Message registerMessage(String username, String password) {
        return new Message(MessageType.REGISTER, username, null, password);
    }

    /**
     * Creates a system/server response message.
     */
    public static Message systemMessage(MessageType type, String content) {
        return new Message(type, "SERVER", null, content);
    }

    // --- Getters & Setters ---

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s (%s)",
                timestamp, sender, receiver, content, type);
    }
}
