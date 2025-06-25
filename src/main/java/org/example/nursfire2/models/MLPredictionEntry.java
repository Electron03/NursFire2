package org.example.nursfire2.models;

public class MLPredictionEntry {
    private String id;
    private String packetId;
    private String modelVersion;
    private String predictedClass;
    private float confidence;
    private String timestamp;

    // Геттеры и сеттеры

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    // Остальные поля — по аналогии
}
