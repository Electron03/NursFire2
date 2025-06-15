package org.example.nursfire2.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.AccessLogEntry;
import org.example.nursfire2.models.WatchedFolder;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.stage.StageFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.example.nursfire2.manager.FolderAccessManager.*;

public class HandleAddFolderView {
    @FXML
    private TableView<WatchedFolder> folderTable;
    @FXML private TableView<AccessLogEntry> accessLogTable;
    @FXML private TableColumn<AccessLogEntry, String> usernameColumn;
    @FXML private TableColumn<AccessLogEntry, String> filepathColumn;
    @FXML private TableColumn<AccessLogEntry, String> accessTypeColumn;
    @FXML private TableColumn<AccessLogEntry, String> accessTimeColumn;
    @FXML private TableColumn<AccessLogEntry, String> accessResultColumn;
    @FXML
    private TableColumn<WatchedFolder, String> pathColumn;

    @FXML
    private TableColumn<WatchedFolder, Void> actionColumn;

    private ObservableList<WatchedFolder> folderList = FXCollections.observableArrayList();
    @FXML
    private TextField pathField;
    @FXML
    private Button browseButton;
    @FXML
    private ComboBox<String> permissionComboBox;
    @FXML
    private Label result;
    @FXML
    public void initialize() {
        browseButton.setId(null);
        browseButton.getStyleClass().clear();
        browseButton.setStyle("");
      reset();
    }
    public void reset(){
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("folderPath"));

        pathColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFolderPath()));

        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");

            {
                deleteButton.getStyleClass().clear();
                deleteButton.setStyle("");
                deleteButton.setOnAction(e -> {
                    WatchedFolder folder = getTableView().getItems().get(getIndex());
                    Path path = Paths.get(folder.getFolderPath());
                    if (DatabaseManager.deleteWatchedFolder(folder.getFolderPath())) {
                        folderTable.getItems().remove(folder);
                        setFolderAccessBack(path);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });

        ObservableList<WatchedFolder> folderList = FXCollections.observableArrayList();
        folderList.addAll(DatabaseManager.getWatchedFolderList());
        folderTable.setItems(folderList);
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        filepathColumn.setCellValueFactory(new PropertyValueFactory<>("filepath"));
        accessTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accessType"));
        accessTimeColumn.setCellValueFactory(new PropertyValueFactory<>("accessTime"));
        accessResultColumn.setCellValueFactory(new PropertyValueFactory<>("accessResult"));

        List<AccessLogEntry> logs = DatabaseManager.getAccessLogs();
        accessLogTable.setItems(FXCollections.observableArrayList(logs));
    }
    @FXML
    private void onBrowseClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберите папку");

        File selectedDir = directoryChooser.showDialog(getPrimaryStage());
        if (selectedDir != null) {
            pathField.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    private void onOkClicked() {
        String selected = permissionComboBox.getValue();
        if (selected == null || pathField.getText().isEmpty()) {
            System.out.println("⚠️ Путь не указан или не выбрано действие.");
            return;
        }

        Path folderPath = Paths.get(pathField.getText());

        switch (selected) {
            case "Только чтение":
                setFolderReadOnly(folderPath);
                break;
            case "Чтение и запись":
                setFolderWritable(folderPath);
                break;
            case "Полный доступ":
                setFolderAccessBack(folderPath);
                break;
            default:
                System.out.println("❓ Неизвестный вариант доступа.");
        }
        if(DatabaseManager.addWatchedFolder(folderPath.toString())){
            result.setText("папка успешно добавлено");
        }else result.setText("ошибка добавлении папки");
        printPermissions(folderPath);
        reset();
    }
    @FXML
    private void onExit(ActionEvent event) throws IOException {
        StageFunction stageFunction = new StageFunction();
        stageFunction.openMainApplication(event,PageName.MAINPAGE_STRING,PageName.MAIN,true);
    }
    private Stage getPrimaryStage() {
        return (Stage) pathField.getScene().getWindow();
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
