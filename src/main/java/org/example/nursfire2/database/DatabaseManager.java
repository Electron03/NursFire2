    package org.example.nursfire2.database;

    import org.example.nursfire2.models.AccessLogEntry;
    import org.example.nursfire2.models.EncryptedFile;
    import org.example.nursfire2.models.MLPredictionLogEntry;
    import org.example.nursfire2.models.WatchedFolder;

    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.sql.Connection;
    import java.sql.DriverManager;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Statement;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.UUID;

    public class DatabaseManager {
        private static final String URL = "jdbc:sqlite:network_traffic.db";


        public static Connection connect() throws SQLException {
            return DriverManager.getConnection(URL);
        }


        public static void createTables() {
            String createCapturedPacket = """
                CREATE TABLE IF NOT EXISTS CapturedPacket (
                    id TEXT PRIMARY KEY,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                    source_ip TEXT,
                    destination_ip TEXT,
                    source_port INTEGER,
                    destination_port INTEGER,
                    protocol TEXT,
                    packet_size INTEGER,
                    raw_data BLOB
                );
            """;

            String createPacketMetadata = """
                CREATE TABLE IF NOT EXISTS PacketMetadata (
                    id TEXT PRIMARY KEY REFERENCES CapturedPacket(id),
                    ttl INTEGER,
                    flags TEXT,
                    payload_size INTEGER,
                    packet_type TEXT,
                    anomaly_score FLOAT
                );
            """;

            String createAttackTable = """
                CREATE TABLE IF NOT EXISTS Attack (
                    id TEXT PRIMARY KEY,
                    packet_id TEXT REFERENCES CapturedPacket(id),
                    attack_type TEXT,
                    severity INTEGER,
                    detection_method TEXT,
                    detected_at TEXT DEFAULT CURRENT_TIMESTAMP
                );
            """;

            String createMLPredictionLog = """
                CREATE TABLE IF NOT EXISTS MLPredictionLog (
                    id TEXT PRIMARY KEY,
                    packet_id TEXT REFERENCES CapturedPacket(id),
                    model_version TEXT,
                    predicted_class TEXT,
                    confidence FLOAT,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP
                );
            """;

            String createEncryptedFiles = """
            CREATE TABLE IF NOT EXISTS EncryptedFiles (
                id TEXT PRIMARY KEY,
                filename TEXT,
                path TEXT,
                encrypted_at TEXT DEFAULT CURRENT_TIMESTAMP,
                algorithm TEXT DEFAULT 'AES',
                encryption_key TEXT,
                data BLOB
            );
        """;

            String createAccessLog = """
            CREATE TABLE IF NOT EXISTS AccessLog (
                id TEXT PRIMARY KEY,
                username TEXT,
                filepath TEXT,
                access_type TEXT, -- например: READ, WRITE, DELETE
                access_time TEXT DEFAULT CURRENT_TIMESTAMP,
                access_result TEXT -- например: ALLOWED, BLOCKED
            );
        """;
            String createWatchedFolders = """
                        CREATE TABLE IF NOT EXISTS WatchedFolders (
                        id TEXT PRIMARY KEY,
                       folder_path TEXT NOT NULL,
                        added_at TEXT DEFAULT CURRENT_TIMESTAMP
                    );
                    """;
            try (Connection conn = connect();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(createCapturedPacket);
                stmt.execute(createPacketMetadata);
                stmt.execute(createAttackTable);
                stmt.execute(createMLPredictionLog);
                stmt.execute(createEncryptedFiles);
                stmt.execute(createAccessLog);
                stmt.execute(createWatchedFolders);
                System.out.println("Все таблицы успешно созданы!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static boolean addWatchedFolder( String folderPath) {
            String checkSql = "SELECT COUNT(*) FROM WatchedFolders WHERE folder_path = ?";
            String insertSql = "INSERT INTO WatchedFolders (id, folder_path) VALUES (?, ?)";

            try (Connection conn = connect(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, folderPath);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("⚠️ Папка уже добавлена: " + folderPath);
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                String id = UUID.randomUUID().toString();
                pstmt.setString(1, id);
                pstmt.setString(2, folderPath);
                pstmt.executeUpdate();
                System.out.println("✅ Папка добавлена: " + folderPath);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        public static List<MLPredictionLogEntry> getMLPredictionLogs() {
            List<MLPredictionLogEntry> logs = new ArrayList<>();
            String sql = "SELECT packet_id, model_version, predicted_class, confidence, timestamp FROM MLPredictionLog";

            try (Connection conn = connect();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    logs.add(new MLPredictionLogEntry(
                            rs.getString("packet_id"),
                            rs.getString("model_version"),
                            rs.getString("predicted_class"),
                            rs.getFloat("confidence"),
                            rs.getString("timestamp")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return logs;
        }
        public static List<EncryptedFile> getEncryptedFiles() {
            List<EncryptedFile> files = new ArrayList<>();
            String sql = "SELECT id, filename, path, encrypted_at, encryption_key FROM EncryptedFiles";

            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    files.add(new EncryptedFile(
                            rs.getString("id"),
                            rs.getString("filename"),
                            rs.getString("path"),
                            rs.getString("encrypted_at"),
                            rs.getString("encryption_key"),
                            rs.getString("path")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return files;
        }
        public static List<AccessLogEntry> getAccessLogs() {
            List<AccessLogEntry> logs = new ArrayList<>();
            String sql = "SELECT username, filepath, access_type, access_time, access_result FROM AccessLog";

            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    logs.add(new AccessLogEntry(
                            rs.getString("username"),
                            rs.getString("filepath"),
                            rs.getString("access_type"),
                            rs.getString("access_time"),
                            rs.getString("access_result")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return logs;
        }

        // Добавление перехваченного пакета
        public static void insertCapturedPacket(String id, String sourceIp, String destinationIp, int sourcePort, int destinationPort,
                                                String protocol, int packetSize, byte[] rawData) {
            String sql = "INSERT INTO CapturedPacket (id, source_ip, destination_ip, source_port, destination_port, protocol, packet_size, raw_data) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, sourceIp);
                pstmt.setString(3, destinationIp);
                pstmt.setInt(4, sourcePort);
                pstmt.setInt(5, destinationPort);
                pstmt.setString(6, protocol);
                pstmt.setInt(7, packetSize);
                pstmt.setBytes(8, rawData);
                pstmt.executeUpdate();
                System.out.println("paket insert");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static boolean deleteWatchedFolder(String folderPath) {
            String sql = "DELETE FROM WatchedFolders WHERE folder_path = ?";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, folderPath);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    System.out.println("❌ Папка удалена: " + folderPath);
                    return true;
                } else {
                    System.out.println("⚠️ Папка не найдена: " + folderPath);
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        public static List<WatchedFolder> getWatchedFolderList() {
            List<WatchedFolder> folders = new ArrayList<>();
            String sql = "SELECT id, folder_path FROM WatchedFolders";

            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String path = rs.getString("folder_path");
                    folders.add(new WatchedFolder(id, path));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return folders;
        }

        // Добавление метаданных пакета
        public static void insertPacketMetadata(String id, int ttl, String flags, int payloadSize, String packetType, float anomalyScore) {
            String sql = "INSERT INTO PacketMetadata (id, ttl, flags, payload_size, packet_type, anomaly_score) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setInt(2, ttl);
                pstmt.setString(3, flags);
                pstmt.setInt(4, payloadSize);
                pstmt.setString(5, packetType);
                pstmt.setFloat(6, anomalyScore);
                pstmt.executeUpdate();
                System.out.println("meta date insert!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Добавление записи об атаке
        public static void insertAttack(String id, String packetId, String attackType, int severity, String detectionMethod) {
            String sql = "INSERT INTO Attack (id, packet_id, attack_type, severity, detection_method) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, packetId);
                pstmt.setString(3, attackType);
                pstmt.setInt(4, severity);
                pstmt.setString(5, detectionMethod);
                pstmt.executeUpdate();
                System.out.println("Атака зарегистрирована!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        // Добавление предсказания ML-модели
        public static void insertMLPrediction(String id, String packetId, String modelVersion, String predictedClass, float confidence) {
            String sql = "INSERT INTO MLPredictionLog (id, packet_id, model_version, predicted_class, confidence) VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, packetId);
                pstmt.setString(3, modelVersion);
                pstmt.setString(4, predictedClass);
                pstmt.setFloat(5, confidence);
                pstmt.executeUpdate();
                System.out.println("result ML insert!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static void insertEncryptedFile(String id, String filename, String path,byte[] encryptedData,String userKey) {
            String sql = """
            INSERT INTO EncryptedFiles (id, filename, path, data, encryption_key) VALUES (?, ?, ?, ?, ?)
        """;

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, filename);
                pstmt.setString(3, path);
                pstmt.setBytes(4, encryptedData);
                pstmt.setString(5, userKey);
                pstmt.executeUpdate();
                System.out.println("Encrypted file inserted successfully.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static void insertAccessLog( String id, String username, String filepath, String accessType, String accessResult) {
            String sql = """
            INSERT INTO AccessLog (id, username, filepath, access_type, access_result)
            VALUES (?, ?, ?, ?, ?);
        """;

            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, username);
                pstmt.setString(3, filepath);
                pstmt.setString(4, accessType);
                pstmt.setString(5, accessResult);
                pstmt.executeUpdate();
                System.out.println("Access log inserted successfully.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        public static List<Path> loadWatchedFoldersFromDB() {
            List<Path> folders = new ArrayList<>();
            String sql = "SELECT folder_path FROM WatchedFolders";
                try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)){

                while (rs.next()) {
                    folders.add(Paths.get(rs.getString("folder_path")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return folders;
        }

        // Вывести все пакеты
        public static void getPackets() {
            String sql = "SELECT * FROM CapturedPacket";

            try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getString("id") +
                            ", Время: " + rs.getString("timestamp") +
                            ", Источник: " + rs.getString("source_ip") +
                            ", Получатель: " + rs.getString("destination_ip") +
                            ", Протокол: " + rs.getString("protocol") +
                            ", Размер: " + rs.getInt("packet_size"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }