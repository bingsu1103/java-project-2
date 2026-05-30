package com.chatapp.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a chat group with a name and list of members.
 */
public class ChatGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private String groupId;
    private String groupName;
    private String owner;  // Username of the group creator
    private List<String> members;  // List of member usernames

    public ChatGroup() {
        this.members = new ArrayList<>();
    }

    public ChatGroup(String groupId, String groupName, String owner) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.owner = owner;
        this.members = new ArrayList<>();
        this.members.add(owner);
    }

    // --- Getters & Setters ---

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean isMember(String username) {
        return members.contains(username);
    }

    @Override
    public String toString() {
        return groupName + " (" + members.size() + " members)";
    }
}
