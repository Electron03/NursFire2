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
import java.util.ArrayList;
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
            trainModel();
        }
    }

    private void trainModel() {
        try {
            Instances dataset = createEmptyDataset();
            Random rand = new Random();

            for (int i = 0; i < 1000; i++) {
                double[] values = new double[7]; // 6 признаков + класс
                values[0] = 40 + rand.nextInt(1460); // packetLength
                values[1] = rand.nextInt(3);         // protocol (TCP=0, UDP=1, ICMP=2)
                values[2] = rand.nextInt(100);       // numConnections
                values[3] = rand.nextInt(3);         // tcpFlag (SYN=0, ACK=1, FIN=2)
                values[4] = rand.nextInt(1024);      // dstPort
                values[5] = rand.nextDouble();       // interArrivalTime

                int classIndex = rand.nextInt(attackTypes.length);
                values[6] = classIndex;              // attack type

                Instance instance = new DenseInstance(1.0, values);
                dataset.add(instance);
            }

            model = new RandomForest();
            model.buildClassifier(dataset);
            SerializationHelper.write("weka_model.model", model);
            datasetFormat = dataset;

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

            String modelVersion = "1.0"; // Пока вручную, можешь привязать к дате или хешу

            return new PredictionResult(predictedClass, confidence, modelVersion);

        } catch (Exception e) {
            e.printStackTrace();
            return new PredictionResult("error", 0f, "1.0");
        }
    }


    public void retrainModelFromFile(String arffFilePath) {
        try {

            DataSource source = new DataSource(arffFilePath);
            Instances newDataset = source.getDataSet();
            newDataset.setClassIndex(newDataset.numAttributes() - 1);

            // Обновляем модель
            model.buildClassifier(newDataset);
            SerializationHelper.write("weka_model.model", model); // Сохраняем обновленную модель

            System.out.println("Model retrained successfully with new data.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
