package org.example.nursfire2.models;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class EncryptedFile {
    private  SimpleStringProperty id;
    private  SimpleStringProperty fileName;
    private  SimpleStringProperty filePath;
    private  SimpleStringProperty dateEncrypted;
    private  String Key;
    private String path;

    public EncryptedFile(String id, String fileName, String filePath, String dateEncrypted, String key, String path) {
        this.id = new SimpleStringProperty(id);
        this.fileName = new SimpleStringProperty(fileName);
        this.filePath = new SimpleStringProperty(filePath);
        this.dateEncrypted = new SimpleStringProperty(dateEncrypted);
        this.Key=new String(key);
        this.path=new String(path);
    }

    public String getId() {
        return id.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public StringProperty filePathProperty() {
        return filePath;
    }

    public StringProperty dateEncryptedProperty() {
        return dateEncrypted;
    }
    public String getKey(){
        return Key;
    }
    public String getPath(){
        return path;
    }
}

