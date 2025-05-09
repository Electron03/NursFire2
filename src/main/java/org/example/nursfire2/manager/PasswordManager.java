package org.example.nursfire2.manager;

import org.example.nursfire2.utils.PasswordUtils;

import java.io.*;
import java.nio.file.*;

public class PasswordManager {
    private static final String FILE_PATH = "password.txt";

    // Сохранение хешированного пароля в файл
    public static void savePassword(String password) {
        String hashedPassword = PasswordUtils.hashPassword(password);
        try {
            Files.write(Paths.get(FILE_PATH), hashedPassword.getBytes());
            System.out.println("Пароль сохранён.");
        } catch (IOException e) {
            System.out.println("Ошибка сохранения пароля: " + e.getMessage());
        }
    }

    // Проверка введённого пароля
    public static boolean verifyPassword(String inputPassword) {
        try {
            String savedHash = Files.readString(Paths.get(FILE_PATH)).trim();
            String inputHash = PasswordUtils.hashPassword(inputPassword);
            return savedHash.equals(inputHash);
        } catch (IOException e) {
            System.out.println("Ошибка чтения пароля: " + e.getMessage());
            return false;
        }
    }

    // Проверка, существует ли пароль
    public static boolean isPasswordSet() {
        return Files.exists(Paths.get(FILE_PATH));
    }
}
