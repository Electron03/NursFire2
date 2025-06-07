package org.example.nursfire2.models;

import javafx.beans.property.SimpleStringProperty;

public class AccessLogEntry {
    private final SimpleStringProperty username;
    private final SimpleStringProperty filepath;
    private final SimpleStringProperty accessType;
    private final SimpleStringProperty accessTime;
    private final SimpleStringProperty accessResult;

    public AccessLogEntry(String username, String filepath, String accessType, String accessTime, String accessResult) {
        this.username = new SimpleStringProperty(username);
        this.filepath = new SimpleStringProperty(filepath);
        this.accessType = new SimpleStringProperty(accessType);
        this.accessTime = new SimpleStringProperty(accessTime);
        this.accessResult = new SimpleStringProperty(accessResult);
    }

    public String getUsername() { return username.get(); }
    public String getFilepath() { return filepath.get(); }
    public String getAccessType() { return accessType.get(); }
    public String getAccessTime() { return accessTime.get(); }
    public String getAccessResult() { return accessResult.get(); }
}
