package org.example.nursfire2.controller;

import ai.djl.util.Pair;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.nursfire2.manager.PasswordManager;
import org.example.nursfire2.stage.*;

import java.util.Optional;

import java.io.IOException;


public class LoginController {
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button setPasswordButton;

    @FXML
    public void initialize() {
        if (PasswordManager.isPasswordSet()) {
            // Пароль уже установлен
            loginButton.setVisible(true);
            setPasswordButton.setText("Сменить пароль");
        } else {
            // Пароль еще не установлен
            loginButton.setVisible(false);
            setPasswordButton.setText("Установить пароль");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        StageFunction stageFunction = new StageFunction();
        String password = passwordField.getText();

        if (PasswordManager.verifyPassword(password)) {
            messageLabel.setText("Успешный вход!");
            messageLabel.setStyle("-fx-text-fill: green;");

            try {
                stageFunction.openMainApplication(event, PageName.MAINPAGE_STRING,PageName.MAIN,true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Неверный пароль!");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleSetPassword() {
        if (!PasswordManager.isPasswordSet()) {
            // Если пароля ещё нет — просто установить
            String newPassword = passwordField.getText();
            if (newPassword.isEmpty()) {
                messageLabel.setText("Введите новый пароль!");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }
            PasswordManager.savePassword(newPassword);
            messageLabel.setText("Пароль успешно установлен!");
            messageLabel.setStyle("-fx-text-fill: green;");
            loginButton.setVisible(true);
            setPasswordButton.setText("Сменить пароль");
            return;
        }

        // Если пароль уже есть — открываем диалог смены
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Смена пароля");

        // Кнопки OK / Cancel
        ButtonType changeButtonType = new ButtonType("Сменить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        // Поля для старого и нового пароля
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Старый пароль");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Новый пароль");

        // Вертикальный layout
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(new Label("Введите старый и новый пароль:"), oldPasswordField, newPasswordField);
        dialog.getDialogPane().setContent(vbox);

        // Когда нажали "Сменить" — возвращаем пару старый/новый пароль
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                return new Pair<>(oldPasswordField.getText(), newPasswordField.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(passwords -> {
            String oldPassword = passwords.getKey();
            String newPassword = passwords.getValue();

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                messageLabel.setText("Поля не должны быть пустыми!");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (PasswordManager.verifyPassword(oldPassword)) {
                PasswordManager.savePassword(newPassword);
                messageLabel.setText("Пароль успешно изменён!");
                messageLabel.setStyle("-fx-text-fill: green;");
            } else {
                messageLabel.setText("Неверный старый пароль!");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        });
    }

}
