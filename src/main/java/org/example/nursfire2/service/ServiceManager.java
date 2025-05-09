package org.example.nursfire2.service;

import org.example.nursfire2.utils.FolderMonitor;

import java.io.IOException;
import java.sql.SQLException;

public class ServiceManager {
    private static Thread folderMonitorThread;
    private static FolderMonitor folderMonitor;

    public static void startFolderMonitor() {
        if (folderMonitorThread == null || !folderMonitorThread.isAlive()) {
            try {
                folderMonitor = new FolderMonitor(org.example.nursfire2.database.DatabaseManager.connect());
                folderMonitorThread = new Thread(folderMonitor);
                folderMonitorThread.start();
                System.out.println("🚀 start forder monitor");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void stopFolderMonitor() {

            folderMonitor.stop();
            System.out.println("🛑 stop forder monitor.");
    }
}
