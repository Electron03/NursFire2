package org.example.nursfire2.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PasswordUtils {
    // Хеширование пароля с помощью SHA-256
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash); // Кодируем в строку
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хеширования!", e);
        }
    }
}

