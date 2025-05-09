package org.example.nursfire2.manager;
import java.io.File;
import java.nio.file.Path;

public class FolderAccessManager {

    public static void setFolderReadOnly(Path folderPath) {
        File folder = folderPath.toFile();
        folder.setReadable(true, false);
        folder.setWritable(false, false);
        folder.setExecutable(true, false);
    }

    public static void setFolderWritable(Path folderPath) {
        File folder = folderPath.toFile();
        folder.setReadable(true, false);
        folder.setWritable(true, false);
        folder.setExecutable(true, false);
    }
    public static void setFolderAccessBack(Path folderPath) {
        File folder = folderPath.toFile();
        folder.setReadable(true, false);   // Разрешить чтение всем
        folder.setWritable(true, false);   // Разрешить запись всем
        folder.setExecutable(true, false); // Разрешить выполнение всем
        System.out.println("🔓 Access to folder restored: " + folderPath);
    }
    public static void printPermissions(Path folderPath) {
        File folder = folderPath.toFile();
        System.out.println("Readable: " + folder.canRead());
        System.out.println("Writable: " + folder.canWrite());
        System.out.println("Executable: " + folder.canExecute());
    }
}

