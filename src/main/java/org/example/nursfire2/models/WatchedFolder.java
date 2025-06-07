package org.example.nursfire2.models;

public class WatchedFolder {
    private final String id;
    private final String folderPath;

    public WatchedFolder(String id, String folderPath) {
        this.id = id;
        this.folderPath = folderPath;
    }

    public String getId() {
        return id;
    }

    public String getFolderPath() {
        return folderPath;
    }
}
