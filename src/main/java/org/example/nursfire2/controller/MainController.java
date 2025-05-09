package org.example.nursfire2.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.stage.StageFunction;

import java.io.IOException;

public class MainController {
    @FXML
    private Button addFolderButton;

    @FXML
    private Button encryptFileButton;

    @FXML
    private Button reportsButton;

    @FXML
    public void handleAddFolder(ActionEvent event) throws IOException {
        StageFunction stageFunction = new StageFunction();
        stageFunction.openMainApplication(event, PageName.handleAddFolder,PageName.ADD_FOLDER,true);
    }
    @FXML
    public void handleEncryptFile(ActionEvent event) throws IOException {
        StageFunction stageFunction = new StageFunction();
        stageFunction.openMainApplication(event, PageName.handleEncryptFile,PageName.ENCRYPT_FILE,true);
    }
    @FXML
    public void handleReports(ActionEvent event) throws IOException {
        StageFunction stageFunction = new StageFunction();
        stageFunction.openMainApplication(event, PageName.handleReports,PageName.REPORTS,true);
    }
}
