package com.chatapp.common.protocol;

import com.chatapp.common.model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility class for serializing/deserializing Message objects to/from JSON.
 * This serves as the communication protocol between client and server.
 *
 * Protocol format: Each message is a single line of JSON text, terminated by newline.
 */
public class Protocol {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Gson compactGson = new Gson();

    /**
     * Serialize a Message to JSON string (compact, for network transmission).
     */
    public static String toJson(Message message) {
        return compactGson.toJson(message);
    }

    /**
     * Deserialize a JSON string to a Message object.
     */
    public static Message fromJson(String json) {
        return compactGson.fromJson(json, Message.class);
    }

    /**
     * Serialize a Message to pretty-printed JSON (for logging/debugging).
     */
    public static String toPrettyJson(Message message) {
        return gson.toJson(message);
    }

    /**
     * Validate that a JSON string can be parsed as a valid Message.
     */
    public static boolean isValidMessage(String json) {
        try {
            Message msg = fromJson(json);
            return msg != null && msg.getType() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
