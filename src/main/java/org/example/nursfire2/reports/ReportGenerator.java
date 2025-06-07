package org.example.nursfire2.reports;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.MLPredictionLogEntry;
import org.example.nursfire2.models.AccessLogEntry;
import java.util.stream.Stream;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ReportGenerator {

    public static void generateMLReport(String format) {
        File file = chooseFile("Сохранить отчёт по машинному обучению", format, "ml_report");
        if (file != null) {
            if (format.equals("PDF")) {
                generateMLReportPdf(file);
            } else {
                generateMLReportExcel(file);
            }
            openFile(file);
        }
    }

    public static void generateFileProtectionReport(String format) {
        File file = chooseFile("Сохранить отчёт по защите файловой системы", format, "file_protection_report");
        if (file != null) {
            if (format.equals("PDF")) {
                generateFileProtectionReportPdf(file);
            } else {
                generateFileProtectionReportExcel(file);
            }
            openFile(file);
        }
    }

    private static File chooseFile(String title, String format, String defaultName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultName + (format.equals("PDF") ? ".pdf" : ".xlsx"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format + " файл", "*." + (format.equals("PDF") ? "pdf" : "xlsx")));
        return fileChooser.showSaveDialog(null);
    }

    private static void generateMLReportPdf(File file) {
        List<MLPredictionLogEntry> logs = DatabaseManager.getMLPredictionLogs();
        Map<String, Long> classCounts = logs.stream().collect(Collectors.groupingBy(MLPredictionLogEntry::getPredictedClass, Collectors.counting()));
        double avgConfidence = logs.stream().mapToDouble(MLPredictionLogEntry::getConfidence).average().orElse(0);

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("ОТЧЁТ ПО РАБОТЕ МАШИННОГО ОБУЧЕНИЯ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Количество записей: " + logs.size()));
            document.add(new Paragraph("Средняя уверенность: " + String.format("%.2f", avgConfidence)));
            document.add(new Paragraph("Предсказания по классам: " + classCounts.toString()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            Stream.of("Пакет", "Модель", "Класс", "Уверенность", "Время").forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });

            for (MLPredictionLogEntry entry : logs) {
                table.addCell(entry.getPacketId());
                table.addCell(entry.getModelVersion());
                table.addCell(entry.getPredictedClass());
                table.addCell(String.format("%.2f", entry.getConfidence()));
                table.addCell(entry.getTimestamp());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            showError("Ошибка генерации PDF", e.getMessage());
        }
    }

    private static void generateMLReportExcel(File file) {
        List<MLPredictionLogEntry> logs = DatabaseManager.getMLPredictionLogs();
        Map<String, Long> classCounts = logs.stream().collect(Collectors.groupingBy(MLPredictionLogEntry::getPredictedClass, Collectors.counting()));
        double avgConfidence = logs.stream().mapToDouble(MLPredictionLogEntry::getConfidence).average().orElse(0);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ML Report");

            int rowIdx = 0;
            sheet.createRow(rowIdx++).createCell(0).setCellValue("ОТЧЁТ ПО РАБОТЕ МАШИННОГО ОБУЧЕНИЯ");
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Всего записей: " + logs.size());
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Средняя уверенность: " + String.format("%.2f", avgConfidence));
            sheet.createRow(rowIdx++).createCell(0).setCellValue("Классы: " + classCounts);
            rowIdx++;

            Row header = sheet.createRow(rowIdx++);
            String[] columns = {"Пакет", "Модель", "Класс", "Уверенность", "Время"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (MLPredictionLogEntry log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getPacketId());
                row.createCell(1).setCellValue(log.getModelVersion());
                row.createCell(2).setCellValue(log.getPredictedClass());
                row.createCell(3).setCellValue(log.getConfidence());
                row.createCell(4).setCellValue(log.getTimestamp());
            }

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
        } catch (Exception e) {
            showError("Ошибка генерации Excel", e.getMessage());
        }
    }

    private static void generateFileProtectionReportPdf(File file) {
        List<AccessLogEntry> logs = DatabaseManager.getAccessLogs();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("ОТЧЁТ ПО ЗАЩИТЕ ФАЙЛОВОЙ СИСТЕМЫ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Количество записей: " + logs.size()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            Stream.of("Пользователь", "Файл", "Тип доступа", "Результат", "Время").forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });

            for (AccessLogEntry entry : logs) {
                table.addCell(entry.getUsername());
                table.addCell(entry.getFilepath());
                table.addCell(entry.getAccessType());
                table.addCell(entry.getAccessResult());
                table.addCell(entry.getAccessTime());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            showError("Ошибка генерации PDF", e.getMessage());
        }
    }

    private static void generateFileProtectionReportExcel(File file) {
        List<AccessLogEntry> logs = DatabaseManager.getAccessLogs();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Access Report");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            String[] columns = {"Пользователь", "Файл", "Тип доступа", "Результат", "Время"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (AccessLogEntry entry : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getUsername());
                row.createCell(1).setCellValue(entry.getFilepath());
                row.createCell(2).setCellValue(entry.getAccessType());
                row.createCell(3).setCellValue(entry.getAccessResult());
                row.createCell(4).setCellValue(entry.getAccessTime());
            }

            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
        } catch (Exception e) {
            showError("Ошибка генерации Excel", e.getMessage());
        }
    }

    private static void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            showError("Ошибка открытия файла", e.getMessage());
        }
    }

    private static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
