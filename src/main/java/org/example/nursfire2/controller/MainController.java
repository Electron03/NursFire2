package org.example.nursfire2.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.nursfire2.ML.PacketClassifier;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.stage.StageFunction;
import org.example.nursfire2.reports.ReportGenerator;
import javafx.scene.chart.PieChart;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.AccessLogEntry;
import org.example.nursfire2.models.AttackEntry;
import java.util.List;
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
    private PieChart mlPieChart;

    @FXML
    private PieChart accessLogPieChart;

    @FXML
    public void initialize() {
        loadMLPieChart();
        loadAccessLogBarChart();
    }

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
    private void loadMLPieChart() {
        List<AttackEntry> logs = DatabaseManager.getAttackLogs();
        var classCounts = new java.util.HashMap<String, Integer>();

        for (AttackEntry log : logs) {
            classCounts.put(log.getAttackType(), classCounts.getOrDefault(log.getAttackType(), 0) + 1);
        }

        for (var entry : classCounts.entrySet()) {
            mlPieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void loadAccessLogBarChart() {
        List<AccessLogEntry> logs = DatabaseManager.getAccessLogs();
        var accessTypeCounts = new java.util.HashMap<String, Integer>();

        for (AccessLogEntry log : logs) {
            accessTypeCounts.put(log.getAccessType(), accessTypeCounts.getOrDefault(log.getAccessType(), 0) + 1);
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (var entry : accessTypeCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        accessLogPieChart.setData(pieChartData);

    }
    @FXML
    public void overfitting(){
        PacketClassifier packetClassifier=new PacketClassifier();
        String db="network_traffic.db";
        String file="traffic_data.arff";
        packetClassifier.retrainModelFromDBAndFile(db,file);

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
