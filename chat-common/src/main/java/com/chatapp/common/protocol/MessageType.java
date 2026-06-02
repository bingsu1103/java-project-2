package com.chatapp.common.protocol;

/**
 * Enum defining all message types used in the chat protocol.
 */
public enum MessageType {
    // --- Authentication ---
    REGISTER,           // Client requests to register a new user
    REGISTER_SUCCESS,   // Server confirms registration
    REGISTER_FAIL,      // Server rejects registration

    LOGIN,              // Client requests to login
    LOGIN_SUCCESS,      // Server confirms login
    LOGIN_FAIL,         // Server rejects login

    LOGOUT,             // Client notifies logout

    // --- Chat Messages ---
    TEXT,               // Regular text message (1-to-1)
    GROUP_TEXT,         // Text message in a group chat
    EMOJI,              // Emoji message
    MESSAGE_DELETE,     // Deletion of a single message

    // --- File Transfer ---
    FILE_SEND,          // Client sends a file
    FILE_RECEIVE,       // Server forwards file to recipient
    FILE_CHUNK,         // A chunk of file data

    // --- User List ---
    USER_LIST,          // Server sends list of online users
    USER_ONLINE,        // Notification: a user came online
    USER_OFFLINE,       // Notification: a user went offline

    // --- Group Management ---
    GROUP_CREATE,       // Client requests to create a group
    GROUP_CREATE_SUCCESS,
    GROUP_CREATE_FAIL,
    GROUP_JOIN,         // Client requests to join a group
    GROUP_LEAVE,        // Client leaves a group
    GROUP_LIST,         // Server sends list of groups
    GROUP_MEMBERS,      // Server sends group member list

    // --- Chat History ---
    HISTORY_REQUEST,    // Client requests chat history
    HISTORY_RESPONSE,   // Server sends chat history
    HISTORY_DELETE,     // Client requests to delete history entries

    // --- Voice & Video (Phase 12) ---
    VOICE_CALL_REQUEST,
    VOICE_CALL_ACCEPT,
    VOICE_CALL_REJECT,
    VOICE_CALL_END,
    VOICE_DATA,

    VIDEO_CALL_REQUEST,
    VIDEO_CALL_ACCEPT,
    VIDEO_CALL_REJECT,
    VIDEO_CALL_END,
    VIDEO_DATA,

    // --- System ---
    PING,               // Keep-alive ping
    PONG,               // Keep-alive pong
    ERROR               // General error message
}
