package org.example.nursfire2.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.stage.StageFunction;
import org.example.nursfire2.reports.ReportGenerator;

import java.io.IOException;
import java.util.Optional;

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
    public void handleReports() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Выбор отчёта");
        dialog.setHeaderText("Выберите тип отчёта и формат:");

        ButtonType okButtonType = new ButtonType("Открыть", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Первый ComboBox — выбор отчёта
        ComboBox<String> reportTypeBox = new ComboBox<>();
        reportTypeBox.getItems().addAll(
                "Отчёт по работе машинного обучения",
                "Отчёт по защите файловой системы"
        );
        reportTypeBox.setValue("Отчёт по работе машинного обучения");

        // Второй ComboBox — выбор формата
        ComboBox<String> formatBox = new ComboBox<>();
        formatBox.getItems().addAll("PDF", "Excel");
        formatBox.setValue("PDF");

        // Объединяем в VBox
        VBox content = new VBox(10);
        content.getChildren().addAll(new Label("Тип отчёта:"), reportTypeBox,
                new Label("Формат отчёта:"), formatBox);
        dialog.getDialogPane().setContent(content);

        // Обработка результата
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new String[] { reportTypeBox.getValue(), formatBox.getValue() };
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();

        result.ifPresent(selection -> {
            String report = selection[0];
            String format = selection[1];

            if (report.contains("машинного обучения")) {
                ReportGenerator.generateMLReport(format);
            } else if (report.contains("файловой системы")) {
                ReportGenerator.generateFileProtectionReport(format);
            }
        });
    }
}
