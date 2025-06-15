package org.example.nursfire2.utils;

import org.example.nursfire2.controller.HandleAddFolderView;
import org.example.nursfire2.database.DatabaseManager;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.example.nursfire2.database.DatabaseManager.insertAccessLog;
import static org.example.nursfire2.database.DatabaseManager.loadWatchedFoldersFromDB;

public class FolderMonitor implements Runnable {

    private final WatchService watchService;
    private final Set<Path> registeredFolders = new HashSet<>();
    private volatile boolean running = true;

    public FolderMonitor(Connection connection) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    public void stop() {
        running = false;
        try {
            watchService.close(); // остановит блокировку на watchService.take()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerFolders() {
        List<Path> folders = loadWatchedFoldersFromDB();
        for (Path folder : folders) {
            if (!registeredFolders.contains(folder)) {
                try {
                    folder.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    registeredFolders.add(folder);
                    System.out.println("folder register: " + folder);
                } catch (IOException e) {
                    System.err.println("error register folder: " + folder);
                }
            }
        }
    }

    @Override
    public void run() {
        registerFolders();

        new Thread(() -> {
            while (running) {
                registerFolders();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {}
            }
        }).start();

        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path folder = (Path) key.watchable();
                    Path fullPath = folder.resolve((Path) event.context());

                    String accessType = switch (kind.name()) {
                        case "ENTRY_CREATE" -> "CREATE";
                        case "ENTRY_DELETE" -> "DELETE";
                        case "ENTRY_MODIFY" -> "MODIFY";
                        default -> "UNKNOWN";
                    };
                    String id = UUID.randomUUID().toString();
                    insertAccessLog(id, System.getProperty("user.name"), fullPath.toString(), accessType, "MONITORED");
                    if (SystemTray.isSupported()) {
                        SystemTray tray = SystemTray.getSystemTray();
                        TrayIcon trayIcon = new TrayIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB), "Java Notification");
                        trayIcon.setImageAutoSize(true);
                        trayIcon.setToolTip("Java уведомление");
                        tray.add(trayIcon);

                        trayIcon.displayMessage("Внимание", "Зафиксирована попытка"+accessType, TrayIcon.MessageType.WARNING);
                    }
                }
                key.reset();
            } catch (ClosedWatchServiceException e) {
                // Ожидаемое исключение при закрытии сервиса
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        }
    }
}