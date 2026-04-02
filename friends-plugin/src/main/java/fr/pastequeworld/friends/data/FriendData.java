package fr.pastequeworld.friends.data;

import java.util.ArrayList;
import java.util.List;

public class FriendData {

    private String lastKnownName;
    private List<String> friends;
    private List<String> pendingRequests;

    public FriendData() {
        this.friends = new ArrayList<String>();
        this.pendingRequests = new ArrayList<String>();
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public List<String> getFriends() {
        return friends;
    }

    public List<String> getPendingRequests() {
        return pendingRequests;
    }
}
