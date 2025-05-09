package org.example.nursfire2.manager;

import org.example.nursfire2.database.DatabaseManager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

public class FileManager {
    private static final String URL = "jdbc:sqlite:network_traffic.db";
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }
    public static void encryptFileAndLog(Path filePath, String userKey) {
        try {
            try {
                // Убедимся, что ключ длиной 16 байт (AES-128)
                if (userKey.length() < 16) {
                    userKey = String.format("%-16s", userKey); // дополним пробелами
                } else if (userKey.length() > 16) {
                    userKey = userKey.substring(0, 16); // обрежем
                }

                byte[] keyBytes = userKey.getBytes(StandardCharsets.UTF_8);
                SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                byte[] fileBytes = Files.readAllBytes(filePath);
                byte[] encryptedData = cipher.doFinal(fileBytes); // Заменяем оригинал
                String id = UUID.randomUUID().toString();
                DatabaseManager.insertEncryptedFile(id, filePath.getFileName().toString(), filePath.toString(), encryptedData, userKey);


            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void decryptFileWithUserKey(String fileId, String userKey, Path outputPath, Connection conn) {
        try {
            if (userKey.length() < 16) {
                userKey = String.format("%-16s", userKey);
            } else if (userKey.length() > 16) {
                userKey = userKey.substring(0, 16);
            }

            byte[] keyBytes = userKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            String selectSQL = "SELECT data FROM EncryptedFiles WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, fileId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    byte[] encryptedData = rs.getBytes("data");
                    byte[] decryptedData = cipher.doFinal(encryptedData);

                    Files.write(outputPath, decryptedData);
                    System.out.println("Файл успешно расшифрован в: " + outputPath);

                    // Удаляем файл из базы
                    String deleteSQL = "DELETE FROM EncryptedFiles WHERE id = ?";
                    try (PreparedStatement delStmt = conn.prepareStatement(deleteSQL)) {
                        delStmt.setString(1, fileId);
                        delStmt.executeUpdate();
                        System.out.println("Файл удалён из базы данных.");
                    }

                } else {
                    System.out.println("Файл не найден по ID: " + fileId);
                }
            }

        } catch (Exception e) {
            System.out.println("Ошибка расшифровки. Возможно, неверный ключ.");
            e.printStackTrace();
        }
    }

}
