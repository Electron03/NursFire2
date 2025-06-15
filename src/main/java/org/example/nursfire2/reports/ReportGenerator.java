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
import org.example.nursfire2.models.AccessLogEntry;
import org.example.nursfire2.models.AttackEntry;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class ReportGenerator {

    public static void generateMLReport(String format) {
        File file = chooseFile("Сохранить отчёт об атаках", format, "ml_report");
        if (file != null) {
            if (format.equals("PDF")) {
                generateAttackReportPdf(file);
            } else {
                generateAttackReportExcel(file);
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

    private static void generateAttackReportPdf(File file) {
        List<AttackEntry> logs=DatabaseManager.getAttackLogs();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("ОТЧЁТ ОБ ОБНАРУЖЕННЫХ АТАКАХ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Количество записей: " + logs.size()));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            Stream.of("Пакет", "Тип атаки", "Уровень", "Метод", "Время").forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });

            for (AttackEntry entry : logs) {
                table.addCell(entry.getPacketId());
                table.addCell(entry.getAttackType());
                table.addCell(String.valueOf(entry.getSeverity()));
                table.addCell(entry.getDetectionMethod());
                table.addCell(entry.getDetectedAt());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            showError("Ошибка генерации PDF", e.getMessage());
        }
    }

    private static void generateAttackReportExcel(File file) {
        List<org.example.nursfire2.models.AttackEntry> logs = org.example.nursfire2.database.DatabaseManager.getAttackLogs();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attack Report");
            int rowIdx = 0;

            Row header = sheet.createRow(rowIdx++);
            String[] columns = {"Пакет", "Тип атаки", "Уровень", "Метод", "Время"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (org.example.nursfire2.models.AttackEntry entry : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getPacketId());
                row.createCell(1).setCellValue(entry.getAttackType());
                row.createCell(2).setCellValue(entry.getSeverity());
                row.createCell(3).setCellValue(entry.getDetectionMethod());
                row.createCell(4).setCellValue(entry.getDetectedAt());
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
