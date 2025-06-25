package org.example.nursfire2.reports;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.AccessLogEntry;
import org.example.nursfire2.models.AttackEntry;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.itextpdf.text.FontFactory.*;

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
        List<AttackEntry> logs = DatabaseManager.getAttackLogs();

        try {
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont("C:\\Windows\\Fonts\\arial.ttf", PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);

            // Заголовок
            Paragraph title = new Paragraph(new Text("ОТЧЁТ ОБ ОБНАРУЖЕННЫХ АТАКАХ").setFont(font).setFontSize(16));
            document.add(title);
            document.add(new Paragraph(new Text("Количество записей: " + logs.size()).setFont(font)));

            // 🔢 Подсчёт количества по типам атак
            Map<String, Long> attackStats = logs.stream()
                    .collect(Collectors.groupingBy(AttackEntry::getAttackType, Collectors.counting()));

            document.add(new Paragraph(new Text("Статистика по типам атак:").setFont(font)));

            for (Map.Entry<String, Long> entry : attackStats.entrySet()) {
                String line = "- " + entry.getKey() + ": " + entry.getValue();
                document.add(new Paragraph(new Text(line).setFont(font)));
            }

            document.add(new Paragraph("\n"));

            // 📋 Таблица с логами
            float[] columnWidths = {80F, 100F, 60F, 100F, 100F};
            Table table = new Table(columnWidths);

            String[] headers = {"Пакет", "Тип атаки", "Уровень", "Метод", "Время"};
            for (String h : headers) {
                Cell cell = new Cell().add(new Paragraph(h).setFont(font));
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            for (AttackEntry entry : logs) {
                table.addCell(new Cell().add(new Paragraph(entry.getPacketId()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getAttackType()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getSeverity())).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getDetectionMethod()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getDetectedAt()).setFont(font)));
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
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont("C:\\Windows\\Fonts\\arial.ttf", PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);

            Paragraph title = new Paragraph(new Text("ОТЧЁТ ПО ЗАЩИТЕ ФАЙЛОВОЙ СИСТЕМЫ").setFont(font).setFontSize(16));
            document.add(title);
            document.add(new Paragraph(new Text("Количество записей: " + logs.size()).setFont(font)));
            document.add(new Paragraph("\n"));

            float[] columnWidths = {100F, 150F, 100F, 100F, 100F};
            Table table = new Table(columnWidths);

            String[] headers = {"Пользователь", "Файл", "Тип доступа", "Результат", "Время"};
            for (String h : headers) {
                Cell cell = new Cell().add(new Paragraph(h).setFont(font));
                cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                table.addHeaderCell(cell);
            }

            for (AccessLogEntry entry : logs) {
                table.addCell(new Cell().add(new Paragraph(entry.getUsername()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getFilepath()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccessType()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccessResult()).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(entry.getAccessTime()).setFont(font)));
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
