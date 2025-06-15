package org.example.nursfire2.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.manager.FileManager;
import org.example.nursfire2.models.EncryptedFile;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.stage.StageFunction;

import javax.sound.midi.Patch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class HandleEncryptFileView {
    @FXML
    private TextField pathField;
    @FXML
    private TextField keyField;
    @FXML
    private Button browseButton;
    @FXML
    private TableView<EncryptedFile> encryptedFilesTable;

    @FXML
    private TableColumn<EncryptedFile, String> fileNameColumn;

    @FXML
    private TableColumn<EncryptedFile, String> filePathColumn;
    @FXML
    private TableColumn<EncryptedFile, Void> actionColumn;

    @FXML
    private TableColumn<EncryptedFile, String> dateEncryptedColumn;

    private final ObservableList<EncryptedFile> encryptedFilesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        browseButton.setId(null);
        browseButton.getStyleClass().clear();
        browseButton.setStyle("");

reset();
    }

    public void reset(){
        fileNameColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        filePathColumn.setCellValueFactory(cellData -> cellData.getValue().filePathProperty());
        dateEncryptedColumn.setCellValueFactory(cellData -> cellData.getValue().dateEncryptedProperty());

        List<EncryptedFile> loadedFiles = DatabaseManager.getEncryptedFiles();
        encryptedFilesList.setAll(loadedFiles);

        encryptedFilesTable.setItems(encryptedFilesList);

        addDecryptButtonToTable();
    }
    @FXML
    public void handleBrowse(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        // (Необязательно) Установить фильтр типов файлов
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Все файлы", "*.*"),
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            pathField.setText(selectedFile.getAbsolutePath());
        }
    }
    @FXML
    public void handleEncrypt(ActionEvent event) {
        String path = pathField.getText();
        if (path == null || path.isEmpty()) {
            showAlert("Ошибка", "Укажите путь к папке.");
            return;
        }
        Path path1= Paths.get(path);
        String key = keyField.getText();
        if(key==null){
            showAlert("ошибка","нет ключа");
        }
        FileManager.encryptFileAndLog(path1,key);
        reset();

    }

    @FXML
    public void handleBack(ActionEvent event) throws IOException {
        StageFunction stageFunction = new StageFunction();
        stageFunction.openMainApplication(event, PageName.MAINPAGE_STRING,PageName.MAIN,true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void addDecryptButtonToTable() {
        Callback<TableColumn<EncryptedFile, Void>, TableCell<EncryptedFile, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<EncryptedFile, Void> call(final TableColumn<EncryptedFile, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Расшифровать");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            EncryptedFile selectedFile = getTableView().getItems().get(getIndex());
                            try {
                                Connection connection=DatabaseManager.connect();
                                Path path = Paths.get(selectedFile.getPath());

                                FileManager.decryptFileWithUserKey(selectedFile.getId(),selectedFile.getKey(),path,connection);
                                // Удаление элемента из таблицы после успешной расшифровки
                                getTableView().getItems().remove(selectedFile);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }

                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }

}
