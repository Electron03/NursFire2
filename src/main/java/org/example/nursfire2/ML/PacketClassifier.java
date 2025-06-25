// org.example.nursfire2.ML.PacketClassifier.java (ПОСЛЕ ИСПРАВЛЕНИЙ)
package org.example.nursfire2.ML;

import org.example.nursfire2.models.PredictionResult;
import weka.classifiers.trees.J48; // Или любой другой классификатор Weka
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class PacketClassifier {
    private double[] featureMins;
    private double[] featureMaxs;
    private J48 model; // Ваш обученный классификатор Weka
    private Instances dataHeader; // Заголовок Instances для Weka

    // Конструктор, который будет загружать вашу модель и параметры нормализации
    public PacketClassifier() {
        // Путь к файлу вашей обученной модели Weka
        String modelPath = "path/to/your/trained_model.ser"; // !!! ЗАМЕНИТЕ НА РЕАЛЬНЫЙ ПУТЬ !!!
        String headerPath = "path/to/your/arff_header.ser"; // !!! ЗАМЕНИТЕ НА РЕАЛЬНЫЙ ПУТЬ К ЗАГОЛОВКУ ARFF !!!

        // Эти значения МИНИМУМОВ и МАКСИМУМОВ должны соответствовать тем,
        // что использовались при обучении вашей модели.
        // Вам нужно будет получить их из вашего процесса подготовки данных.
        // Это ПРИМЕРНЫЕ значения, основанные на вашем ARFF.
        // Точные значения должны быть извлечены из данных, использованных для ТРЕНИРОВКИ.
        this.featureMins = new double[]{
                981,    // packetLength min
                0,      // protocol min (TCP)
                5,      // numConnections min
                0,      // tcpFlag min
                9284,   // dstPort min
                0.3425  // interArrivalTime min
        };
        this.featureMaxs = new double[]{
                1498,   // packetLength max
                2,      // protocol max (ICMP)
                91,     // numConnections max
                6,      // tcpFlag max
                61849,  // dstPort max
                0.6823  // interArrivalTime max
        };

        // Загрузка обученной модели Weka
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            this.model = (J48) ois.readObject();
            System.out.println("Модель Weka успешно загружена.");
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке модели Weka: " + e.getMessage());
            e.printStackTrace();
            this.model = null; // Установить null, если загрузка не удалась
        }

        // Загрузка заголовка Instances для Weka (очень важно для создания новых экземпляров)
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(headerPath))) {
            this.dataHeader = (Instances) ois.readObject();
            System.out.println("Заголовок Instances успешно загружен.");
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке заголовка Instances: " + e.getMessage());
            e.printStackTrace();
            // Если заголовок не загружен, вам нужно будет создать его вручную
            // или бросить исключение, так как классификация невозможна.
            this.dataHeader = createWekaHeader(); // Попытка создать, если не удалось загрузить
            if (this.dataHeader == null) {
                System.err.println("Не удалось ни загрузить, ни создать заголовок Weka Instances. Классификация может быть некорректной.");
            }
        }

        // Валидация: убедимся, что размеры массивов совпадают с количеством признаков
        if (featureMins.length != 6 || featureMaxs.length != 6) { // 6 признаков в вашем ARFF
            System.err.println("Ошибка: Размеры featureMins/Maxs не соответствуют ожидаемому количеству признаков (6).");
        }
    }

    // Метод для создания заголовка Weka, если его не удалось загрузить
    private Instances createWekaHeader() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("packetLength"));
        attributes.add(new Attribute("protocol"));
        attributes.add(new Attribute("numConnections"));
        attributes.add(new Attribute("tcpFlag"));
        attributes.add(new Attribute("dstPort"));
        attributes.add(new Attribute("interArrivalTime"));

        // Атрибут класса
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("normal");
        classValues.add("DoS");
        classValues.add("Probe");
        classValues.add("R2L");
        classValues.add("U2R");
        attributes.add(new Attribute("class", classValues));

        Instances header = new Instances("PacketData", attributes, 0);
        header.setClassIndex(header.numAttributes() - 1); // Устанавливаем последний атрибут как класс
        return header;
    }

    public double[] normalizeFeatures(double[] features) {
        // Проверка на null ДО использования!
        if (featureMins == null || featureMaxs == null) {
            throw new IllegalStateException("");
        }
        if (features.length != featureMins.length) {
            throw new IllegalArgumentException("");
        }

        double[] normalized = new double[features.length];
        for (int i = 0; i < features.length; i++) {
            double min = this.featureMins[i];
            double max = this.featureMaxs[i];
            if (max - min == 0) { // Избегаем деления на ноль, если min == max
                normalized[i] = 0.0; // Или другое разумное значение
            } else {
                normalized[i] = (features[i] - min) / (max - min);
            }
        }
        return normalized;
    }

    public PredictionResult classify(double[] features) {
        if (model == null || dataHeader == null) {
            System.err.println("Na.");
            return new PredictionResult("Unknown", "Model Not Loaded", 0.0);
        }

        double[] normalizedFeatures = normalizeFeatures(features);

        // Создание экземпляра Weka из нормализованных признаков
        DenseInstance instance = new DenseInstance(dataHeader.numAttributes());
        instance.setDataset(dataHeader); // Очень важно установить dataset для экземпляра

        for (int i = 0; i < normalizedFeatures.length; i++) {
            instance.setValue(i, normalizedFeatures[i]);
        }

        // Устанавливаем пропущенное значение для атрибута класса, так как мы его предсказываем
        instance.setClassMissing();

        try {
            double predictedClassIndex = model.classifyInstance(instance);
            String predictedClass = dataHeader.classAttribute().value((int) predictedClassIndex);

            // Оценка уверенности (confidence)
            double[] distribution = model.distributionForInstance(instance);
            double confidence = distribution[(int) predictedClassIndex];

            return new PredictionResult(predictedClass, "J48", confidence);

        } catch (Exception e) {
            System.err.println("Ошибка при классификации пакета: " + e.getMessage());
            e.printStackTrace();
            return new PredictionResult("Unknown", "Error during classification", 0.0);
        }
    }
    public void retrainModelFromDBAndFile(String dbPath, String arffFilePath) {
        try {
            List<double[]> featuresList = new ArrayList<>();
            List<String> classLabels = new ArrayList<>();

            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement stmt = conn.createStatement();

            String query = """
            SELECT 
                cp.packet_size, cp.protocol, cp.destination_port, 
                pm.flags, pm.packet_type
            FROM CapturedPacket cp
            JOIN PacketMetadata pm ON cp.id = pm.id
        """;

            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                double[] features = new double[6];

                // Признак 1: packet size
                features[0] = rs.getInt("packet_size");

                // Признак 2: protocol (строка → число)
                String protocol = rs.getString("protocol").toUpperCase();
                features[1] = switch (protocol) {
                    case "TCP" -> 0;
                    case "UDP" -> 1;
                    case "ICMP" -> 2;
                    default -> -1; // неизвестный
                };

                // Признак 3: numConnections — фиктивный (пока)
                features[2] = 6;

                // Признак 4: tcpFlag
                String flag = rs.getString("flags").toUpperCase();
                features[3] = switch (flag) {
                    case "SYN" -> 0;
                    case "ACK" -> 1;
                    case "FIN" -> 2;
                    default -> -1;
                };

                // Признак 5: destination port
                features[4] = rs.getInt("destination_port");

                // Признак 6: interArrivalTime — фиктивный
                features[5] = 0.02;

                // Метка класса
                String label = rs.getString("packet_type");

                featuresList.add(features);
                classLabels.add(label);
            }

            rs.close();
            stmt.close();
            conn.close();

            // Запись в ARFF
            writeToArffFile(arffFilePath, featuresList, classLabels);

            // Загрузка и обучение модели
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(arffFilePath);
            Instances newDataset = source.getDataSet();
            newDataset.setClassIndex(newDataset.numAttributes() - 1);

            model.buildClassifier(newDataset);
            SerializationHelper.write("weka_model.model", model);

            System.out.println("Model retrained successfully from database and saved.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToArffFile(String filePath, List<double[]> features, List<String> labels) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("""
            @RELATION PacketData

            @ATTRIBUTE packetLength NUMERIC
            @ATTRIBUTE protocol NUMERIC
            @ATTRIBUTE numConnections NUMERIC
            @ATTRIBUTE tcpFlag NUMERIC
            @ATTRIBUTE dstPort NUMERIC
            @ATTRIBUTE interArrivalTime NUMERIC
            @ATTRIBUTE class {normal,DoS,Probe,R2L,U2R}

            @DATA
            """);

            for (int i = 0; i < features.size(); i++) {
                double[] f = features.get(i);
                writer.write(String.format("%f,%f,%f,%f,%f,%f,%s\n",
                        f[0], f[1], f[2], f[3], f[4], f[5], labels.get(i)));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}