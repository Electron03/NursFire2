package org.example.nursfire2.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.example.nursfire2.ML.PacketClassifier;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.PredictionResult;
import org.example.nursfire2.service.ServiceManager;
import org.example.nursfire2.sniffer.PacketSniffer;
import org.example.nursfire2.stage.PageName;
import org.example.nursfire2.utils.FolderMonitor;

import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(PageName.LOGINPAGE_STRING));

        Scene scene = new Scene(fxmlLoader.load(), 400, 300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(PageName.LOGINCSS_STRING)).toExternalForm());
        stage.setTitle("Безопасный вход");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        try {
            DatabaseManager.createTables();

////            Запуск сниффера в отдельном потоке
//          new Thread(() -> {
//                try {
//                    PacketSniffer.startSniffing();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//           }).start();
//


        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // Запустить все сервисы
            ServiceManager.startFolderMonitor();

            // можно добавить ServiceManager.startPacketSniffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch();
    }
    @Override
    public void stop() {
        ServiceManager.stopFolderMonitor();
        // ServiceManager.stopPacketSniffer();
    }
}
