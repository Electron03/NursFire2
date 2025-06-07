package org.example.nursfire2.ML;

import org.example.nursfire2.models.PredictionResult;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.Attribute;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PacketClassifier {

    private Classifier model;
    private Instances datasetFormat;

    private static final String[] attackTypes = {"normal", "DoS", "Probe", "R2L", "U2R"};

    public PacketClassifier() {
        File modelFile = new File("weka_model.model");
        if (modelFile.exists()) {
            try {
                model = (Classifier) SerializationHelper.read("weka_model.model");
                datasetFormat = createEmptyDataset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            trainModel("traffic_data.arff");
        }
    }

    public void trainModel(String arffFilePath) {
        try {
            DataSource source = new DataSource(arffFilePath);
            Instances dataset = source.getDataSet();
            dataset.setClassIndex(dataset.numAttributes() - 1);

            model = new RandomForest();
            model.buildClassifier(dataset);
            SerializationHelper.write("weka_model.model", model);

            datasetFormat = dataset.stringFreeStructure();

            System.out.println("Model trained from file: " + arffFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Instances createEmptyDataset() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // Определяем атрибуты
        attributes.add(new Attribute("packetLength"));
        attributes.add(new Attribute("protocol"));
        attributes.add(new Attribute("numConnections"));
        attributes.add(new Attribute("tcpFlag"));
        attributes.add(new Attribute("dstPort"));
        attributes.add(new Attribute("interArrivalTime"));

        ArrayList<String> classValues = new ArrayList<>();
        for (String label : attackTypes) {
            classValues.add(label);
        }
        attributes.add(new Attribute("class", classValues));

        Instances dataset = new Instances("PacketData", attributes, 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);
        return dataset;
    }

    // Классификация данных
    public PredictionResult classify(double[] features) {
        try {
            Instance instance = new DenseInstance(features.length + 1); // +1 для класса
            instance.setDataset(datasetFormat);

            for (int i = 0; i < features.length; i++) {
                instance.setValue(i, features[i]);
            }

            double classIndex = model.classifyInstance(instance);
            String predictedClass = datasetFormat.classAttribute().value((int) classIndex);

            // Уверенность: вероятности по всем классам
            double[] distribution = model.distributionForInstance(instance);
            float confidence = (float) distribution[(int) classIndex];

            String modelVersion = "1.0";

            return new PredictionResult(predictedClass, confidence, modelVersion);

        } catch (Exception e) {
            e.printStackTrace();
            return new PredictionResult("error", 0f, "1.0");
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
                features[2] = 0;

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
                features[5] = 0.0;

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
            DataSource source = new DataSource(arffFilePath);
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
